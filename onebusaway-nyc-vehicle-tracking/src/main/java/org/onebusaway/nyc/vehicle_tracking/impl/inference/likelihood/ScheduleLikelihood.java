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

import org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;

import gov.sandia.cognition.statistics.distribution.StudentTDistribution;

import org.apache.commons.math.util.FastMath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ScheduleLikelihood implements SensorModelRule {

  /*
   * These are all in minutes
   */
  final static private StudentTDistribution schedDevInformalRunDist = new StudentTDistribution(
      2, 0d, 1d / (2d * 250d));
  final static private StudentTDistribution schedDevFormalRunDist = new StudentTDistribution(
      1, 0d, 1d / (2d * 290d));
  final static private StudentTDistribution schedDevDeadheadInformalRunDist = new StudentTDistribution(
      1, 0d, 1d / (2d * 2000d));
  final static private StudentTDistribution schedDevDeadheadFormalRunDist = new StudentTDistribution(
      1, 0d, 1d / (2d * 8900d));

  final static private double pFormal = 0.8d;
  final static private double pDeadhead = 0.3d;

  /*
   * In minutes, as well
   */
  private static final double POS_SCHED_DEV_CUTOFF = 75d;
  private static final double NEG_SCHED_DEV_CUTOFF = -30d;

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
  }

  @Override
  public SensorModelResult likelihood(Context context)
      throws BadProbabilityParticleFilterException {

    final VehicleState state = context.getState();
    final Observation obs = context.getObservation();
    final EVehiclePhase phase = state.getJourneyState().getPhase();
    final BlockState blockState = state.getBlockState();
    final VehicleState parentState = context.getParentState();

    final SensorModelResult result = computeSchedTimeProb(parentState, state,
        blockState, phase, obs);

    return result;
  }

  private SensorModelResult computeSchedTimeProb(VehicleState parentState,
      VehicleState state, BlockState blockState, EVehiclePhase phase,
      Observation obs) throws BadProbabilityParticleFilterException {
    final SensorModelResult result = new SensorModelResult("pSchedule", 1.0);
    if (blockState == null) {
      result.addLogResultAsAnd("null state", 0.0);

    } else {
      final double x = state.getBlockStateObservation().getScheduleDeviation();

      if (EVehiclePhase.DEADHEAD_AFTER == phase) {

        if (FastMath.abs(x) > 0d) {
          final StudentTDistribution schedDist = getSchedDistForBlockState(state.getBlockStateObservation(), false);
          final double pSched = getScheduleDevLogProb(x, state.getBlockStateObservation().isRunFormal(), 
              false, schedDist);
          result.addLogResultAsAnd("deadhead after", pSched);
        } else {
          result.addLogResultAsAnd("deadhead after", 0.0);
        }

      } else if (EVehiclePhase.DEADHEAD_BEFORE == phase) {

        if (FastMath.abs(x) > 0d) {
          final StudentTDistribution schedDist = getSchedDistForBlockState(state.getBlockStateObservation(), false);
          final double pSched = getScheduleDevLogProb(x, state.getBlockStateObservation().isRunFormal(), 
              false, schedDist);
          result.addLogResultAsAnd("deadhead before", pSched);
        } else {
          result.addLogResultAsAnd("deadhead before", 0.0);
        }

      } else if (EVehiclePhase.LAYOVER_BEFORE == phase) {

        if (FastMath.abs(x) > 0d) {
          final StudentTDistribution schedDist = getSchedDistForBlockState(state.getBlockStateObservation(), false);
          final double pSched = getScheduleDevLogProb(x, state.getBlockStateObservation().isRunFormal(), 
              false, schedDist);
          result.addLogResultAsAnd("layover before", pSched);
        } else {
          result.addLogResultAsAnd("layover before", 0.0);
        }

      } else if (EVehiclePhase.DEADHEAD_DURING == phase) {

        final StudentTDistribution schedDist = getSchedDistForBlockState(state.getBlockStateObservation(), false);
        final double pSched = getScheduleDevLogProb(x, state.getBlockStateObservation().isRunFormal(), 
            false, schedDist);
        result.addLogResultAsAnd("deadhead during", pSched);

      } else if (EVehiclePhase.LAYOVER_DURING == phase) {

        final StudentTDistribution schedDist = getSchedDistForBlockState(state.getBlockStateObservation(), false);
        final double pSched = getScheduleDevLogProb(x, state.getBlockStateObservation().isRunFormal(), 
            false, schedDist);
        result.addLogResultAsAnd("layover during", pSched);

      } else if (EVehiclePhase.IN_PROGRESS == phase) {
        final StudentTDistribution schedDist = getSchedDistForBlockState(state.getBlockStateObservation(), false);
        final double pSched = getScheduleDevLogProb(x, state.getBlockStateObservation().isRunFormal(), 
            false, schedDist);
        result.addLogResultAsAnd("in progress", pSched);
      }
    }

    return result;
  }

  /**
   * Manual truncation (when using the formal run distribution) We shouldn't
   * have to worry about normalization, yet.
   */
  public static double getScheduleDevLogProb(final double x, final boolean isFormal, final boolean isDeadhead,
      StudentTDistribution schedDist) {
    final double pSched;
    if (!isFormal || (x <= POS_SCHED_DEV_CUTOFF && x >= NEG_SCHED_DEV_CUTOFF)) {
        
      double mixLogWeight = 0d;
      if (isFormal)
        mixLogWeight += FastMath.log(pFormal);
      else 
        mixLogWeight += FastMath.log1p(-pFormal);
      
      if (isDeadhead)
        mixLogWeight += FastMath.log(pDeadhead);
      else
        mixLogWeight += FastMath.log1p(-pDeadhead);
        
      pSched = mixLogWeight + schedDist.getProbabilityFunction().logEvaluate(x);
    } else {
      pSched = Double.NEGATIVE_INFINITY;
    }
    return pSched;
  }

  public static StudentTDistribution getSchedDistForBlockState(
      BlockStateObservation blockState, final boolean isDeadhead) {
    if (blockState.isRunFormal()) { 
      if (isDeadhead)
        return schedDevDeadheadFormalRunDist;
      else
        return schedDevFormalRunDist;
    } else {
      if (isDeadhead)
        return schedDevDeadheadInformalRunDist;
      else
        return schedDevInformalRunDist;
    }
  }

  public static StudentTDistribution getSchedDevNonRunDist() {
    return schedDevInformalRunDist;
  }

  public static double truncateTime(double d, boolean isFormal) {
    if (!isFormal) {
      if (d > POS_SCHED_DEV_CUTOFF) {
        return Double.POSITIVE_INFINITY;
      } else if (d < NEG_SCHED_DEV_CUTOFF) {
        return -Double.NEGATIVE_INFINITY;
      }
    }
    return d;
  }

}
