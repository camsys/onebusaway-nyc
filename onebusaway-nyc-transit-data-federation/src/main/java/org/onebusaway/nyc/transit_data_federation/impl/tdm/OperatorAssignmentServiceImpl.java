package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.transit_data_federation.impl.tdm.model.OperatorAssignmentItem;
import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

@Component
public class OperatorAssignmentServiceImpl implements OperatorAssignmentService {

	private static Logger _log = LoggerFactory.getLogger(VehicleAssignmentServiceImpl.class);

	private Timer _updateTimer = null;

	private static Gson _gson = new Gson();
	
	@Autowired
	private ConfigurationService _configurationService;
	
	private volatile HashMap<String, ArrayList<OperatorAssignmentItem>> _serviceDateToOperatorListMap = 
			new HashMap<String, ArrayList<OperatorAssignmentItem>>();

	private String getServiceDateKey(Date date) {
		if(date != null)
			return String.format("%Y-%m-%d", date);
		else
			return null;
	}
	
	private ArrayList<OperatorAssignmentItem> getOperatorListForServiceDate(String serviceDate) {
		try {			
			ArrayList<JsonObject> operatorAssignments = 
				TransitDataManagerApiLibrary.getItemsForRequest("crew", serviceDate, "list");

			ArrayList<OperatorAssignmentItem> output = new ArrayList<OperatorAssignmentItem>();
			for(JsonObject itemToAdd : operatorAssignments) {
				output.add(_gson.fromJson(itemToAdd, OperatorAssignmentItem.class));
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
					ArrayList<OperatorAssignmentItem> list = getOperatorListForServiceDate(serviceDate);
					if(list != null)
						_serviceDateToOperatorListMap.put(serviceDate, list);
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
	public ArrayList<OperatorAssignmentItem> getOperatorsForServiceDate(
			Date serviceDate) {
		String serviceDateKey = getServiceDateKey(serviceDate);		
		if(serviceDateKey == null) 
			return null;
		
		ArrayList<OperatorAssignmentItem> list = _serviceDateToOperatorListMap.get(serviceDateKey);
		if(list == null) {
			list = getOperatorListForServiceDate(serviceDateKey);
			_serviceDateToOperatorListMap.put(serviceDateKey, list);
		}
		return list;
	}

  @Override
  public OperatorAssignmentItem getOperatorAssignmentItem(Date today,
      String operatorId) {
    // TODO Auto-generated method stub
    return null;
  }
}
