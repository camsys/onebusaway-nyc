package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

@Component
public class VehicleAssignmentServiceImpl implements VehicleAssignmentService {

  private static Logger _log = LoggerFactory.getLogger(VehicleAssignmentServiceImpl.class);

  private volatile HashMap<String, ArrayList<AgencyAndId>> _depotToVehicleIdListMap = new HashMap<String, ArrayList<AgencyAndId>>();

  private volatile Map<AgencyAndId, String> _vehicleIdToDepotMap = new HashMap<AgencyAndId, String>();

  private ScheduledFuture<VehicleAssignmentServiceImpl.UpdateThread> _updateTask = null;
  
  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;
  
  private ConfigurationService _configurationService;
  
  @Autowired
  private TransitDataManagerApiLibrary _transitDataManagerApiLibrary = null;

  @Autowired
  public void setConfigurationService(ConfigurationService configurationService) {
    this._configurationService = configurationService;
  }

  public void setTransitDataManagerApiLibrary(TransitDataManagerApiLibrary apiLibrary) {
    this._transitDataManagerApiLibrary = apiLibrary;
  }

  private ArrayList<AgencyAndId> getVehicleListForDepot(String depotId) {
    try {
      List<Map<String, String>> vehicleAssignments = 
          _transitDataManagerApiLibrary.getItems("depot", depotId, "vehicles", "list");

      ArrayList<AgencyAndId> vehiclesForThisDepot = new ArrayList<AgencyAndId>();

      for (Map<String, String> depotVehicleAssignment : vehicleAssignments) {
        AgencyAndId vehicle = new AgencyAndId(
            depotVehicleAssignment.get("agency-id"),
            depotVehicleAssignment.get("vehicle-id"));
      
        vehiclesForThisDepot.add(vehicle);
      }
      
      return vehiclesForThisDepot;
    } catch (Exception e) {
      _log.error("Error getting vehicle list for depot with ID " + depotId);
      return null;
    }
  }

  private void updateVehicleIdToDepotMap(List<AgencyAndId> oldList, List<AgencyAndId> newList, String depotId) {
    synchronized(_vehicleIdToDepotMap) {
      // remove all vehicles assigned to the depot we're "refreshing"
      if(oldList != null) {
        for(AgencyAndId vehicleId : oldList) {
          if(newList.contains(vehicleId)) {
            continue;
          }

          _vehicleIdToDepotMap.remove(vehicleId);
        }
      }
      
      // add vehicles back that are (still) assigned to the depot
      for(AgencyAndId vehicleId : newList) {
        _vehicleIdToDepotMap.put(vehicleId, depotId);
      }    
    }
  }
  
  public void refreshData() {
    synchronized (_depotToVehicleIdListMap) {
      for (String depotId : _depotToVehicleIdListMap.keySet()) {
        ArrayList<AgencyAndId> list = getVehicleListForDepot(depotId);

        if (list != null) {
          updateVehicleIdToDepotMap(_depotToVehicleIdListMap.get(depotId), list, depotId);
          _depotToVehicleIdListMap.put(depotId, list);
        }
      }
    }    
  }
  
  private class UpdateThread extends TimerTask {
    @Override
    public void run() {
      refreshData();
    }
  }

  @SuppressWarnings("unused")
  @Refreshable(dependsOn = "tdm.vehicleAssignmentRefreshInterval")
  private void configChanged() {
    Integer updateInterval = _configurationService.getConfigurationValueAsInteger(
        "tdm.vehicleAssignmentRefreshInterval", null);

    if (updateInterval != null) {
      setUpdateFrequency(updateInterval);
    }
  }

  @SuppressWarnings("unchecked")
  private void setUpdateFrequency(int seconds) {
    if (_updateTask != null) {
      _updateTask.cancel(true);
    }

    _updateTask = _taskScheduler.scheduleWithFixedDelay(new UpdateThread(), seconds * 1000);
  }

  @SuppressWarnings("unused")
  @PostConstruct
  private void startUpdateProcess() {
    setUpdateFrequency(1 * 60 * 60); // 1h
  }

  @Override
  public ArrayList<AgencyAndId> getAssignedVehicleIdsForDepot(String depotId) 
      throws Exception {
    
    synchronized (_depotToVehicleIdListMap) {
      ArrayList<AgencyAndId> list = _depotToVehicleIdListMap.get(depotId);
      
      if (list == null) {
        list = getVehicleListForDepot(depotId);
        if(list == null) {
          throw new Exception("Vehicle assignment service is temporarily unavailable.");
        }
        
        updateVehicleIdToDepotMap(_depotToVehicleIdListMap.get(depotId), list, depotId);
        _depotToVehicleIdListMap.put(depotId, list);
      }

      _log.debug("Have " + list.size() + " vehicles for depot " + depotId);
      
      return list;
    }
  }

  @Override
  public String getAssignedDepotForVehicleId(AgencyAndId vehicle) {
    synchronized(_vehicleIdToDepotMap) {
      return _vehicleIdToDepotMap.get(vehicle);
    }
  }
}
