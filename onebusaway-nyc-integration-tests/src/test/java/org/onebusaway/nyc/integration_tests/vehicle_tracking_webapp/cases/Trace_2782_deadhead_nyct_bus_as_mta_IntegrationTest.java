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

public class Trace_2782_deadhead_nyct_bus_as_mta_IntegrationTest extends AbstractTraceRunner {
  public Trace_2782_deadhead_nyct_bus_as_mta_IntegrationTest() throws Exception {
    super("2782_deadhead_nyct_bus_as_mta.csv");
    setBundle("2014Jan_AllCity_r09_b3", "2014-01-29T09:50:00EDT");
  }
}
