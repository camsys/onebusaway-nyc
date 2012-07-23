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
 * In this test the bus is deadheading back to Staten Island but showing an in-service destination sign.
 * It's eXpress/commuter service, which is uni-directional.
 * So, for a couple of short intervals where the deadhead route is tangent to the in-service route it is briefly inferred as in_progress.
 * This is acceptable (since driver should just be changing the DSC) but if something could be done about this it would be great.
 * @author jmaki
 *
 */

@RunWith(RunUntilSuccess.class)
public class Trace_2711_20111208_054046_102329_IntegrationTest extends AbstractTraceRunner {

  public Trace_2711_20111208_054046_102329_IntegrationTest() throws Exception {
    super("2711_20111208_054046_102329.csv");
    setBundle("si", "2011-12-07T00:00:00EDT");
  }
}
