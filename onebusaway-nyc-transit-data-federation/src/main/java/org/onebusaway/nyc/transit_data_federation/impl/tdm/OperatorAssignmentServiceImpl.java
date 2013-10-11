package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;

import org.onebusaway.nyc.transit_data_federation.model.tdm.OperatorAssignmentItem;
import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.impl.tdm.TransitDataManagerApiLibrary;

import com.google.gson.JsonObject;

import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

/**
 * A component that fetches operator assignment data from the TDM and provides it to the IE upon request.
 * @author jmaki
 *
 */
@Component
public class OperatorAssignmentServiceImpl implements OperatorAssignmentService {

  private static Logger _log = LoggerFactory.getLogger(VehicleAssignmentServiceImpl.class);

  private static final DateTimeFormatter _updatedDateFormatter = ISODateTimeFormat.dateTimeNoMillis();

  // the range of service dates to consider
  private static final long MAX_SERVICE_DATE_DELTA = 1000 * 60 * 60 * 24 * 2;

  // map structure: service date -> (operator pass ID->operator assignment item)
  private volatile Map<ServiceDate, HashMap<String, OperatorAssignmentItem>> _serviceDateToOperatorListMap = 
		  new HashMap<ServiceDate, HashMap<String, OperatorAssignmentItem>>();

  private ScheduledFuture<OperatorAssignmentServiceImpl.UpdateThread> _updateTask = null;

  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;

  private ConfigurationService _configurationService;

  private TransitDataManagerApiLibrary _transitDataManagerApiLibrary = null;

  @Autowired
  public void setConfigurationService(ConfigurationService configurationService) {
    this._configurationService = configurationService;
    configChanged();
  }

  @Autowired
  public void setTransitDataManagerApiLibrary(TransitDataManagerApiLibrary apiLibrary) {
    this._transitDataManagerApiLibrary = apiLibrary;
  }

  private void preCache() {
	  _log.info("pre-cache start");
	  ServiceDate today = new ServiceDate(Calendar.getInstance().getTime());
	  getOperatorMapForServiceDate(today);
	  Calendar c1 = Calendar.getInstance();
	  c1.add(Calendar.DAY_OF_YEAR, -1);
	  ServiceDate yesterday = new ServiceDate(c1);
	  getOperatorMapForServiceDate(yesterday);
	  c1.add(Calendar.DAY_OF_YEAR, 2);
	  ServiceDate tomorrow = new ServiceDate(c1);
	  getOperatorMapForServiceDate(tomorrow);
	  _log.info("pre-cache end");
  }
  
  private final Pattern operatorIdPattern = Pattern.compile("^0*[a-zA-Z]*0*(\\d+)$");

  private HashMap<String, OperatorAssignmentItem> getOperatorMapForServiceDate(ServiceDate serviceDate) {
    try {
      String serviceDateUrlParameter = serviceDate.getYear() + "-" + serviceDate.getMonth() + "-" + serviceDate.getDay();
      List<JsonObject> operatorAssignments = _transitDataManagerApiLibrary.getItemsForRequest(
          "crew", serviceDateUrlParameter, "list");

      HashMap<String, OperatorAssignmentItem> output = new HashMap<String, OperatorAssignmentItem>();

      for (JsonObject itemToAdd : operatorAssignments) {
        OperatorAssignmentItem item = new OperatorAssignmentItem();
        item.setAgencyId(itemToAdd.get("agency-id").getAsString());
        item.setPassId(itemToAdd.get("pass-id").getAsString());
        item.setRunNumber(itemToAdd.get("run-number").getAsString());
        item.setRunRoute(itemToAdd.get("run-route").getAsString());
        item.setDepot(itemToAdd.get("depot").getAsString());
        item.setServiceDate(ServiceDate.parseString(itemToAdd.get("service-date").getAsString().replace("-", "")));
        item.setUpdated(_updatedDateFormatter.parseDateTime(itemToAdd.get("updated").getAsString()));

        Matcher operatorIdMatcher = operatorIdPattern.matcher(item.getPassId());
        if (!operatorIdMatcher.matches()) {
          _log.error("Couldn't parse operator ID from TDM: " + item.getPassId());
          continue;
        }

        String tailoredId = operatorIdMatcher.group(1);
        output.put(item.getAgencyId() + "_" + tailoredId, item);
      }

      return output;
    } catch (Exception e) {
      _log.error("Error getting operator list for serviceDate=" + serviceDate + "; error was " + e.getMessage());
      return null;
    }
  }

