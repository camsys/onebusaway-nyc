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

import java.util.Comparator;
import java.util.EnumMap;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.springframework.stereotype.Component;

import com.google.common.base.Predicate;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Iterables;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.primitives.Longs;

@Component
public class ObservationCache {

  public enum EObservationCacheKey {
    ROUTE_LOCATION, STREET_NETWORK_EDGES, JOURNEY_START_BLOCK_CDF, JOURNEY_IN_PROGRESS_BLOCK_CDF, JOURNEY_START_BLOCK, JOURNEY_IN_PROGRESS_BLOCK, CLOSEST_BLOCK_LOCATION, SCHEDULED_BLOCK_LOCATION, BLOCK_LOCATION, BEST_BLOCK_STATES
  }

  /*
   * This is a vehicle-id cache, with timed expiration, that holds an
   * observation cache per vehicle-id. The observation caches hold only the last
   * two entries.
   */
  private final LoadingCache<AgencyAndId, MinMaxPriorityQueue<ObservationContents>> _contentsByVehicleId = CacheBuilder.newBuilder().concurrencyLevel(
      4).expireAfterWrite(30, TimeUnit.MINUTES).build(
      new CacheLoader<AgencyAndId, MinMaxPriorityQueue<ObservationContents>>() {

        @Override
        public MinMaxPriorityQueue<ObservationContents> load(
            AgencyAndId key) throws Exception {
          
          return MinMaxPriorityQueue.orderedBy(new Comparator<ObservationContents>() {
            @Override
            public int compare(ObservationContents o1, ObservationContents o2) {
              return -Longs.compare(o1.getObservation().getTime(), o2.getObservation().getTime());
            }
            
          }).maximumSize(2).create();
        }

      });

  @SuppressWarnings("unchecked")
  public <T> T getValueForObservation(Observation observation,
      EObservationCacheKey key) {
    final NycRawLocationRecord record = observation.getRecord();
    final MinMaxPriorityQueue<ObservationContents> contentsCache =  _contentsByVehicleId.getUnchecked(record.getVehicleId());
    /*
     * Synchronize for integration tests, which can do crazy things.
     */
    final ObservationContents contents;
    synchronized (contentsCache) {
      contents = Iterables.find(contentsCache, new ObsSearch(observation), null);
      
      if (contents == null) {
        /*
         *  Add this new observation content, which, if it's truly a new obs, 
         *  will be added later
         */
        if (!contentsCache.offer(new ObservationContents(observation))) {
          return null;
        }
        return null;
      }
    }

    return (T) contents.getValueForValueType(key);
  }

  public void putValueForObservation(Observation observation,
      EObservationCacheKey key, Object value) {
    final NycRawLocationRecord record = observation.getRecord();
    final MinMaxPriorityQueue<ObservationContents> contentsCache = _contentsByVehicleId.getUnchecked(record.getVehicleId());

    /*
     * Synchronize for integration tests, which can do crazy things.
     */
    ObservationContents contents;
    synchronized (contentsCache) {
      contents = Iterables.find(contentsCache, new ObsSearch(observation), null);
      
      if (contents == null) {
        contents = new ObservationContents(observation);
        /*
         * If we attempt to add observations that are behind our current, 
         * then just return
         */
        if (!contentsCache.offer(contents)) {
          return;
        }
      } 
    }

    contents.putValueForValueType(key, value);
  }

  private static class ObsSearch implements Predicate<ObservationContents> {

    private final Observation obsToFind;
    
    public ObsSearch(Observation obsToFind) {
      this.obsToFind = obsToFind;
    }
    
    @Override
    public boolean apply(@Nonnull ObservationContents input) {
      return input.getObservation().getTime() == obsToFind.getTime();
    }
    
  }
  
  private static class ObservationContents {

    private final Observation observation;
    
    public ObservationContents(Observation observation) {
      this.observation = observation;
    }
    
    private final EnumMap<EObservationCacheKey, Object> _contents = new EnumMap<EObservationCacheKey, Object>(
        EObservationCacheKey.class);

    @SuppressWarnings("unchecked")
    public <T> T getValueForValueType(EObservationCacheKey key) {
      return (T) _contents.get(key);
    }

    public void putValueForValueType(EObservationCacheKey key, Object value) {
      _contents.put(key, value);
    }

    @Override
    public String toString() {
      return "ObservationContents [obsTime=" + observation.getTime() + ", _contents=" + _contents.keySet() + "]";
    }

    public Observation getObservation() {
      return observation;
    }
  }

  public void purge(AgencyAndId vehicleId) {
    final MinMaxPriorityQueue<ObservationContents> contentsCache = _contentsByVehicleId.getUnchecked(vehicleId);
    if (contentsCache != null) {
      contentsCache.clear();
    }
  }

}
