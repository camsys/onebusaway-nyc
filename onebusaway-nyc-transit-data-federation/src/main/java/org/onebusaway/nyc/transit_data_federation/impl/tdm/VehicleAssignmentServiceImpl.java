package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.tdm.ConfigurationService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleDepotAssignmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.JsonObject;

@Component
public class VehicleAssignmentServiceImpl implements VehicleDepotAssignmentService {

	private static Logger _log = LoggerFactory.getLogger(VehicleAssignmentServiceImpl.class);

	private Timer _updateTimer = null;

	@Autowired
	private ConfigurationService _configurationService;

	private volatile HashMap<String, ArrayList<AgencyAndId>> _depotToVehicleListMap = 
			new HashMap<String, ArrayList<AgencyAndId>>();

	private ArrayList<AgencyAndId> getVehicleListForDepot(String depotId) {
		try {
			ArrayList<JsonObject> vehicleAssignments = 
					DataFetcherLibrary.getItemsForRequest("depot", depotId, "vehicles", "list");

			ArrayList<AgencyAndId> vehiclesForThisDepot = new ArrayList<AgencyAndId>();
			for(JsonObject depotVehicleAssignment : vehicleAssignments) {
				vehiclesForThisDepot.add(new AgencyAndId(
					depotVehicleAssignment.get("agencyId").getAsString(), 
					depotVehicleAssignment.get("vehicleId").getAsString()));
			}
	
			return vehiclesForThisDepot;
		} catch(Exception e) {
			_log.error("Error getting vehicle list for depot with ID " + depotId);
			return null;
		}		
	}
	
	private class UpdateThread extends TimerTask {
		@Override
		public void run() {
			synchronized(_depotToVehicleListMap) {
				for(String depotId : _depotToVehicleListMap.keySet()) {
					ArrayList<AgencyAndId> list = getVehicleListForDepot(depotId);
					if(list != null)
						_depotToVehicleListMap.put(depotId, list);
				}
			}
		}		
	}

	@Refreshable(dependsOn = "tdm.vehicleAssignmentRefreshInterval")
	public void configChanged() {
		Integer updateInterval = 
				_configurationService.getConfigurationValueAsInteger("tdm.vehicleAssignmentRefreshInterval", null);
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
		setUpdateFrequency(1 * 24 * 60 * 60); // 1h
	}
	
	@Override
	public ArrayList<AgencyAndId> getAssignedVehicleIdsForDepot(String depotId) {
		ArrayList<AgencyAndId> list = _depotToVehicleListMap.get(depotId);
		if(list == null) {
			list = getVehicleListForDepot(depotId);
			_depotToVehicleListMap.put(depotId, list);
		}
		return list;
	}
}