  public void refreshData() {
    _log.info("refreshData starting...");
    /*
     * update a copy of the data, to reduce the synchronization interval
     */
    Map<ServiceDate, HashMap<String, OperatorAssignmentItem>> serviceDateMapCopy = new HashMap<ServiceDate, HashMap<String, OperatorAssignmentItem>>();
    for (ServiceDate serviceDate: _serviceDateToOperatorListMap.keySet()) {
      if (isApplicable(serviceDate)) {
        serviceDateMapCopy.put(serviceDate, _serviceDateToOperatorListMap.get(serviceDate));
      }
    }

    Set<ServiceDate> keySet = new HashSet<ServiceDate>(serviceDateMapCopy.keySet());
    for (ServiceDate serviceDate : keySet) {
      HashMap<String, OperatorAssignmentItem> operatorIdToAssignmentItemMap = getOperatorMapForServiceDate(serviceDate);
      if (operatorIdToAssignmentItemMap != null) {
        serviceDateMapCopy.put(serviceDate, operatorIdToAssignmentItemMap);
      }
    }
    
    
    // the actual sync takes place in this method
    updateData(serviceDateMapCopy);
    _log.info("refreshData complete");
  }

  /**
   * lock the underlying map to update with refreshed values. 
   */
  private  void updateData(Map<ServiceDate, HashMap<String, OperatorAssignmentItem>> serviceDateMapCopy) {
    _log.info("updateData starting...");
    synchronized (_serviceDateToOperatorListMap) {
    	for (ServiceDate serviceDate: serviceDateMapCopy.keySet()) {
    		_serviceDateToOperatorListMap.put(serviceDate, serviceDateMapCopy.get(serviceDate));
    	}
    }
    _log.info("updateData complete");
  }
  
  // cap service dates to within a range of days from now
  boolean isApplicable(ServiceDate serviceDate) {
    if (serviceDate == null) return false;
    
    Calendar cal = Calendar.getInstance();
    ServiceDate now = new ServiceDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
    return Math.abs(now.getAsDate().getTime() - serviceDate.getAsDate().getTime()) < MAX_SERVICE_DATE_DELTA; 
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
    Integer updateInterval = _configurationService.getConfigurationValueAsInteger("tdm.crewAssignmentRefreshInterval", null);
    if (updateInterval != null) {
      setUpdateFrequency(updateInterval);
    }
  }

  @SuppressWarnings("unchecked")
  private void setUpdateFrequency(int seconds) {
    if (_updateTask != null) {
      _updateTask.cancel(true);
    }
    _log.info("operator assignment refresh interval=" + seconds + "s");
    _updateTask = _taskScheduler.scheduleWithFixedDelay(new UpdateThread(), seconds * 1000);
  }

  @SuppressWarnings("unused")
  @PostConstruct
  private void startUpdateProcess() {
	preCache();
    if (_updateTask==null) {
      setUpdateFrequency(30 * 60); // 30m
    }
  }

  @Override
  public  Collection<OperatorAssignmentItem> getOperatorsForServiceDate(
      ServiceDate serviceDate) throws Exception {

    if (serviceDate == null) {
      return null;
    }

    HashMap<String, OperatorAssignmentItem> list = _serviceDateToOperatorListMap.get(serviceDate);
    if (list != null) return list.values();
    
    synchronized(this._serviceDateToOperatorListMap) {
      // check once more, in case the thread ahead of us did our job for us
      list = _serviceDateToOperatorListMap.get(serviceDate);
      if (list != null) return list.values();
      
      list = getOperatorMapForServiceDate(serviceDate);
      if (list == null) {
        throw new Exception("Operator service is temporarily not available.");
      }

      _serviceDateToOperatorListMap.put(serviceDate, list);

      if (_serviceDateToOperatorListMap.size() == 0) {
        _log.warn("No operator assignment values are present!");
      } else {
        _log.debug("Have " + _serviceDateToOperatorListMap.size()
            + " operator assignments.");
      }
    }

    return list.values();
  }

  @Override
  public  OperatorAssignmentItem getOperatorAssignmentItemForServiceDate(
      ServiceDate serviceDate, AgencyAndId operatorId) throws Exception {

    if (serviceDate == null) {
      return null;
    }

    HashMap<String, OperatorAssignmentItem> list = _serviceDateToOperatorListMap.get(serviceDate);
    if (list != null) return list.get(operatorId.toString());
    synchronized (_serviceDateToOperatorListMap) {
      // check once more in case the thread ahead of us did the work 
      list = _serviceDateToOperatorListMap.get(serviceDate);
      if (list != null) return list.get(operatorId.toString());
      
      list = getOperatorMapForServiceDate(serviceDate);
      if (list == null) {
        throw new Exception("Operator service is temporarily not available.");
      }

      _serviceDateToOperatorListMap.put(serviceDate, list);

      if (_serviceDateToOperatorListMap.size() == 0) {
        _log.warn("No operator assignment values are present!");
      } else {
        _log.debug("Have " + _serviceDateToOperatorListMap.size()
            + " operator assignments.");
      }
    }

    return list.get(operatorId.toString());
  }
}
