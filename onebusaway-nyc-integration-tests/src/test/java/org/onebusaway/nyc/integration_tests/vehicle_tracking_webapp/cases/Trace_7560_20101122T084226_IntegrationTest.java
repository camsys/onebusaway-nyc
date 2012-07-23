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

import org.junit.runner.RunWith;
import org.onebusaway.nyc.integration_tests.RunUntilSuccess;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

/**
 * At end of block:
 * 
 * Trip # 2008_20101024EE_086200_B63_0002_B63_13
 * 
 * Block # 2008_12888417
 * 
 * DSC for deadhead back to base is valid so IN_PROGRESS/deviated is fine.
 * But why the few updates with DEADHEAD_AFTER before it gets to that state?
 * 
 * Also, we're fine with in-progress deviated instead of deadhead-after
 * 
 * @author bdferris
 */
@RunWith(RunUntilSuccess.class)
public class Trace_7560_20101122T084226_IntegrationTest extends AbstractTraceRunner {

  public Trace_7560_20101122T084226_IntegrationTest() throws Exception {
    super("7560-2010-11-22T08-42-26.csv");
    setBundle("b63-winter10", "2010-12-20T00:00:00EDT");
  }
}
