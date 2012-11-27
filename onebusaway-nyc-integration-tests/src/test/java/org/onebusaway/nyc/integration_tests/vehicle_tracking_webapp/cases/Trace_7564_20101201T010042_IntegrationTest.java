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
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

/**
 * Layover before during the deadhead at 4th ave and bay ridge is wrong, since there
 * are no layovers at any terminals for any blocks with the given DSCs anywhere 
 * near the location.
 * 
 */
@Ignore
public class Trace_7564_20101201T010042_IntegrationTest extends AbstractTraceRunner {

  public Trace_7564_20101201T010042_IntegrationTest() throws Exception {
    super("7564-2010-12-01T01-00-42.csv");
    setBundle("b63-winter10", "2010-12-20T00:00:00EDT");
  }
}
