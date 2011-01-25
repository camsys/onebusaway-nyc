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
package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;

/**
 * Methods for persisting and querying raw nyc vehicle location records from a
 * database.
 * 
 * @author bdferris
 * @see NycVehicleLocationRecord
 */
public interface VehicleTrackingMutableDao {

  /**
   * Persist the specified record to the database
   * 
   * @param record
   */
  public void saveOrUpdateVehicleLocationRecord(NycVehicleLocationRecord record);

  /**
   * @param timeFrom - unix time (ms)
   * @param timeTo - unix time (ms)
   * @return records in the specified time range
   */
  public List<NycVehicleLocationRecord> getRecordsForTimeRange(long timeFrom,
      long timeTo);

}
