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
package org.onebusaway.nyc.integration_tests.nyc_webapp;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class SiriBlockLayoverInference_IntegrationTest extends SiriIntegrationTestBase {
	
  private boolean _isSetUp = false;
	
  public SiriBlockLayoverInference_IntegrationTest() {
    super(null);
  }

  @Before
  public void setUp() throws Throwable {
	if(_isSetUp == false) {
		setTrace("siri-test-block-level-inference-layover.csv");
		setBundle("2012Jan_SIB63M34_r20_b01", "2012-03-02T13:57:00-04:00");

		resetAll();
		loadRecords();
		
		_isSetUp = true;
	}
  }
  
  @Test
  public void testDepartureTimeSetOnSM() throws HttpException, IOException {
	 HashMap<String,Object> smResponse = getSmResponse("MTA", "404923");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)smResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("StopMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 assertEquals(mvj.get("OriginAimedDepartureTime"), "2012-03-01T14:20:00.000-05:00");
  }
  
  @Test
  public void testDepartureTimeSetOnVM() throws HttpException, IOException, InterruptedException {
   Thread.sleep(60 * 1000); // break cache
	 HashMap<String,Object> vmResponse = getVmResponse("MTA%20NYCT", "2436");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)vmResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("VehicleMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("VehicleActivity");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 assertEquals(mvj.get("OriginAimedDepartureTime"), "2012-03-01T14:20:00.000-05:00");
  }
  
  // VM onward calls
  @Test
  @Ignore
  public void testVMOnwardCalls() throws HttpException, IOException {
	 HashMap<String,Object> vmResponse = getVmResponse("MTA%20NYCT", "2436");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)vmResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("VehicleMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("VehicleActivity");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 HashMap<String,Object> onwardCallWrapper = (HashMap<String, Object>) mvj.get("OnwardCalls");
	 ArrayList<Object> onwardCalls = (ArrayList<Object>) onwardCallWrapper.get("OnwardCall");
	 
	 assertEquals(onwardCalls.size(), 57);
	 
	 HashMap<String,Object> stop1 = (HashMap<String, Object>) onwardCalls.get(0);
	 HashMap<String,Object> stop2 = (HashMap<String, Object>) onwardCalls.get(onwardCalls.size() - 1);
	 
	 assertEquals(stop1.get("StopPointRef"), "MTA NYCT_404923");
	 
	 HashMap<String,Object> extensions1 = (HashMap<String, Object>) stop1.get("Extensions");
	 HashMap<String,Object> distances1 = (HashMap<String, Object>) extensions1.get("Distances");

	 assertEquals(distances1.get("PresentableDistance"), "approaching");	 
	 assertEquals(distances1.get("DistanceFromCall"), 53.82);	 
	 assertEquals(distances1.get("StopsFromCall"), 0);	 
	 assertEquals(distances1.get("CallDistanceAlongRoute"), 0.06);	 
	 
	 assertEquals(stop2.get("StopPointRef"), "MTA NYCT_905046");

	 HashMap<String,Object> extensions2 = (HashMap<String, Object>) stop2.get("Extensions");
	 HashMap<String,Object> distances2 = (HashMap<String, Object>) extensions2.get("Distances");

	 assertEquals(distances2.get("PresentableDistance"), "24.9 miles away");	 
	 assertEquals(distances2.get("DistanceFromCall"), 39992.98);	 
	 assertEquals(distances2.get("StopsFromCall"), 56);	 
	 assertEquals(distances2.get("CallDistanceAlongRoute"), 39939.21);	 
  }
}
