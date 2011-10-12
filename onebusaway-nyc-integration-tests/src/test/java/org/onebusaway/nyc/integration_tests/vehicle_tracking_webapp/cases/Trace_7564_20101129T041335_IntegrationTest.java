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

/**
 * Block = 2008_12888410
 * 
 * Interesting trace because it goes off route (deviation onto 4th Ave from 60th
 * to 50th)
 * 
 * Comment:
 * https://openplans.basecamphq.com/projects/5152119/todo_items/74384063
 * /comments
 * 
 * One issue I see is that we lose GPS from about 12:25 to 12:37 an incorrectly
 * assign to a different block when the GPS comes back. I think that incorrect
 * block assignment might lead to lingering problems later on at 3:25 pm...
 * 
 * 
 * @author bdferris
 */
public class Trace_7564_20101129T041335_IntegrationTest extends AbstractTraceRunner {

  public Trace_7564_20101129T041335_IntegrationTest() throws Exception {
    super("7564-2010-11-29T04-13-35.csv.gz");
    setBundle("b63-winter10", "2010-12-20T00:00:00EDT");
  }
}
