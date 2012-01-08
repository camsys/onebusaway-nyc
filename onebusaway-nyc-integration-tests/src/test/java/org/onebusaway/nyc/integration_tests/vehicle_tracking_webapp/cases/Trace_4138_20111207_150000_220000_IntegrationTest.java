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
 * Another interesting case of mid-trip entry.
 * 
 * @author bwillard
 * */
public class Trace_4138_20111207_150000_220000_IntegrationTest extends AbstractTraceRunner {

  public Trace_4138_20111207_150000_220000_IntegrationTest() throws Exception {
    super("4138_20111207_150000_220000.csv.gz");
    setBundle("si", "2011-12-14T00:00:00EDT");
    setLoops(2);
    setMinAccuracyRatioForPhase(EVehiclePhase.IN_PROGRESS, 0.91);
//    setMinAccuracyRatioForPhase(EVehiclePhase.AT_BASE, 0.86);
    setMinAccuracyRatioForPhase(EVehiclePhase.DEADHEAD_BEFORE, 0.82);
    /**
     * Whether it should be layover or deadhead is still a question,
     * but in general it doesn't hurt us (since it's a block-less state).
     */
    setMinAccuracyRatioForPhase(EVehiclePhase.LAYOVER_BEFORE, 0.97);
  }
}
