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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class BlockLikelihood implements SensorModelRule {

  public static enum BLOCK_INFO_STATE {
    NO_BLOCK_INFO, BLOCK_MATCH, NO_BLOCK_MATCH, BLOCK_INFO_NO_BLOCK,
  };

  @Override
  public SensorModelResult likelihood(Context context) throws BadProbabilityParticleFilterException {

    final SensorModelResult result = new SensorModelResult("pBlock");
    final BLOCK_INFO_STATE state = getBlockInfoState(context);

    switch (state) {
      case NO_BLOCK_INFO:
        result.addResultAsAnd("no block info", 1d);
        return result;
      case BLOCK_MATCH:
        result.addResultAsAnd("block match", (7d/20d));
        return result;
      case NO_BLOCK_MATCH:
        result.addResultAsAnd("no block match", (5d/20d));
        return result;
      case BLOCK_INFO_NO_BLOCK:
        result.addResultAsAnd("block-info, but no block", (2d/20d));
        return result;
      default:
        return null;
    }
  }

  public static BLOCK_INFO_STATE getBlockInfoState(Context context) {
    final VehicleState state = context.getState();
    final BlockStateObservation blockState = state.getBlockStateObservation();
    final Observation obs = context.getObservation();

    String assignedBlockId = obs.getAssignedBlockId();
    String inferredBlockId = null;
    if (blockState != null) {
      inferredBlockId = AgencyAndId.convertToString(
          blockState
          .getBlockState()
          .getBlockInstance()
          .getBlock()
          .getBlock()
          .getId()
          );
    }

    if (StringUtils.isEmpty(assignedBlockId))
      return BLOCK_INFO_STATE.NO_BLOCK_INFO;

    return BLOCK_INFO_STATE.NO_BLOCK_INFO;
    return BLOCK_INFO_STATE.BLOCK_MATCH;
    return BLOCK_INFO_STATE.NO_BLOCK_MATCH;
    return BLOCK_INFO_STATE.BLOCK_INFO_NO_BLOCK;

    // TODO: BLOCK_INFO_NO_BLOCK means we have an assigned block but it doesn't match the current bundle
    
//    if (blockState != null) {
//      if (blockState.getOpAssigned() == Boolean.TRUE) {
//        return BLOCK_INFO_STATE.FORMAL_RUN_MATCH;
//      } else {
//        if (blockState.getRunReported() == Boolean.TRUE) {
//          return BLOCK_INFO_STATE.NO_FORMAL_FUZZY_MATCH;
//        } else {
//          return BLOCK_INFO_STATE.NO_FORMAL_NO_FUZZY_MATCH;
//        }
//      }
//    } else {
//      return BLOCK_INFO_STATE.RUN_INFO_NO_RUN;
//    }
    
    return BLOCK_INFO_STATE.BLOCK_INFO_NO_BLOCK;
  }

}
