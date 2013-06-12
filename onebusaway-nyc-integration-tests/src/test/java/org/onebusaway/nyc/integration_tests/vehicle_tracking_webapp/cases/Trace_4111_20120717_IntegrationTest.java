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

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

/**
 * Inferred block location issue while in LAYOVER_BEFORE
 * 
 * @author jmaki
 *
 */
public class Trace_4111_20120717_IntegrationTest extends AbstractTraceRunner {

  public Trace_4111_20120717_IntegrationTest() throws Exception {
    super("4111-2012-07-17.csv");
    setBundle("2012July_r04_b02", "2012-07-17T00:00:00EDT");
  }
}
