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

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.onebusaway.nyc.integration_tests.RunUntilSuccess;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

/**
 * In this test the bus is deadheading back to Staten Island but showing an in-service destination sign.
 * It's eXpress/commuter service, which is uni-directional.
 * 
 * So, for a couple of short intervals where the deadhead route is tangent to the in-service route it is briefly inferred as in_progress.
 * This is acceptable (since driver should just be changing the DSC) but if something could be done about this it would be great.
 * 
 * To fix:
 * 1) Bus should not be able to enter layover near the base with a non-in-service DSC around 5:44

 * 2) If we can prevent the bus from showing in_progress in a few places where it starts to go the same 
 * way as the route back to staten island (it is actually deadheading with a valid in-service DSC), that would be great.
 * But that's a lower priority.
 * 
 * @author jmaki
 *
 */

@Ignore
@RunWith(RunUntilSuccess.class)
public class Trace_2711_20111208_054046_102329_IntegrationTest extends AbstractTraceRunner {

  public Trace_2711_20111208_054046_102329_IntegrationTest() throws Exception {
    super("2711_20111208_054046_102329.csv");
    setBundle("si", "2011-12-07T00:00:00EDT");
  }
}
