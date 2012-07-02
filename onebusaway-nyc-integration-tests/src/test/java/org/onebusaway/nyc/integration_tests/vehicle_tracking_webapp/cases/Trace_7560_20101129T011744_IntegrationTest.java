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
package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.realtime.api.EVehiclePhase;

/**
 * Needs work - particle filter crash
 * 
 * @author bdferris
 * 
 */
public class Trace_7560_20101129T011744_IntegrationTest extends AbstractTraceRunner {

  public Trace_7560_20101129T011744_IntegrationTest() throws Exception {
    super("7560-2010-11-29T01-17-44.csv.gz");
    setBundle("b63-winter10", "2010-12-20T00:00:00EDT");
    /*
     * FIXME this has a potential bundle-conflict with the trace 
     * this case may be more conceptual, since we mismatch some
     * deadhead-before's with the trace at the end of a block.
     * we might want to change the isAtTerminal condition to be
     * less authoritative (in VehicleStateLibrary).
     */
    setMinAccuracyRatioForPhase(EVehiclePhase.DEADHEAD_BEFORE, 0.92);
    setMinAccuracyRatioForPhase(EVehiclePhase.IN_PROGRESS, 0.92);
    setMinAccuracyRatioForPhase(EVehiclePhase.LAYOVER_DURING, 0.92);
  }
}
