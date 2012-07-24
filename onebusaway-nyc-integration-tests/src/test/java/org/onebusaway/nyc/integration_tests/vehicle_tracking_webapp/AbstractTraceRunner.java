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
package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.utility.DateLibrary;
import org.opentripplanner.routing.impl.DistanceLibrary;

import com.vividsolutions.jts.geom.Coordinate;

public class AbstractTraceRunner {

  private static TraceSupport _traceSupport = new TraceSupport();

  private String _factorySeed = "298763210";

  private String _cdfSeed = "184970829";

  private String _trace;

  /**
   * The max amount of time we should wait for a single record to process
   */
  private long _maxTimeout = 40 * 1000;

  public AbstractTraceRunner() {
  }

  public AbstractTraceRunner(String trace) {
    _trace = trace;
  }

  public void setTrace(String trace) {
    _trace = trace;
  }

  public void setMaxTimeout(long maxTimeout) {
    _maxTimeout = maxTimeout;
  }

  public void setBundle(String bundleId, String date) throws Exception {
    setBundle(bundleId, DateLibrary.getIso8601StringAsTime(date));
  }

  public void setBundle(String bundleId, Date date) throws Exception {
    String port = System.getProperty(
        "org.onebusaway.transit_data_federation_webapp.port", "9005");
    String url = "http://localhost:" + port
        + "/onebusaway-nyc-vehicle-tracking-webapp/change-bundle.do?bundleId="
        + bundleId + "&time=" + DateLibrary.getTimeAsIso8601String(date);

    HttpClient client = new HttpClient();
    GetMethod get = new GetMethod(url);
    client.executeMethod(get);

    String response = get.getResponseBodyAsString();
    if (!response.equals("OK"))
      throw new Exception("Bundle switch failed!");
  }

  @Before
  public void setup() throws Exception {
    setSeeds();
  }

  public void setSeeds() throws Exception {
    String port = System.getProperty(
        "org.onebusaway.transit_data_federation_webapp.port", "9005");

    String urlStr = "http://localhost:"
        + port
        + "/onebusaway-nyc-vehicle-tracking-webapp/vehicle-location-simulation!set-seeds.do?factorySeed="
        + _factorySeed + "&cdfSeed=" + _cdfSeed;

    HttpClient client = new HttpClient();
    GetMethod get = new GetMethod(urlStr);
    client.executeMethod(get);

    String response = get.getResponseBodyAsString();
    if (!response.equals("OK"))
      throw new Exception("Failed trying to execute:" + urlStr);

    System.out.println("Successfully set seeds: phase=" + _factorySeed + ", cdf="
        + _cdfSeed);
  }
  
