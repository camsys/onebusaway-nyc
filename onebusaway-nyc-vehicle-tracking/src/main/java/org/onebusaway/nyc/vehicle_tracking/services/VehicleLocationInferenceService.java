package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.realtime.api.VehicleLocationRecord;

public interface VehicleLocationInferenceService {

  public void handleVehicleLocation(NycVehicleLocationRecord record);

  public List<VehicleLocationRecord> getLatestProcessedVehicleLocationRecords();
}
