/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.model.library;

import org.onebusaway.nyc.transit_data_federation.model.NycInferredLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
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

  public static NycInferredLocationRecord getVehicleLocationRecordAsNycTestLocationRecord(
      VehicleLocationRecord record) {
    NycInferredLocationRecord r = new NycInferredLocationRecord();
    return r;
  }

  public static VehicleLocationRecord getNycTestLocationRecordAsVehicleLocationRecord(
      NycInferredLocationRecord record) {

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
    vlr.setVehicleId(record.getVehicleId());
    return vlr;
  }

  public static NycRawLocationRecord getNycTestLocationRecordAsNycVehicleLocationRecord(
      NycInferredLocationRecord record) {

    NycRawLocationRecord vlr = new NycRawLocationRecord();
    vlr.setDestinationSignCode(record.getDsc());
    vlr.setLatitude(record.getLat());
    vlr.setLongitude(record.getLon());
    vlr.setTime(record.getTimestamp());
    vlr.setTimeReceived(record.getTimestamp());
    vlr.setVehicleId(record.getVehicleId());
    return vlr;
  }
}
