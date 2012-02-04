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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.googlecode.concurrentlinkedhashmap.Weighers;

import org.springframework.stereotype.Component;

@Component
public class ObservationCache {

  public enum EObservationCacheKey {
    ROUTE_LOCATION, STREET_NETWORK_EDGES, JOURNEY_START_BLOCK_CDF, JOURNEY_IN_PROGRESS_BLOCK_CDF, JOURNEY_START_BLOCK, JOURNEY_IN_PROGRESS_BLOCK, CLOSEST_BLOCK_LOCATION, SCHEDULED_BLOCK_LOCATION, BLOCK_LOCATION
  }

  /*
   * This is a vehicle-id cache, with timed expiration, that holds
   * an observation cache per vehicle-id.  The observation caches hold only the last two
   * entries.
   * 
   */
  private Cache<AgencyAndId, Cache<Observation, ObservationContents>> _contentsByVehicleId = 
      CacheBuilder.newBuilder()
      .concurrencyLevel(4)
      .expireAfterWrite(30, TimeUnit.MINUTES)
      .build(
             new CacheLoader<AgencyAndId, Cache<Observation, ObservationContents>>() {

              @Override
              public Cache<Observation, ObservationContents> load(AgencyAndId key)
                  throws Exception {
                return CacheBuilder.newBuilder()
                    .concurrencyLevel(1)
                    .weakKeys()
                    .maximumSize(2)
                    .build(
                           new CacheLoader<Observation, ObservationContents> () {

                            @Override
                            public ObservationContents load(Observation key)
                                throws Exception {
                              return new ObservationContents();
                            }
                           }
                         );
              }
               
             }
           );
  

  @SuppressWarnings("unchecked")
  public <T> T getValueForObservation(Observation observation,
      EObservationCacheKey key) {
    NycRawLocationRecord record = observation.getRecord();
    Cache<Observation, ObservationContents> contentsCache = _contentsByVehicleId.getUnchecked(record.getVehicleId());
    ObservationContents contents = contentsCache.getUnchecked(observation);
    
    if (contents == null)
      return null;
    
    return (T) contents.getValueForValueType(key);
  }

  public void putValueForObservation(Observation observation,
      EObservationCacheKey key, Object value) {
    NycRawLocationRecord record = observation.getRecord();
    Cache<Observation, ObservationContents> contentsCache = _contentsByVehicleId.getUnchecked(record.getVehicleId());
    
    ObservationContents contents = contentsCache.getUnchecked(observation);
    
    contents.putValueForValueType(key, value);
  }

  private static class ObservationContents {

    private EnumMap<EObservationCacheKey, Object> _contents = new EnumMap<EObservationCacheKey, Object>(
        EObservationCacheKey.class);

    @SuppressWarnings("unchecked")
    public <T> T getValueForValueType(EObservationCacheKey key) {
      return (T) _contents.get(key);
    }

    public void putValueForValueType(EObservationCacheKey key, Object value) {
      _contents.put(key, value);
    }
  }
  
}
