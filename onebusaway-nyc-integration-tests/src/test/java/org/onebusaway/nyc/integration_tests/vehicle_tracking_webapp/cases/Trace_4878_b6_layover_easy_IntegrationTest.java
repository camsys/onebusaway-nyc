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
 * Standard run of the mill run on the B63. No formal inference.
 * 
 * @author jmaki
 *
 */
public class Trace_4878_b6_layover_easy_IntegrationTest extends AbstractTraceRunner {

  public Trace_4878_b6_layover_easy_IntegrationTest() throws Exception {
    super("4878-b6-layover-easy.csv");
    setBundle("2016Jan_Prod_r03_b6_A6_Sunday", "2016-03-06T04:15:00EDT");
  }
}
