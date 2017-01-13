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
 * OBANYC-2257 Ensure changed DSC results in correct trip ID inference
 *
 *
 */
public class Trace_4222_Incorrect_Dsc_IntegrationTest extends AbstractTraceRunner {

  public Trace_4222_Incorrect_Dsc_IntegrationTest() throws Exception {
    super("4222-incorrect-dsc.csv");
    setBundle("2014Sept_Prod_r08_b01", "2014-09-11T08:00:00EDT");
  }
}
