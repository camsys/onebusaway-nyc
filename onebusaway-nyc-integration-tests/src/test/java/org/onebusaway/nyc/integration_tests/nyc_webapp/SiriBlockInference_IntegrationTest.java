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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.onebusaway.nyc.integration_tests.RunUntilSuccess;

@SuppressWarnings("unchecked")
@RunWith(RunUntilSuccess.class)
public class SiriBlockInference_IntegrationTest extends SiriIntegrationTestBase {
	
  private boolean _isSetUp = false;
	
  public SiriBlockInference_IntegrationTest() {
    super(null);
  }

  @Before
  public void setUp() throws Throwable {
	if(_isSetUp == false) {
		setTrace("siri-test-block-level-inference.csv");
		setBundle("2012Jan_SIB63M34_r20_b01", "2012-03-02T13:57:00-04:00");

		reset("MTA NYCT_2437");
		reset("MTA NYCT_2436");    
		loadRecords();
		
		_isSetUp = true;
	}
  }
  
  @Test
  public void testBlockSetOnSM() throws HttpException, IOException {
	 HashMap<String,Object> smResponse = getSmResponse("MTA", "903036");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)smResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("StopMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 assertTrue(mvj.get("BlockRef") != null);
  }

  @Test
  public void testBlockSetOnVM() throws HttpException, IOException {
	 HashMap<String,Object> vmResponse = getVmResponse("MTA%20NYCT", "2436");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)vmResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("VehicleMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("VehicleActivity");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 assertTrue(mvj.get("BlockRef") != null);
  }

  @Test
  public void testStatusOnCurrentTrip() throws HttpException, IOException {
	 HashMap<String,Object> smResponse = getSmResponse("MTA", "903036");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)smResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("StopMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 assertEquals(mvj.get("ProgressStatus"), null);	 
  }

  @Test
  public void testStatusOnNextTrip() throws HttpException, IOException {
	 HashMap<String,Object> smResponse = getSmResponse("MTA", "404050");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)smResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("StopMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 assertEquals(mvj.get("ProgressStatus"), "prevTrip");	 
  }  
  
  // wrapping
  @Test
  public void testWrapping() throws HttpException, IOException {
	 HashMap<String,Object> smResponse = getSmResponse("MTA", "404050");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)smResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("StopMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");

