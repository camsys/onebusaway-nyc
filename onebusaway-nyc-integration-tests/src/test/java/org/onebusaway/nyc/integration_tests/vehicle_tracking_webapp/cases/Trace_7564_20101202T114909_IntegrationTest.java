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
 * Trace: 7564-2010-12-02T11-49-09
 * 
 * block = 2008_12888581
 * 
 * Interesting trace because it goes off route (deviation onto 4th Ave from 60th
 * to 50th)
 * 
 * @author bdferris
 */
public class Trace_7564_20101202T114909_IntegrationTest extends AbstractTraceRunner {

  public Trace_7564_20101202T114909_IntegrationTest() throws Exception {
    super("7564-2010-12-02T11-49-09.csv.gz");
    setBundle("b63-winter10", "2010-12-20T00:00:00EDT");
    
    /*
     * FIXME unresolved deadhead/layover-before conflict.  need a 
     * good decision on which is correct. 
     * also, some in-progress & deviated/deadhead conflicts.
     */
    setMinAccuracyRatioForPhase(EVehiclePhase.IN_PROGRESS, 0.92);
//    setMinAccuracyRatioForPhase(EVehiclePhase.DEADHEAD_BEFORE, 0.94);
    setMinAccuracyRatioForPhase(EVehiclePhase.DEADHEAD_AFTER, 0.86);
//    setMinAccuracyRatioForPhase(EVehiclePhase.LAYOVER_BEFORE, 0.94);
  }
}
