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
 * A bus that starts at base, goes out on an unknown route (bundle-wise), and comes back.
 * Should produce no UI-mappable inference results.
 * 
 * @author jmaki
 *
 */

@RunWith(RunUntilSuccess.class)
public class Trace_1404_20101210T034249_IntegrationTest extends AbstractTraceRunner {

  public Trace_1404_20101210T034249_IntegrationTest() throws Exception {
    super("1404-2010-12-10T03-42-49.csv");
    setBundle("b63-winter10", "2010-12-20T00:00:00EDT");
  }
}
