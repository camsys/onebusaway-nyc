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
 * OBANYC-1807
 * "This one I don't really understand at all. After the end of the westbound trip 
 *  it does its layover/turnaround up Bailey Ave which shows up as a detour. This 
 *  probably shouldn't be a detour but that doesn't really matter right now (and 
 *  trace is not labeled for this section). When it comes back to the route at 
 *  11:23 it is then deadhead, even though the assigned_run_id of BX12-5 has a trip 
 *  leaving that location at 11:18. Only problem with the inputs from the field is 
 *  that the DSC stayed at 3125 when it should have changed to 3124. The result 
 *  should be formal, and should be correcting the DSC."
 * 
 * @author bwillard 
 *
 */
public class Trace_5799_20121011_IntegrationTest extends AbstractTraceRunner {

  public Trace_5799_20121011_IntegrationTest() throws Exception {
    super("5799-deadhead-labeled.csv");
    setBundle("2012September_Bronx_r10_b03", "2012-09-03T01:00:00EDT");
  }
}
