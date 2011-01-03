package org.onebusaway.nyc.vehicle_tracking.model;

import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;

public class RecordLibrary {

  public static NycTestLocationRecord getVehicleLocationRecordAsNycTestLocationRecord(
      VehicleLocationRecord record) {
    NycTestLocationRecord r = new NycTestLocationRecord();
    return r;
  }

  public static VehicleLocationRecord getNycTestLocationRecordAsVehicleLocationRecord(
      NycTestLocationRecord record) {

    VehicleLocationRecord vlr = new VehicleLocationRecord();
    vlr.setTimeOfRecord(record.getTimestamp());
    vlr.setTimeOfLocationUpdate(record.getTimestamp());
    vlr.setBlockId(AgencyAndIdLibrary.convertFromString(record.getInferredBlockId()));
    vlr.setServiceDate(record.getInferredServiceDate());
    vlr.setDistanceAlongBlock(record.getInferredDistanceAlongBlock());
    vlr.setCurrentLocationLat(record.getInferredLat());
    vlr.setCurrentLocationLon(record.getInferredLon());
    vlr.setPhase(EVehiclePhase.valueOf(record.getInferredPhase()));
    vlr.setStatus(record.getInferredStatus());
    return vlr;
  }
}
