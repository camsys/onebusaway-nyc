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
 * This trace has a "wrong" reported run, no DSC, a sizable deadhead between trips,
 * and a bad/detoured start (etc).  This confirms that the lev distance of run
 * ids is 0 by failing to match to a run-id of 999-485 that is one character off.
 * 
 * This trace is a copy of Trace_6152_20111214_123000_143500_no_dsc to guard against 
 * false positives caused by a lev distance greater than 0.  It is not that interesting,
 * in that what used to be a partially inferred run is now completely DEADHEAD.
 */


public class Trace_6152_20111214_123000_143500_no_dsc_levdist_IntegrationTest extends AbstractTraceRunner {

  public Trace_6152_20111214_123000_143500_no_dsc_levdist_IntegrationTest() throws Exception {
    super("6154_20111214_123000_143500_no-dsc_levdist.csv");
    setBundle("si", "2011-12-07T00:00:00EDT");
  }
}
