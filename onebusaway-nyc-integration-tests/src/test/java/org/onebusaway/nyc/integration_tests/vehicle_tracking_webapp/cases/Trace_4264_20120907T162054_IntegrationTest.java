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

@RunWith(RunUntilSuccess.class)
public class Trace_4264_20120907T162054_IntegrationTest extends AbstractTraceRunner {

  public Trace_4264_20120907T162054_IntegrationTest() throws Exception {
    super("4264-20120907T162054.csv");
    setBundle("2012September", "2012-09-04T00:00:00EDT");
  }
}
