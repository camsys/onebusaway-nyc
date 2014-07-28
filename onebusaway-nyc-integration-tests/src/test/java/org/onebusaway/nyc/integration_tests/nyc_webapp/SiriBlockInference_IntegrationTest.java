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
import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unchecked")
public class SiriBlockInference_IntegrationTest extends SiriIntegrationTestBase {
	
  protected static Logger _log = LoggerFactory.getLogger(SiriBlockInference_IntegrationTest.class);
  private boolean _isSetUp = false;
	
  public SiriBlockInference_IntegrationTest() {
    super(null);
  }

  @Before
  public void setUp() throws Throwable {
	if(_isSetUp == false) {
	  BasicConfigurator.configure();
		setTrace("siri-test-block-level-inference.csv");
		setBundle("2012Jan_SIB63M34_r20_b01", "2012-03-02T13:57:00-04:00");

		resetAll();
		loadRecords();
		
		_isSetUp = true;
	}
  }
  
  private void debugSMError(String msg, HashMap<String,Object> smResponse, String arg1, String arg2) throws IOException {
      _log.error(msg + " for args " + arg1 + "_" + arg2);
      _log.error("response=" + smResponse);
      smResponse = getSmResponse(arg1, arg2, true);
      _log.error("second try=" + smResponse);
      assertTrue(false);
  }
  
  @Test
  public void testBlockSetOnSM() throws HttpException, IOException {
    String arg1 = "MTA";
    String arg2 = "903036";
	 HashMap<String,Object> smResponse = getSmResponse(arg1, arg2);
	 if (smResponse == null || smResponse.size() == 0)  debugSMError("empty stop monitoring response", smResponse, arg1, arg2);
	 HashMap<String,Object> siri = (HashMap<String, Object>)smResponse.get("Siri");
	 if (siri == null || siri.size() == 0) debugSMError("empty Siri", smResponse, arg1, arg2);
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 if (serviceDelivery == null || serviceDelivery.size() == 0) debugSMError("empty ServiceDelivery", smResponse, arg1, arg2);
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("StopMonitoringDelivery");
	 if (stopMonitoringDelivery == null || stopMonitoringDelivery.size() == 0) debugSMError("empty stopMonitoringDelivery", smResponse, arg1, arg2);
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 if (monitoredStopVisit == null || monitoredStopVisit.size() == 0) debugSMError("empty monitoredStopVisit", smResponse, arg1, arg2);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");
	 if (mvjs == null || mvjs.size() == 0) debugSMError("empty mvjs", smResponse, arg1, arg2);
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 if (mvjWrapper == null || mvjWrapper.size() == 0) debugSMError("empty mvjWrapper", smResponse, arg1, arg2);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");
	 if (mvj == null || mvj.size() == 0) debugSMError("empty journey", smResponse, arg1, arg2);
	 
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

  
  /**
   * Open ended VM response is officially supported, though its discouraged.  Test
   * the caching of it.
   */
  @Test
  public void testOpenVM() throws HttpException, IOException {
    long start = System.currentTimeMillis();
    HashMap<String, Object> vmResponse1 = getVmResponse(null, null);
    long end = System.currentTimeMillis();
    long vmTime1 = (end - start);
    // assuming we are the first call to the cache
    _log.info("call took " + vmTime1);
    assert(vmTime1 > 50);  // we are only loading/returning the csv data set
    HashMap<String, Object> siri = (HashMap<String, Object>) vmResponse1.get("Siri");
    HashMap<String, Object> serviceDelivery = (HashMap<String, Object>) siri.get("ServiceDelivery");
    ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>) serviceDelivery.get("VehicleMonitoringDelivery");
    HashMap<String, Object> monitoredStopVisit = (HashMap<String, Object>) stopMonitoringDelivery.get(0);
    ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("VehicleActivity");
    HashMap<String, Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
    HashMap<String, Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

    assertTrue(mvj.get("BlockRef") != null);
    
    // now make the same call expecting it to be cached
    start = System.currentTimeMillis();
    HashMap<String, Object> vmResponse2 = getVmResponse(null, null);
    end = System.currentTimeMillis();
    long vmTime2 = (end - start);
    _log.info("call 2 took " + vmTime2);
    assert(vmTime2 < vmTime1);

    final int MAX_VM_TIME = 150; // slow build server!
    // caching will still be faster than the small data set
    assertTrue("vmTime2=" + vmTime2 + " not less than " + MAX_VM_TIME, vmTime2 < MAX_VM_TIME);
    
    // compare the responses -- they should be identical
    assert(vmResponse1.equals(vmResponse2));
    
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
  
  // distances: these are ignored for now since there is some jitter in terms of where the simulator
  // snaps the observed bus to the route line, resulting in some variance of the distances we need to find
  // a way to handle in this test environment.
  
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
