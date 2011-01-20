package org.onebusaway.nyc.vehicle_tracking.model;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;

public class RecordLibrary {

  /**
   * We favor the device timestamp except in instances where the device
   * timestamp is wildly divergent from the received timestamp, indicating a
   * possible device error. In those situations we trust our own timestamp more.
   * 
   * @param timeDevice
   * @param timeReceived
   * @return
   */
  public static long getBestTimestamp(long timeDevice, long timeReceived) {
    if (Math.abs(timeReceived - timeDevice) > 30 * 60 * 1000)
      return timeReceived;
    return timeDevice;
  }

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
    vlr.setTripId(AgencyAndIdLibrary.convertFromString(record.getInferredTripId()));
    vlr.setServiceDate(record.getInferredServiceDate());
    vlr.setDistanceAlongBlock(record.getInferredDistanceAlongBlock());
    vlr.setCurrentLocationLat(record.getLat());
    vlr.setCurrentLocationLon(record.getLon());
    vlr.setPhase(EVehiclePhase.valueOf(record.getInferredPhase()));
    vlr.setStatus(record.getInferredStatus());
    return vlr;
  }

  public static NycVehicleLocationRecord getNycTestLocationRecordAsNycVehicleLocationRecord(
      NycTestLocationRecord record, String agencyId) {

    NycVehicleLocationRecord vlr = new NycVehicleLocationRecord();
    vlr.setDestinationSignCode(record.getDsc());
    vlr.setLatitude(record.getLat());
    vlr.setLongitude(record.getLon());
    vlr.setTime(record.getTimestamp());
    vlr.setTimeReceived(record.getTimestamp());
    vlr.setVehicleId(new AgencyAndId(agencyId, record.getVehicleId()));
    return vlr;
  }
}
