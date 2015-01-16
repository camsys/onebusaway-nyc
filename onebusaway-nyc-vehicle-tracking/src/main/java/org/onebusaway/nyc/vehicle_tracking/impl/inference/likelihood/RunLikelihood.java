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

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RunLikelihood implements SensorModelRule {

  private final Logger _log = LoggerFactory.getLogger(RunLikelihood.class);
  
  public static enum RUN_INFO_STATE {
    NO_RUN_INFO, RUN_INFO_NO_RUN, FORMAL_RUN_MATCH, NO_FORMAL_FUZZY_MATCH, NO_FORMAL_NO_FUZZY_MATCH,
  };

  @Override
  public SensorModelResult likelihood(Context context) throws BadProbabilityParticleFilterException {

    final SensorModelResult result = new SensorModelResult("pRun");
    final RUN_INFO_STATE state = getRunInfoState(context);

    switch (state) {
      case NO_RUN_INFO:
        _log.debug("no run info");
        result.addResultAsAnd("no run info", 1d);
        return result;
      case FORMAL_RUN_MATCH:
        result.addResultAsAnd(">= operator run match", (7d/20d));
        return result;
      case NO_FORMAL_FUZZY_MATCH:
        result.addResultAsAnd("just fuzzy match", (6d/20d));
        return result;
      case NO_FORMAL_NO_FUZZY_MATCH:
        result.addResultAsAnd("no matches", (5d/20d));
        return result;
      case RUN_INFO_NO_RUN:
        result.addResultAsAnd("run-info, but no run", (2d/20d));
        return result;
      default:
        _log.debug("run fall through");
        return null;
    }
  }

  public static RUN_INFO_STATE getRunInfoState(Context context) {
    final VehicleState state = context.getState();
    final BlockStateObservation blockState = state.getBlockStateObservation();
    final Observation obs = context.getObservation();

    if (StringUtils.isEmpty(obs.getOpAssignedRunId())
        && obs.getFuzzyMatchDistance() == null)
      return RUN_INFO_STATE.NO_RUN_INFO;

    if (blockState != null) {
      if (blockState.getOpAssigned() == Boolean.TRUE) {
        return RUN_INFO_STATE.FORMAL_RUN_MATCH;
      } else {
        if (blockState.getRunReported() == Boolean.TRUE) {
          return RUN_INFO_STATE.NO_FORMAL_FUZZY_MATCH;
        } else {
          return RUN_INFO_STATE.NO_FORMAL_NO_FUZZY_MATCH;
        }
      }
    } else {
      return RUN_INFO_STATE.RUN_INFO_NO_RUN;
    }
  }

}
