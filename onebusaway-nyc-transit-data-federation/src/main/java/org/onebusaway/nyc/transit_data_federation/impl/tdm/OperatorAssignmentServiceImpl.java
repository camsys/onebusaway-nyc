package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.model.OperatorAssignmentItem;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;

@Component
public class OperatorAssignmentServiceImpl implements OperatorAssignmentService {

	private static Logger _log = LoggerFactory.getLogger(VehicleAssignmentServiceImpl.class);

	private Timer _updateTimer = null;
	
	private static final DateTimeFormatter _updatedDateFormatter = ISODateTimeFormat.dateTimeNoMillis();

  private static SimpleDateFormat _serviceDateFormatter = new SimpleDateFormat("yyyy-MM-dd");

	@Autowired
	private ConfigurationService _configurationService;
	
	// map structure: service date -> (operator pass ID->operator assignment item)
	private volatile HashMap<String, HashMap<String, OperatorAssignmentItem>> _serviceDateToOperatorListMap = 
			new HashMap<String, HashMap<String, OperatorAssignmentItem>>();

	private String getServiceDateKey(Date date) {
		if(date != null)
		  return _serviceDateFormatter.format(date);
		else
			return null;
	}
	
	private HashMap<String, OperatorAssignmentItem> getOperatorMapForServiceDate(String serviceDate) {
		try {			
			ArrayList<JsonObject> operatorAssignments = 
				TransitDataManagerApiLibrary.getItemsForRequest("crew", serviceDate, "list");

			HashMap<String, OperatorAssignmentItem> output = new HashMap<String, OperatorAssignmentItem>();
			for(JsonObject itemToAdd : operatorAssignments) {
			  OperatorAssignmentItem item = new OperatorAssignmentItem();
			  item.setAgencyId(itemToAdd.get("agency-id").getAsString());
        item.setPassId(itemToAdd.get("pass-id").getAsString());
        item.setRunNumber(itemToAdd.get("run-id").getAsString());
        item.setRunRoute(itemToAdd.get("run-route").getAsString());
        item.setRunId(item.getRunRoute() + "-" +  item.getRunNumber());
        item.setServiceDate(_serviceDateFormatter.parse(itemToAdd.get("service-date").getAsString()));
        item.setUpdated(_updatedDateFormatter.parseDateTime(itemToAdd.get("updated").getAsString()));
			  
			  output.put(item.getPassId(), item);
			}
			
			return output;
		} catch(Exception e) {
			_log.error("Error getting operator list for serviceDate=" + serviceDate);
			return null;
		}		
	}
	
	private class UpdateThread extends TimerTask {
		@Override
		public void run() {
			synchronized(_serviceDateToOperatorListMap) {
				for(String serviceDate : _serviceDateToOperatorListMap.keySet()) {
					HashMap<String, OperatorAssignmentItem> operatorIdToAssignmentItemMap = getOperatorMapForServiceDate(serviceDate);
					if(operatorIdToAssignmentItemMap != null)
						_serviceDateToOperatorListMap.put(serviceDate, operatorIdToAssignmentItemMap);
				}
			}
		}		
	}

	@Refreshable(dependsOn = "tdm.crewAssignmentRefreshInterval")
	public void configChanged() {
		Integer updateInterval = 
				_configurationService.getConfigurationValueAsInteger("tdm.crewAssignmentRefreshInterval", null);

		if(updateInterval != null)
			setUpdateFrequency(updateInterval);
	}
	
	public void setUpdateFrequency(int seconds) {
		if(_updateTimer != null) {
			_updateTimer.cancel();
		}

		_updateTimer = new Timer();
		_updateTimer.schedule(new UpdateThread(), 0, seconds * 1000);		
	}
	
	@SuppressWarnings("unused")
	@PostConstruct
	private void startUpdateProcess() {
		setUpdateFrequency(30 * 60 * 60); // 30m
	}

	@Override
	public synchronized Collection<OperatorAssignmentItem> getOperatorsForServiceDate(Date serviceDate) {
		String serviceDateKey = getServiceDateKey(serviceDate);		
		if(serviceDateKey == null) 
			return null;
		
		HashMap<String, OperatorAssignmentItem> list = _serviceDateToOperatorListMap.get(serviceDateKey);
		if(list == null) {
			list = getOperatorMapForServiceDate(serviceDateKey);
			if(list == null)
			  return null;
			_serviceDateToOperatorListMap.put(serviceDateKey, list);
		}
		return list.values();
	}

  @Override
  public synchronized OperatorAssignmentItem getOperatorAssignmentItem(Date serviceDate, String operatorId) {
    String serviceDateKey = getServiceDateKey(serviceDate);   
    if(serviceDateKey == null) 
      return null;
    
    HashMap<String, OperatorAssignmentItem> list = _serviceDateToOperatorListMap.get(serviceDateKey);
    if(list == null) {
      list = getOperatorMapForServiceDate(serviceDateKey);
      if(list == null)
        return null;
      _serviceDateToOperatorListMap.put(serviceDateKey, list);
    }
    return list.get(operatorId);
  }
}
