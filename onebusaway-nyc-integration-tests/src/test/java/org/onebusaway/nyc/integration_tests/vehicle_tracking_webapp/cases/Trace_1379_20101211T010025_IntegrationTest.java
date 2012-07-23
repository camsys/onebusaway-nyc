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
 * A route not in the active bundle--should return no UI-mappable IE results.
 * e.g. all results should be DEADHEAD* or anything except IN_PROGRESS.
 * 
 * @author jmaki
 *
 */
@RunWith(RunUntilSuccess.class)
public class Trace_1379_20101211T010025_IntegrationTest extends AbstractTraceRunner {

  public Trace_1379_20101211T010025_IntegrationTest() throws Exception {
    super("1379-2010-12-11T01-00-25.csv");
    setBundle("b63-winter10", "2010-12-20T00:00:00EDT");
  }
}
