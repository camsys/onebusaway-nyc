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
package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases.AbstractTraceRunner;
import org.onebusaway.realtime.api.EVehiclePhase;

public class BundleSwitchingIntegrationTest_TestTrace1 extends AbstractTraceRunner {

  public BundleSwitchingIntegrationTest_TestTrace1() throws Exception {
    super("7564-2010-12-02T03-40-55.csv.gz");
    setLoops(1);
    // Looks like we need to do this because the tests are
    // loaded once and run twice, so the results will differ
    setSeeds();
    setMinAccuracyRatioForPhase(EVehiclePhase.LAYOVER_DURING, 0.80);
  }
}
