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
package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.EnumMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.springframework.stereotype.Component;

@Component
public class ObservationCache {

  public enum EObservationCacheKey {
    STREET_NETWORK_EDGES,
    JOURNEY_START_BLOCK_CDF,
    JOURNEY_IN_PROGRESS_BLOCK_CDF,
    CLOSEST_BLOCK_LOCATION,
    SCHEDULED_BLOCK_LOCATION,
    BLOCK_LOCATION    
  }

  private ConcurrentMap<AgencyAndId, ObservationContents> _contentsByVehicleId = new ConcurrentHashMap<AgencyAndId, ObservationCache.ObservationContents>();

  @SuppressWarnings("unchecked")
  public <T> T getValueForObservation(Observation observation, EObservationCacheKey key) {
    NycVehicleLocationRecord record = observation.getRecord();

    ObservationContents contents = _contentsByVehicleId.get(record.getVehicleId());
    if (contents == null || contents.getObservation() != observation)
      return null;
    return (T) contents.getValueForValueType(key);
  }

  public void putValueForObservation(Observation observation, EObservationCacheKey key,
      Object value) {
    NycVehicleLocationRecord record = observation.getRecord();
    /**
     * This doesn't need to be thread-safe in the strict sense since we should
     * never get concurrent operations for the same vehicle
     */
    ObservationContents contents = _contentsByVehicleId.get(record.getVehicleId());
    if (contents == null || contents.getObservation() != observation) {
      contents = new ObservationContents(observation);
      _contentsByVehicleId.put(record.getVehicleId(), contents);
    }
    contents.putValueForValueType(key, value);
  }

  private static class ObservationContents {

    private final Observation _observation;

    private EnumMap<EObservationCacheKey, Object> _contents = new EnumMap<EObservationCacheKey, Object>(
        EObservationCacheKey.class);

    public ObservationContents(Observation observation) {
      _observation = observation;
    }

    public Observation getObservation() {
      return _observation;
    }

    @SuppressWarnings("unchecked")
    public <T> T getValueForValueType(EObservationCacheKey key) {
      return (T) _contents.get(key);
    }

    public void putValueForValueType(EObservationCacheKey key, Object value) {
      _contents.put(key, value);
    }
  }
}