  /**
   * 
   * @return map of phases to average acceptance ratios
   * @throws Throwable
   */
  @Test
  public void test() throws Throwable {

	// expected results
	File trace = new File("src/integration-test/resources/traces/" + _trace);
	List<NycTestInferredLocationRecord> expectedResults = _traceSupport.readRecords(trace);

	// run to get inferred results
    String taskId = _traceSupport.uploadTraceForSimulation(trace, false);

    long t = System.currentTimeMillis();
    int prevRecordCount = -1;
    while (true) {
      List<NycTestInferredLocationRecord> ourResults = _traceSupport.getSimulationResults(taskId);
      System.out.println("records=" + ourResults.size() + "/" + expectedResults.size());

      // wait for all records to come in
      if (ourResults.size() < expectedResults.size()) {

    	  if (t + _maxTimeout < System.currentTimeMillis()) {
    		  fail("waited but never received enough records: expected="
    				  + expectedResults.size() + " actual=" + expectedResults.size());
    	  }

    	  // We reset our timeout if the record count is growing
    	  if (ourResults.size() > prevRecordCount) {
    		  t = System.currentTimeMillis();
    		  prevRecordCount = ourResults.size();
    	  }

    	  Thread.sleep(1000);
    	  continue;
      }

      // all results are in!

      // TEST: We got a response for all records.
      assertEquals(ourResults.size(), expectedResults.size());
      
      // TEST: Our results match expected results
      for(int i = 0; i < ourResults.size(); i++) {
    	NycTestInferredLocationRecord ourResult = ourResults.get(i);
    	NycTestInferredLocationRecord expectedResult = expectedResults.get(i);
    	  
    	// mark new row
    	System.out.println("\n\nRecord " + (i + 1) + ", line " + (i + 2) + "\n================================");

    	// TEST: block level inference subcases
    	Boolean actualIsRunFormal = expectedResult.getActualIsRunFormal();
    	assertTrue(actualIsRunFormal != null);
    	
    	if(actualIsRunFormal == true) {
    		// run ID should match
    		if(expectedResult.getActualRunId() != null && !StringUtils.isEmpty(expectedResult.getActualRunId())) {
    	    	System.out.println("RUN ID: expected=" + expectedResult.getActualRunId() + ", inferred=" + ourResult.getInferredRunId());
    			assertEquals(ourResult.getInferredRunId(), expectedResult.getActualRunId());
    		}

    		// trip ID should match
    		if(expectedResult.getActualTripId() != null && !StringUtils.isEmpty(expectedResult.getActualTripId())) {
    	    	System.out.println("TRIP ID: expected=" + expectedResult.getActualTripId() + ", inferred=" + ourResult.getInferredTripId());
    			assertTrue(ourResult.getInferredTripId().endsWith(expectedResult.getActualTripId()));
    		}

    		// block ID should match
    		if(expectedResult.getActualBlockId() != null && !StringUtils.isEmpty(expectedResult.getActualBlockId())) {
    	    	System.out.println("BLOCK ID: expected=" + expectedResult.getActualBlockId() + ", inferred=" + ourResult.getInferredBlockId());
    			assertTrue(ourResult.getInferredBlockId().endsWith(expectedResult.getActualBlockId()));
    		}
    	}
    	  
    	// TEST: DSCs should always match if we're in_progress
    	String expectedDsc = expectedResult.getActualDsc();
    	
    	System.out.println("DSC: expected=" + expectedDsc + ", inferred=" + ourResult.getInferredDsc());
    	if(expectedDsc != null && !StringUtils.isEmpty(expectedDsc))
			assertEquals(ourResult.getInferredDsc(), expectedDsc);

    	// TEST: phase matches
    	assertTrue(ourResult.getInferredPhase() != null);
    	
    	System.out.println("PHASE: expected=" + expectedResult.getActualPhase() + ", inferred=" + ourResult.getInferredPhase());
    	if(expectedResult.getActualPhase() != null && !StringUtils.isEmpty(expectedResult.getActualPhase())) {
			String[] _acceptablePhases = StringUtils.split(expectedResult.getActualPhase().toUpperCase(), "+");
			ArrayList<String> acceptablePhases = new ArrayList<String>();
			Collections.addAll(acceptablePhases, _acceptablePhases);

			// allow partial matches for "wildcards"
			boolean pass = false;
			for(String acceptablePhase : acceptablePhases) {
				if(ourResult.getInferredPhase().toUpperCase().startsWith(acceptablePhase)) {
					pass = true;
					break;
				}
			}
			assertTrue(pass == true);
		}
		
		// TEST: status matches
    	assertTrue(ourResult.getInferredStatus() != null);

    	System.out.println("STATUS: expected=" + expectedResult.getActualStatus() + ", inferred=" + ourResult.getInferredStatus());
    	if(expectedResult.getActualStatus() != null && !StringUtils.isEmpty(expectedResult.getActualStatus())) {
			String[] _acceptableStatuses = StringUtils.split(expectedResult.getActualStatus().toUpperCase(), "+");
			ArrayList<String> acceptableStatuses = new ArrayList<String>();
			Collections.addAll(acceptableStatuses, _acceptableStatuses);
    		assertTrue(acceptableStatuses.contains(ourResult.getInferredStatus().toUpperCase()));
		}

		// TEST: distance along block/inferred location/schedule deviation
    	assertTrue(ourResult.getInferredBlockLat() != Double.NaN);
    	assertTrue(ourResult.getInferredBlockLon() != Double.NaN);
    	
		Coordinate reportedLocation = new Coordinate(expectedResult.getLat(), expectedResult.getLon());
		Coordinate ourLocation = new Coordinate(ourResult.getInferredBlockLat(), ourResult.getInferredBlockLon());

		String phase = expectedResult.getActualPhase();
		if(EVehiclePhase.IN_PROGRESS.equals(phase) 
				|| EVehiclePhase.LAYOVER_BEFORE.equals(phase)
				|| EVehiclePhase.LAYOVER_DURING.equals(phase)) {
	    	System.out.println("LOCATION: expected=" + reportedLocation + ", inferred=" + ourLocation);
			assertTrue(DistanceLibrary.distance(reportedLocation, ourLocation) <= 500 * 2);
		}
      }
      
      // break out of wait-for-completion loop
      break;
    }
  }

}
