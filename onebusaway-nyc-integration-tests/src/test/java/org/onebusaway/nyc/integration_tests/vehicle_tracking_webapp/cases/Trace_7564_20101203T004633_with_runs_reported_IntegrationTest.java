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
 * Block = 2008_12888406
 * 
 * Driver left the depot around 4:50 AM, headed for Bay Ridge to start block #
 * 12888406 at 5:05 AM. Leaving depot, the DSC is 12, but the driver never
 * changes the DSC at the start of the trip. OBA tracks the entire trip as
 * "DEADHEAD_BEFORE" because we haven't seen a DSC to indicate which trip we
 * might be on yet. When the driver reaches Cobble Hill, he switches on the
 * proper DSC at 6:25 (6431), and we attempt to assign a block. However, we
 * think he's at the start of a block (as opposed to one trip into an existing
 * block), so we attempt to find a block that starts at Cobble Hill (as opposed
 * to using the already in-progress block). The filter fails to find a block
 * with enough likelihood, so it keeps it DEADHEAD_BEFORE.
 * 
 * NOTE: this one has operator-entered runs.  they don't always match
 * the made-up "actual" run.
 * 
 * @author bdferris, bwillard
 */
public class Trace_7564_20101203T004633_with_runs_reported_IntegrationTest extends AbstractTraceRunner {

  public Trace_7564_20101203T004633_with_runs_reported_IntegrationTest() throws Exception {
    super("7564-2010-12-03T00-46-33.with.runs.csv.gz");
    setBundle("b63-winter10", "2010-12-20T00:00:00EDT");
    setLoops(1);
//    setMinAccuracyRatioForPhase(EVehiclePhase.LAYOVER_DURING, 0.75);
    setMinAccuracyRatioForPhase(EVehiclePhase.DEADHEAD_BEFORE, 0.93);
  }
}
