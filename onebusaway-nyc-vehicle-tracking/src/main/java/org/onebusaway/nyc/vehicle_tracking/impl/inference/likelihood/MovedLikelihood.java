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
package org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood;

import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.springframework.stereotype.Component;

import umontreal.iro.lecuyer.probdist.FoldedNormalDist;

@Component
public class MovedLikelihood implements SensorModelRule {

  @Override
  public SensorModelResult likelihood(Context context) throws BadProbabilityParticleFilterException {
    final SensorModelResult result = new SensorModelResult("pMoved", 1.0);
    final Observation obs = context.getObservation();
    
    final double prob = computeVehicleHasNotMovedProbability(obs);
    if (context.getState().getMotionState().hasVehicleNotMoved()) {
      result.addResultAsAnd("not-moved", prob);
    } else {
      result.addResultAsAnd("moved", Math.min(1d, Math.max(0d, 1d - prob)));
    }
    
    return result;
  }
  
  /**
   * Computes the probability that the vehicle has not moved, relative
   * to the previous observation.
   * TODO: this used to involve the time between observations, and it should
   * probably refer to the last in motion location...
   */
  static public double computeVehicleHasNotMovedProbability(Observation obs) {
    
    final NycRawLocationRecord prevRecord = obs.getPreviousRecord();
    
    if (prevRecord == null) {
      return 0.5d;
    }

    final double d = SphericalGeometryLibrary.distance(
        prevRecord.getLatitude(), prevRecord.getLongitude(),
        obs.getLocation().getLat(), obs.getLocation().getLon());

    /*
     * Although the gps std. dev is reasonable for having not moved, we also
     * have dead-reckoning, which is much more accurate in this regard, so we
     * shrink the gps std. dev.
     * Also, we suspect some numerical issues here, so truncate...
     * TODO: use log results
     */
    final double prob = Math.min(1d, Math.max(0d, 1d - FoldedNormalDist.cdf(0d, GpsLikelihood.gpsStdDev, d)));
    return prob;
  }
}
