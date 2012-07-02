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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.runner.RunWith;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.opentripplanner.routing.impl.DistanceLibrary;

@RunWith(RunUntilFailure.class)
public class Trace_2792_Layover extends AbstractTraceRunner {

  public Trace_2792_Layover() throws Exception {
    super("Trace_2792_Layover.csv");
    setBundle("2012Apr_B61_r03_b03", "2012-06-29T00:00:00EDT");
  }

  protected Map<EVehiclePhase, Double> validateRecords(
      List<NycTestInferredLocationRecord> expected,
      List<NycTestInferredLocationRecord> actual) {

	  
	  
	  for(int i = 0; i < actual.size(); i++) {
		  NycTestInferredLocationRecord current = actual.get(i);
		  
		  double dMeters = DistanceLibrary.distance(current.getInferredBlockLat(), current.getInferredBlockLon(), current.getLat(), current.getLon());
		  
//		  System.out.println(current.getInferredBlockLat() + "," + current.getInferredBlockLon() + " ? " + current.getLat() + "," + current.getLon());
		  System.out.println(dMeters);
		  
		  if(dMeters > 1000) {
			  fail("distance between inferred and actual is big!");
		  }
	  }	  
	  
	  // we don't use this functionality
	  return new HashMap<EVehiclePhase, Double>();
  }
}
