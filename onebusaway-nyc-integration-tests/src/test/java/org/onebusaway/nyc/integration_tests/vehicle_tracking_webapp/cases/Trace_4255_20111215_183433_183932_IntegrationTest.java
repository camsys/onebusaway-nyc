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
 * This trace would produce layover-during, and it shouldn't.
 * Update 02/14/2012: this should now produce all in-progress
 * results except for the first observation (can't assign in-progress
 * to first observation now).
 * 
 * @author bwillard
 * 
 */
public class Trace_4255_20111215_183433_183932_IntegrationTest extends AbstractTraceRunner {

  public Trace_4255_20111215_183433_183932_IntegrationTest() throws Exception {
    super("4255_20111215_183433_183932.csv");
    setBundle("si", "2011-12-14T00:00:00EDT");
  }
}
