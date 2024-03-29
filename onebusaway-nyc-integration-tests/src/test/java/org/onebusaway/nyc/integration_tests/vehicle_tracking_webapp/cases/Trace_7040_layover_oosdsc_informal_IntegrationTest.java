/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

/**
 * Trace of a layover with an out-of-service DSC, without formal inference.  This should be
 * inferred as DEADHEAD, as we don't have confidence that the bus hasn't changed trips on us.
 * It should pick up IN_PROGRESS once the DSC is changed.
 *
 */
public class Trace_7040_layover_oosdsc_informal_IntegrationTest extends AbstractTraceRunner  {

  public Trace_7040_layover_oosdsc_informal_IntegrationTest() throws Exception {
  super("7040-layover-oosdsc-informal-labeled.csv");
  setBundle("2013June_Prod_r03_b02", "2013-07-15T19:40:00EDT");
  }
}
