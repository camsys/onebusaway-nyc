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
 * Starts in progress
 * 
 * Trip # 2008_20101024EE_087600_B63_0089_B70_13
 * 
 * Block # 2008_12888409
 * 
 * @author bdferris
 */
public class Trace_7563_20101209T124732_IntegrationTest extends
    AbstractTraceRunner {

  public Trace_7563_20101209T124732_IntegrationTest() throws Exception {
    super("7563-2010-12-09T12-47-32.csv.gz");
    setBundle("b63-winter10", "2010-12-20T00:00:00EDT");

    setLoops(10);
    // This test is noisy
    setMinAccuracyRatioForPhase(EVehiclePhase.LAYOVER_DURING, 0.85);
  }
}
