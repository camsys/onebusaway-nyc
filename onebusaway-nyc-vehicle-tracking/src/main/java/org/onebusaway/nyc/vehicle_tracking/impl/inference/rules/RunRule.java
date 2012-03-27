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

import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RunRule implements SensorModelRule {

  public static enum RUN_INFO_STATE {
    NO_RUN_INFO,
    RUN_INFO_NO_RUN,
    OP_RUN_MATCH,
    NULL_OP_FUZZY_MATCH,
    NULL_OP_NO_FUZZY_MATCH,
    NO_OP_FUZZY_MATCH,
    NO_OP_NO_FUZZY_MATCH,
  };

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {
    
    final SensorModelResult result = new SensorModelResult("pRun");
    RUN_INFO_STATE state = getRunInfoState(context);
    
    switch (state) {
      case NO_RUN_INFO:
        result.addResultAsAnd("no run info", 0.9/3d);
        return result;
      case RUN_INFO_NO_RUN:
        result.addResultAsAnd("run-info, no run", 0.25/4d);
        return result;
      case OP_RUN_MATCH:
        result.addResultAsAnd("op. run match", 0.9/3d);
        return result;
      case NULL_OP_FUZZY_MATCH:
        result.addResultAsAnd("null op. fuzzy match", 0.9/3d);
        return result;
      case NULL_OP_NO_FUZZY_MATCH:
        result.addResultAsAnd("null op. no fuzzy match", (0.25/2d) * 0.01);
        return result;
      case NO_OP_FUZZY_MATCH:
        result.addResultAsAnd("no op. fuzzy match", 0.75 * 0.1);
        return result;
      case NO_OP_NO_FUZZY_MATCH:
        result.addResultAsAnd("no matches", 0.25/4d);
        return result;
      default:
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
        return RUN_INFO_STATE.OP_RUN_MATCH;
      } else if (blockState.getOpAssigned() == Boolean.FALSE) {
        if (blockState.getRunReported() == Boolean.TRUE) {
          return RUN_INFO_STATE.NO_OP_FUZZY_MATCH;
        } else {
          return RUN_INFO_STATE.NO_OP_NO_FUZZY_MATCH;
        }
      } else {
        if (blockState.getRunReported() == Boolean.TRUE) {
          return RUN_INFO_STATE.NULL_OP_FUZZY_MATCH;
        } else {
          return RUN_INFO_STATE.NULL_OP_NO_FUZZY_MATCH;
        }
      }
    } else {
      return RUN_INFO_STATE.RUN_INFO_NO_RUN;
    }
  }

}
