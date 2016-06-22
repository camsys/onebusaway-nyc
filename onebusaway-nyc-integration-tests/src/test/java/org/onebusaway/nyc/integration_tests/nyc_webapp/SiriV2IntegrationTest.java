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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class SiriV2IntegrationTest extends SiriIntegrationTestBase {
	
  static final Logger _log = Logger.getRootLogger();
  	
  private boolean _isSetUp = false;
	
  public SiriV2IntegrationTest() {
    super(null);
  }

  @Before
  public void setUp() throws Throwable {
	if(_isSetUp == false) {
		setTrace("siri-test-trip-level-inference-layover.csv");
		setBundle("2012Jan_SIB63M34_r20_b01", "2012-03-02T13:57:00-04:00");

		resetAll();
		loadRecords();
		
		_isSetUp = true;
	}
  }

  // (bus wrapped around to stop on next trip since bus is in layover!)
  @Test
  public void testDepartureTimeNotSetOnSMV2() throws HttpException, IOException {
	 HashMap<String,Object> smResponse = getSmV2Response("MTA",  "404923", "normal");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)smResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("StopMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("MonitoredStopVisit");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 assertEquals(mvj.get("OriginAimedDepartureTime"), null);
  }
  
  @Test
  public void testDepartureTimeNotSetOnVMV2() throws HttpException, IOException {
	 HashMap<String,Object> vmResponse = getVmV2Response("MTA%20NYCT", "2437", "basic");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)vmResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("VehicleMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("VehicleActivity");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 assertEquals(mvj.get("OriginAimedDepartureTime"), null);
  }  
  
  @Test
  public void testMinimumVMv2() throws HttpException, IOException {
	 HashMap<String,Object> vmResponse = getVmV2Response("MTA%20NYCT", "2437", "minimum");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)vmResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("VehicleMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("VehicleActivity");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 assertNull("OperatorRef populated", mvj.get("OperatorRef"));
  }
  
  @Test
  public void testBasicVMv2() throws HttpException, IOException {
	 HashMap<String,Object> vmResponse = getVmV2Response("MTA%20NYCT", "2437", "basic");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)vmResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("VehicleMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("VehicleActivity");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");

	 assertNull("OperatorRef populated",mvj.get("OperatorRef"));
	 assertNotNull("DestinationRef populated",mvj.get("DestinationRef"));
  }  
 
  @Test
  public void testCallsVMv2() throws HttpException, IOException {
	 HashMap<String,Object> vmResponse = getVmV2Response("MTA%20NYCT", "2437", "calls");
	  
	 HashMap<String,Object> siri = (HashMap<String, Object>)vmResponse.get("Siri");
	 HashMap<String,Object> serviceDelivery = (HashMap<String, Object>)siri.get("ServiceDelivery");
	 ArrayList<Object> stopMonitoringDelivery = (ArrayList<Object>)serviceDelivery.get("VehicleMonitoringDelivery");
	 HashMap<String,Object> monitoredStopVisit = (HashMap<String,Object>)stopMonitoringDelivery.get(0);
	 ArrayList<Object> mvjs = (ArrayList<Object>) monitoredStopVisit.get("VehicleActivity");
	 HashMap<String,Object> mvjWrapper = (HashMap<String, Object>) mvjs.get(0);
	 HashMap<String,Object> mvj = (HashMap<String, Object>) mvjWrapper.get("MonitoredVehicleJourney");
	 HashMap<String,Object> onwardCalls = (HashMap<String, Object>) mvj.get("OnwardCalls");

	 assertNotNull("OperatorRef not populated",mvj.get("OperatorRef"));
	 assertNotNull("DestinationRef not populated", mvj.get("DestinationRef"));
	 assertNotNull("JourneyPatternRef not populated", mvj.get("JourneyPatternRef"));
	 assertNotNull("OnwardCalls not populated", onwardCalls);
  } 
}