package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

@Component
public class VehicleAssignmentServiceImpl implements VehicleAssignmentService {

  private static Logger _log = LoggerFactory.getLogger(VehicleAssignmentServiceImpl.class);

  private Timer _updateTimer = null;

  @Autowired
  private ConfigurationService _configurationService;

  private volatile HashMap<String, ArrayList<AgencyAndId>> _depotToVehicleListMap = new HashMap<String, ArrayList<AgencyAndId>>();

  private volatile Map<AgencyAndId, String> _vehicleToDepotMap = new HashMap<AgencyAndId, String>();

  private TransitDataManagerApiLibrary _apiLibrary = new TransitDataManagerApiLibrary();

  public void setApiLibrary(TransitDataManagerApiLibrary apiLibrary) {
    this._apiLibrary = apiLibrary;
  }

  private ArrayList<AgencyAndId> getVehicleListForDepot(String depotId) {
    try {
      List<Map<String, String>> vehicleAssignments = _apiLibrary.getItems(
          "depot", depotId, "vehicles", "list");

      ArrayList<AgencyAndId> vehiclesForThisDepot = new ArrayList<AgencyAndId>();
      for (Map<String, String> depotVehicleAssignment : vehicleAssignments) {
        AgencyAndId vehicle = new AgencyAndId(
            depotVehicleAssignment.get("agency-id"),
            depotVehicleAssignment.get("vehicle-id"));
        vehiclesForThisDepot.add(vehicle);
        _vehicleToDepotMap.put(vehicle, depotId);
      }

      return vehiclesForThisDepot;
    } catch (Exception e) {
      _log.error("Error getting vehicle list for depot with ID " + depotId);
      return null;
    }
  }

  private class UpdateThread extends TimerTask {
    @Override
    public void run() {
      synchronized (_depotToVehicleListMap) {
        for (String depotId : _depotToVehicleListMap.keySet()) {
          ArrayList<AgencyAndId> list = getVehicleListForDepot(depotId);
          if (list != null)
            _depotToVehicleListMap.put(depotId, list);
        }
      }

    }
  }

  @Refreshable(dependsOn = "tdm.vehicleAssignmentRefreshInterval")
  public void configChanged() {
    Integer updateInterval = _configurationService.getConfigurationValueAsInteger(
        "tdm.vehicleAssignmentRefreshInterval", null);

    if (updateInterval != null)
      setUpdateFrequency(updateInterval);
  }

  public void setUpdateFrequency(int seconds) {
    if (_updateTimer != null) {
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
  public ArrayList<AgencyAndId> getAssignedVehicleIdsForDepot(
      String depotId) {
    synchronized (_depotToVehicleListMap) {
      ArrayList<AgencyAndId> list = _depotToVehicleListMap.get(depotId);
      if (list == null) {
        list = getVehicleListForDepot(depotId);
        _depotToVehicleListMap.put(depotId, list);
      }
      return list;
    }
  }

  /**
   * Get depot for vehicle.
   * 
   * Notes:
   * 
   * * we only populate the vehicle:depot map when a depot:vehicles call
   * is made, so it is theoretically possible that this method could get called
   * before the map is populated, or before it has been populated for a given
   * depot. Jeff says that the way things work, that should never happen.
   * 
   * * we synch on _depotToVehicleListMap, even though we're looking up in
   * _vehicleToDepotMap.  The former is basically used as a semaphore in this
   * class, for all updates to either map.
   * 
   */
  @Override
  public String getAssignedDepotForVehicle(AgencyAndId vehicle)
      throws Exception {
    synchronized (_depotToVehicleListMap) {
      String depot = _vehicleToDepotMap.get(vehicle);
      _log.info("getAssignedDepotForVehicle is returning depot " + depot + " for vehicle " + vehicle);
      return depot;
    }
  }

}
