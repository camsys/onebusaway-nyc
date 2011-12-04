package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.transit_data_federation.model.tdm.OperatorAssignmentItem;
import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;

import com.google.gson.JsonObject;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

@Component
public class OperatorAssignmentServiceImpl implements OperatorAssignmentService {

	private static Logger _log = LoggerFactory.getLogger(VehicleAssignmentServiceImpl.class);

	private static final DateTimeFormatter _updatedDateFormatter = ISODateTimeFormat.dateTimeNoMillis();

  // map structure: service date -> (operator pass ID->operator assignment item)
  private volatile HashMap<ServiceDate, HashMap<String, OperatorAssignmentItem>> _serviceDateToOperatorListMap = 
      new HashMap<ServiceDate, HashMap<String, OperatorAssignmentItem>>();

  private ScheduledFuture<OperatorAssignmentServiceImpl.UpdateThread> _updateTask = null;
  
  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;
  
  private ConfigurationService _configurationService;

  private TransitDataManagerApiLibrary _transitDataManagerApiLibrary = null;

  @Autowired
  public void setConfigurationService(ConfigurationService configurationService) {
    this._configurationService = configurationService;
  }

  @Autowired
  public void setTransitDataManagerApiLibrary(TransitDataManagerApiLibrary apiLibrary) {
    this._transitDataManagerApiLibrary = apiLibrary;
  }
	
	private HashMap<String, OperatorAssignmentItem> getOperatorMapForServiceDate(ServiceDate serviceDate) {
		try {			
		  String serviceDateUrlParameter = serviceDate.getYear() + "-" + serviceDate.getMonth() + "-" + serviceDate.getDay();
		  ArrayList<JsonObject> operatorAssignments = _transitDataManagerApiLibrary.getItemsForRequest("crew", 
		      serviceDateUrlParameter, "list");

			HashMap<String, OperatorAssignmentItem> output = new HashMap<String, OperatorAssignmentItem>();

			for(JsonObject itemToAdd : operatorAssignments) {
			  OperatorAssignmentItem item = new OperatorAssignmentItem();
			  item.setAgencyId(itemToAdd.get("agency-id").getAsString());
        item.setPassId(itemToAdd.get("pass-id").getAsString());
        item.setRunNumber(itemToAdd.get("run-number").getAsString()); 
        item.setRunRoute(itemToAdd.get("run-route").getAsString());
        item.setServiceDate(ServiceDate.parseString(itemToAdd.get("service-date").getAsString()));
        item.setUpdated(_updatedDateFormatter.parseDateTime(itemToAdd.get("updated").getAsString()));
			  
			  output.put(item.getPassId(), item);
			}
			
			return output;
		} catch(Exception e) {
			_log.error("Error getting operator list for serviceDate=" + serviceDate + "; error was " + e.getMessage());
			return null;
		}		
	}
	
	public void refreshData() {
    synchronized(_serviceDateToOperatorListMap) {
      for(ServiceDate serviceDate : _serviceDateToOperatorListMap.keySet()) {
        HashMap<String, OperatorAssignmentItem> operatorIdToAssignmentItemMap = 
            getOperatorMapForServiceDate(serviceDate);
        
        if(operatorIdToAssignmentItemMap != null) {
          _serviceDateToOperatorListMap.put(serviceDate, operatorIdToAssignmentItemMap);
        }
      }
    }	  
	}
	
	private class UpdateThread implements Runnable {
		@Override
		public void run() {
		  refreshData();
		}		
	}

	@SuppressWarnings("unused")
  @Refreshable(dependsOn = "tdm.crewAssignmentRefreshInterval")
	private void configChanged() {
		Integer updateInterval = 
				_configurationService.getConfigurationValueAsInteger("tdm.crewAssignmentRefreshInterval", null);

		if(updateInterval != null) {
			setUpdateFrequency(updateInterval);
		}
	}
	
	@SuppressWarnings("unchecked")
  private void setUpdateFrequency(int seconds) {
		if(_updateTask != null) {
		  _updateTask.cancel(true);
		}

		_updateTask = _taskScheduler.scheduleWithFixedDelay(new UpdateThread(), seconds * 1000);
	}
	
	@SuppressWarnings("unused")
	@PostConstruct
	private void startUpdateProcess() {
	  setUpdateFrequency(30 * 60); // 30m
	}

	@Override
	public Collection<OperatorAssignmentItem> getOperatorsForServiceDate(ServiceDate serviceDate) 
	    throws Exception {
		
		if(serviceDate == null) {
			return null;
		}
		
    synchronized(_serviceDateToOperatorListMap) {
      HashMap<String, OperatorAssignmentItem> list = _serviceDateToOperatorListMap.get(serviceDate);
      
      if(list == null) {
        list = getOperatorMapForServiceDate(serviceDate);
        if(list == null) {
          throw new Exception("Operator service is temporarily not available.");
        }
        
        _serviceDateToOperatorListMap.put(serviceDate, list);
      }
      
      _log.debug("Have " + list.size() + " operators for servicedate " + serviceDate);

      return list.values();
    }
	}

  @Override
  public OperatorAssignmentItem getOperatorAssignmentItemForServiceDate(ServiceDate serviceDate, String operatorId) 
      throws Exception {
    
    if(serviceDate == null) {
      return null;
    }
    
    synchronized(_serviceDateToOperatorListMap) {
      HashMap<String, OperatorAssignmentItem> list = _serviceDateToOperatorListMap.get(serviceDate);
      
      if(list == null) {
        list = getOperatorMapForServiceDate(serviceDate);
        if(list == null) {
          throw new Exception("Operator service is temporarily not available.");
        }
        
        _serviceDateToOperatorListMap.put(serviceDate, list);
      }      

      _log.debug("Have " + list.size() + " operators for servicedate " + serviceDate);

      return list.get(operatorId);
    }
  }
}
