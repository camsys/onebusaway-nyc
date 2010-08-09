package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

import org.onebusaway.realtime.api.VehicleLocationRecord;

public interface VehicleLocationInferenceService {

  public void handleVehicleLocation(VehicleLocationInferenceRecord record);

  public List<VehicleLocationRecord> getLatestProcessedVehicleLocationRecords();
}
