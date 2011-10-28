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

import static org.junit.Assert.fail;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.onebusaway.realtime.api.EVehiclePhase;


public class Trace_summaryTest extends AbstractTraceRunner {

  public Trace_summaryTest() throws Throwable {
    
    List<Map<EVehiclePhase, Double>> testPhaseResults = new ArrayList<Map<EVehiclePhase, Double>>();
    setTrace("7564-2010-12-06T00-55-51.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7564-2010-12-03T00-46-33.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7564-2010-12-02T11-49-09.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7564-2010-12-02T03-40-55.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7564-2010-12-01T01-00-42.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7564-2010-12-29T04-13-35.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7563-2010-12-09T12-47-32.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7561-2010-12-09T12-47-55.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7560-2010-11-29T23-30-21.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7560-2010-11-29T01-17-44.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7560-2010-11-27T23-28-47.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7560-2010-11-27T00-31-53.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7560-2010-11-23T23-45-15.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7560-2010-11-23T03-17-34.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7560-2010-11-22T22-10-07.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("7560-2010-11-22T08-42-26.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("5318-2010-12-02T17-21-38.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("3649-2010-11-25T12-18-01.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("1404-2010-12-10T03-42-49.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("1379-2010-12-11T01-00-25.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("1325-2010-12-15T01-48-45.csv.gz");
    testPhaseResults.add(runTest());
    setTrace("0927-2010-12-09T12-47-42.csv.gz");
    testPhaseResults.add(runTest());
    setBundle("b63-winter10", "2010-12-20T00:00:00EDT");
    
    // TODO finish t-test...
    fail("not implemented");
  }
}
