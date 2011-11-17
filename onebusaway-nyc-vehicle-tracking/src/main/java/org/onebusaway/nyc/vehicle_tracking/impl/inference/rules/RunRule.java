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

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.*;

import java.util.Date;

import org.onebusaway.nyc.transit_data_federation.model.tdm.OperatorAssignmentItem;
import org.onebusaway.nyc.transit_data_federation.services.tdm.OperatorAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
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

    VehicleState state = context.getState();
    BlockState blockState = state.getBlockState();
    SensorModelResult result = new SensorModelResult("pRun");

    /**
     * Weigh matched run's higher
     */
    if (blockState != null) {
      /*
       * TODO We might want to do something different the
       * run info wasn't even determined.
       */
      if (blockState.getOpAssigned() == null
          || blockState.getRunReported() == null) {
        result.addResultAsAnd("run-info status was not determined", 0.25);
        return result;
      }
    
      if (blockState.getOpAssigned()) {
        /*
         * if the run for this block matches the
         * schedule, then it should not be reduced
         * in likelihood.
         */
        result.addResultAsAnd("operator assigned", 1.0);
      } else if (blockState.getRunReported()){
        /*
         * if the run for this block isn't in the
         * schedule for this driver, but the driver
         * reports (in a fuzzy sense) this run, then
         * make operator assigned runs 2 times more 
         * likely than simply reported runs.
         */
        result.addResultAsAnd("run reported (fuzzy)", 0.5);
      } else {
        result.addResultAsAnd("no run info provided", 0.25);
      }
    } else {
      /*
       * if this block's run is neither reported nor 
       * scheduled, then make op assigned runs 4 times 
       * more likely, and reported runs 2 times more likely.
       */
      result.addResultAsAnd("no run info provided", 0.25);
    }
    
    return result;
  }

}