	 assertEquals(mvjs.size(), 1);
  }  

  @Test
  public void testBusIsWrappedOnlyOnce() throws HttpException, IOException {
	 HashMap<String,Object> smResponse = getSmResponse("MTA", "903037");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)smResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("StopMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");

	 assertEquals(mvjs.size(), 0);
  }  

  // trip data matches trip bus would be on then
  @Test
  public void testNextTripIdOnCurrentTrip() throws HttpException, IOException {
	 HashMap<String,Object> smResponse = getSmResponse("MTA", "903036");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)smResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("StopMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 HashMap<String,Object> fvjr = (HashMap<String, Object>) mvj.get("FramedVehicleJourneyRef");

	 assertEquals(fvjr.get("DatedVehicleJourneyRef"), "MTA NYCT_20120108EE_072000_X1_0288_X0109_19");	 
  }  
  
  @Test
  public void testNextTripIdOnNextTrip() throws HttpException, IOException {
	 HashMap<String,Object> smResponse = getSmResponse("MTA", "404050");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)smResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("StopMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 HashMap<String,Object> fvjr = (HashMap<String, Object>) mvj.get("FramedVehicleJourneyRef");

	 assertEquals(fvjr.get("DatedVehicleJourneyRef"), "MTA NYCT_20120108EE_086000_X1_0291_X0109_19");	 
  }  
  
  // distances
  @Test
  @Ignore
  public void testDistancesOnCurrentTrip() throws HttpException, IOException {
	 HashMap<String,Object> smResponse = getSmResponse("MTA", "903036");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)smResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("StopMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 HashMap<String,Object> monitoredCall = (HashMap<String, Object>) mvj.get("MonitoredCall");
	 HashMap<String,Object> extensions = (HashMap<String, Object>) monitoredCall.get("Extensions");
	 HashMap<String,Object> distances = (HashMap<String, Object>) extensions.get("Distances");

	 assertEquals(distances.get("PresentableDistance"), "1 stop away");	 
	 assertEquals(distances.get("DistanceFromCall"), 286.53);	 
	 assertEquals(distances.get("StopsFromCall"), 1);	 
	 assertEquals(distances.get("CallDistanceAlongRoute"), 38189.61);	 
  } 
  
  @Test
  @Ignore
  public void testDistancesOnNextTrip() throws HttpException, IOException {
	 HashMap<String,Object> smResponse = getSmResponse("MTA", "404050");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)smResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("StopMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 HashMap<String,Object> monitoredCall = (HashMap<String, Object>) mvj.get("MonitoredCall");
	 HashMap<String,Object> extensions = (HashMap<String, Object>) monitoredCall.get("Extensions");
	 HashMap<String,Object> distances = (HashMap<String, Object>) extensions.get("Distances");

	 assertEquals(distances.get("PresentableDistance"), "3 stops away");	 
	 assertEquals(distances.get("DistanceFromCall"), 744.85);	 
	 assertEquals(distances.get("StopsFromCall"), 3);	 
	 assertEquals(distances.get("CallDistanceAlongRoute"), 391.14);	 
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
	 assertEquals(onwardCalls.size(), 2);
	 
	 HashMap<String,Object> stop1 = (HashMap<String, Object>) onwardCalls.get(0);
	 HashMap<String,Object> stop2 = (HashMap<String, Object>) onwardCalls.get(1);

	 assertEquals(stop1.get("StopPointRef"), "MTA NYCT_404992");

	 HashMap<String,Object> extensions1 = (HashMap<String, Object>) stop1.get("Extensions");
	 HashMap<String,Object> distances1 = (HashMap<String, Object>) extensions1.get("Distances");

	 assertEquals(distances1.get("PresentableDistance"), "approaching");	 
	 assertEquals(distances1.get("DistanceFromCall"), 71.49);	 
	 assertEquals(distances1.get("StopsFromCall"), 0);	 
	 assertEquals(distances1.get("CallDistanceAlongRoute"), 37974.57);	 

	 assertEquals(stop2.get("StopPointRef"), "MTA NYCT_903036");

	 HashMap<String,Object> extensions2 = (HashMap<String, Object>) stop2.get("Extensions");
	 HashMap<String,Object> distances2 = (HashMap<String, Object>) extensions2.get("Distances");

	 assertEquals(distances2.get("PresentableDistance"), "1 stop away");	 
	 assertEquals(distances2.get("DistanceFromCall"), 286.53);	 
	 assertEquals(distances2.get("StopsFromCall"), 1);	 
	 assertEquals(distances2.get("CallDistanceAlongRoute"), 38189.61);	 
  }
  
  // SM onward calls
  @Test
  @Ignore
  public void testSMOnwardCalls() throws HttpException, IOException {
	 HashMap<String,Object> vmResponse = getSmResponse("MTA", "903036");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)vmResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("StopMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 HashMap<String,Object> onwardCallWrapper = (HashMap<String, Object>) mvj.get("OnwardCalls");
	 ArrayList<Object> onwardCalls = (ArrayList<Object>) onwardCallWrapper.get("OnwardCall");
	 
	 assertEquals(onwardCalls.size(), 2);
	 
	 HashMap<String,Object> stop1 = (HashMap<String, Object>) onwardCalls.get(0);
	 HashMap<String,Object> stop2 = (HashMap<String, Object>) onwardCalls.get(1);
	 
	 assertEquals(stop1.get("StopPointRef"), "MTA NYCT_404992");

	 HashMap<String,Object> extensions1 = (HashMap<String, Object>) stop1.get("Extensions");
	 HashMap<String,Object> distances1 = (HashMap<String, Object>) extensions1.get("Distances");

	 assertEquals(distances1.get("PresentableDistance"), "approaching");	 
	 assertEquals(distances1.get("DistanceFromCall"), 71.49);	 
	 assertEquals(distances1.get("StopsFromCall"), 0);	 
	 assertEquals(distances1.get("CallDistanceAlongRoute"), 37974.57);	 
	 
	 assertEquals(stop2.get("StopPointRef"), "MTA NYCT_903036");

	 HashMap<String,Object> extensions2 = (HashMap<String, Object>) stop2.get("Extensions");
	 HashMap<String,Object> distances2 = (HashMap<String, Object>) extensions2.get("Distances");

	 assertEquals(distances2.get("PresentableDistance"), "1 stop away");	 
	 assertEquals(distances2.get("DistanceFromCall"), 286.53);	 
	 assertEquals(distances2.get("StopsFromCall"), 1);	 
	 assertEquals(distances2.get("CallDistanceAlongRoute"), 38189.61);	 
  }
  
  // SM onward calls
  @Test
  @Ignore
  public void testSMOnwardCallsNextTrip() throws HttpException, IOException {
	 HashMap<String,Object> vmResponse = getSmResponse("MTA", "404923");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)vmResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("StopMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");
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

	 assertEquals(distances1.get("PresentableDistance"), "< 1 stop away");	 
	 assertEquals(distances1.get("DistanceFromCall"), 353.77);	 
	 assertEquals(distances1.get("StopsFromCall"), 0);	 
	 assertEquals(distances1.get("CallDistanceAlongRoute"), 0.06);	 
	 
	 assertEquals(stop2.get("StopPointRef"), "MTA NYCT_905046");

	 HashMap<String,Object> extensions2 = (HashMap<String, Object>) stop2.get("Extensions");
	 HashMap<String,Object> distances2 = (HashMap<String, Object>) extensions2.get("Distances");

	 assertEquals(distances2.get("PresentableDistance"), "25.0 miles away");	 
	 assertEquals(distances2.get("DistanceFromCall"), 40292.93);	 
	 assertEquals(distances2.get("StopsFromCall"), 56);	 
	 assertEquals(distances2.get("CallDistanceAlongRoute"), 39939.21);	 
  }
}
