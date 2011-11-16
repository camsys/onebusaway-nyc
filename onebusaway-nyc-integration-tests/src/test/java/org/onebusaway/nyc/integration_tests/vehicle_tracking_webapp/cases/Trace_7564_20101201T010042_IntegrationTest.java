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

import org.onebusaway.realtime.api.EVehiclePhase;

/**
 * block = 2008_12888481
 * 
 * In this case, we correctly track the vehicle to the end of block # 12888481,
 * terminating at Bay Ridge. The vehicle dead heads back to the base along 4th
 * Avenue, but pauses for ~ 10 minutes at 40.63477,-74.02344 (4th Ave & Bay
 * Ridge Ave approximately) along the way. That pause is long enough to be
 * inferred as LAYOVER_AFTER, indicating a layover after the completion of a
 * block. At that point, we attempt to start a new block for the vehicle, which
 * is why the vehicle is marked as on-route even though it heads back to the
 * base. It's worth noting that the driver never changes his DSC from 4630 after
 * completing the block and dead-heading back.
 * 
 * @author bdferris
 */
public class Trace_7564_20101201T010042_IntegrationTest extends AbstractTraceRunner {

  public Trace_7564_20101201T010042_IntegrationTest() throws Exception {
    super("7564-2010-12-01T01-00-42.csv.gz");
    setBundle("b63-winter10", "2010-12-20T00:00:00EDT");
    
    /*
     * FIXME this has a potential bundle-conflict with the trace 
     */
    setMinAccuracyRatioForPhase(EVehiclePhase.IN_PROGRESS, 0.94);
  }
}
