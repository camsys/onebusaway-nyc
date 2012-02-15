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
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RunRule implements SensorModelRule {

  private OperatorAssignmentService _operatorAssignmentService;

  @Autowired
  public void setOperatorAssignmentService(
      OperatorAssignmentService operatorAssignmentService) {
    _operatorAssignmentService = operatorAssignmentService;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) {

    final VehicleState state = context.getState();
    final BlockStateObservation blockState = state.getBlockStateObservation();
    final SensorModelResult result = new SensorModelResult("pRun");

    /**
     * Weigh matched run's higher
     */
    if (blockState != null) {
      if (blockState.getOpAssigned() == null
          && blockState.getRunReported() == null) {
        result.addResultAsAnd("run-info status was not determined", 0.5);
        return result;
      }

      if (blockState.getOpAssigned() == Boolean.TRUE) {
        /*
         * If the run for this block matches the schedule, then it should not be
         * reduced in likelihood.
         */
        result.addResultAsAnd("operator assigned", 1.0);
      } else if (blockState.getRunReported() == Boolean.TRUE) {
        if (state.getObservation().getFuzzyMatchDistance() == 0)
          result.addResultAsAnd("run reported (fuzzy)", 0.9);
        else
          result.addResultAsAnd("run reported (fuzzy)", 0.6);
      } else if (blockState.getRunReported() == Boolean.FALSE
          || blockState.getOpAssigned() == Boolean.FALSE) {
        result.addResultAsAnd("no run info matches", 0.5);
      }
    } else {

      if (state.getObservation().isOutOfService()) {
        result.addResultAsAnd("NA (out-of-service)", 1.0);
        return result;
      }

      /*
       * We were given run information, and we were able to find valid info for
       * it, so we penalize for not using it.
       */
      if (!StringUtils.isBlank(state.getObservation().getOpAssignedRunId())
          || (state.getObservation().getFuzzyMatchDistance() != null && state.getObservation().getFuzzyMatchDistance() <= 1)) {
        result.addResultAsAnd("not using results from run info", 0.5);
      } else {
        result.addResultAsAnd("no good results from run info", 1.0);
      }
    }

    return result;
  }

}
