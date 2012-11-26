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
 * OBANYC-1797
 * "Total false negative (DEADHEAD rather than IN_PROGRESS) for the 
 * trip from downtown back to prospect park. 
 * But then yes, it leaves the route to deadhead back to depot."
 * 
 * @author bwillard 
 *
 */
@RunWith(RunUntilSuccess.class)
public class Trace_324_20121004_IntegrationTest extends AbstractTraceRunner {

  public Trace_324_20121004_IntegrationTest() throws Exception {
    super("324-deadhead-labeled.csv");
    setBundle("2012September_Bronx_r10_b03", "2012-09-03T01:00:00EDT");
  }
}
