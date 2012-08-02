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
package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import umontreal.iro.lecuyer.probdist.FoldedNormalDist;

@Component
public class GpsLikelihood implements SensorModelRule {

  final static public double gpsStdDev = 45.0 / 2d;

  final private double inProgressGpsMean = 3.0;

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) throws BadProbabilityParticleFilterException {

    final VehicleState state = context.getState();
    final Observation obs = context.getObservation();
    final BlockState blockState = state.getBlockState();
    EVehiclePhase phase = state.getJourneyState().getPhase();

    final SensorModelResult result = new SensorModelResult("pGps", 1.0);

    if (blockState == null) {
      result.addResultAsAnd("gps(no state)", 1.0);

    } else if (EVehiclePhase.DEADHEAD_AFTER == phase) {
      final double pGps;
      if (state.getBlockStateObservation().isSnapped()) {
        final CoordinatePoint p1 = blockState.getBlockLocation().getLocation();
        final ProjectedPoint p2 = obs.getPoint();
        final double d = SphericalGeometryLibrary.distance(p1.getLat(),
            p1.getLon(), p2.getLat(), p2.getLon());
        pGps = FoldedNormalDist.density(inProgressGpsMean, gpsStdDev, d);
        result.addResultAsAnd("gps(deadhead-after)", pGps);

      } else {
        result.addResultAsAnd("gps(deadhead-after)", 1.0);
      }

    } else if (EVehiclePhase.AT_BASE == phase) {
      result.addResultAsAnd("gps(at-base)", 1.0);

    } else if (EVehiclePhase.IN_PROGRESS == phase) {

      final CoordinatePoint p1 = blockState.getBlockLocation().getLocation();
      final ProjectedPoint p2 = obs.getPoint();
      final double d = SphericalGeometryLibrary.distance(p1.getLat(),
          p1.getLon(), p2.getLat(), p2.getLon());
      final double pGps = FoldedNormalDist.density(inProgressGpsMean,
          gpsStdDev, d);
      result.addResultAsAnd("gps(in-progress)", pGps);

    } else if (EVehiclePhase.DEADHEAD_BEFORE == phase) {
      final double pGps;
      if (state.getBlockStateObservation().isSnapped()) {
        final CoordinatePoint p1 = blockState.getBlockLocation().getLocation();
        final ProjectedPoint p2 = obs.getPoint();
        final double d = SphericalGeometryLibrary.distance(p1.getLat(),
            p1.getLon(), p2.getLat(), p2.getLon());
        pGps = FoldedNormalDist.density(inProgressGpsMean, gpsStdDev, d);
        result.addResultAsAnd("gps(deadhead-before)", pGps);

      } else {
        result.addResultAsAnd("gps(deadhead-before)", 1.0);
      }

    } else if (EVehiclePhase.LAYOVER_BEFORE == phase) {
      final double pGps;
      if (state.getBlockStateObservation().isSnapped()) {
        final CoordinatePoint p1 = blockState.getBlockLocation().getLocation();
        final ProjectedPoint p2 = obs.getPoint();
        final double d = SphericalGeometryLibrary.distance(p1.getLat(),
            p1.getLon(), p2.getLat(), p2.getLon());
        pGps = FoldedNormalDist.density(inProgressGpsMean, gpsStdDev, d);

        result.addResultAsAnd("gps(layover-before)", pGps);
      } else {
        result.addResultAsAnd("gps(layover-before)", 1.0);
      }

    } else if (EVehiclePhase.DEADHEAD_DURING == phase) {

      /*
       * What if the bus turned around? This attempts to handle that.
       */
      final double pGps;
      if (state.getBlockStateObservation().isSnapped()) {
        final CoordinatePoint p1 = blockState.getBlockLocation().getLocation();
        final ProjectedPoint p2 = obs.getPoint();
        final double d = SphericalGeometryLibrary.distance(p1.getLat(),
            p1.getLon(), p2.getLat(), p2.getLon());
        pGps = FoldedNormalDist.density(inProgressGpsMean, gpsStdDev, d);
        result.addResultAsAnd("gps(deadhead-during initial)", pGps);

      } else {
        result.addResultAsAnd("gps(deadhead-during initial)", 1.0);
      }

    } else if (EVehiclePhase.LAYOVER_DURING == phase) {

      final double pGps;
      if (state.getBlockStateObservation().isSnapped()) {
        final CoordinatePoint p1 = blockState.getBlockLocation().getLocation();
        final ProjectedPoint p2 = obs.getPoint();
        final double d = SphericalGeometryLibrary.distance(p1.getLat(),
            p1.getLon(), p2.getLat(), p2.getLon());
        pGps = FoldedNormalDist.density(inProgressGpsMean, gpsStdDev, d);
        result.addResultAsAnd("gps(layover-during initial)", pGps);

      } else {
        result.addResultAsAnd("gps(layover-during initial)", 1.0);
      }
    }
    return result;
  }

}
