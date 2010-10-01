/*
 * Copyright 2010, OpenPlans Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import static org.junit.Assert.*;

import org.onebusaway.siri.model.MonitoredVehicleJourney;
import org.onebusaway.siri.model.ServiceDelivery;
import org.onebusaway.siri.model.Siri;
import org.onebusaway.siri.model.StopMonitoringDelivery;

import com.thoughtworks.xstream.XStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.List;

public class StopMonitoringIntegrationTest {

  @Test
  public void test() throws HttpException, IOException {

    /*
     * todo: we need to insert some data so that this actually works once we
     * switch the code over to ignoring schedules
     */

    Siri siri = getResponse("stop-monitoring.xml?key=TEST&OperatorRef=2008&MonitoringRef=305344");

    GregorianCalendar now = new GregorianCalendar();

    ServiceDelivery serviceDelivery = siri.ServiceDelivery;
    assertTrue(serviceDelivery.ResponseTimestamp.getTimeInMillis()
        - now.getTimeInMillis() < 1000);
    List<StopMonitoringDelivery> deliveries = serviceDelivery.stopMonitoringDeliveries;
    /* there's only one stop requested */
    assertTrue(deliveries.size() == 1);
    StopMonitoringDelivery delivery = deliveries.get(0);

    /* no onward calls requested so none returned */
    MonitoredVehicleJourney journey = delivery.visits.get(0).MonitoredVehicleJourney;
    assertNull(journey.OnwardCalls);

    assertTrue(journey.MonitoredCall.Extensions.Distances.DistanceFromCall > 0);
    assertTrue(Math.abs(journey.MonitoredCall.Extensions.Distances.CallDistanceAlongRoute - 2017) < 1);

    assertEquals("B63", journey.LineRef);
    assertEquals("306619", journey.OriginRef);
  }

  @Test
  public void testOnwardCalls() throws HttpException, IOException {

    Siri siri = getResponse("stop-monitoring.xml?key=TEST&OperatorRef=2008&MonitoringRef=305344&StopMonitoringDetailLevel=calls");

    ServiceDelivery serviceDelivery = siri.ServiceDelivery;
    List<StopMonitoringDelivery> deliveries = serviceDelivery.stopMonitoringDeliveries;
    StopMonitoringDelivery delivery = deliveries.get(0);

    /* onward calls requested */
    MonitoredVehicleJourney journey = delivery.visits.get(0).MonitoredVehicleJourney;
    assertNotNull(journey.OnwardCalls);
    assertTrue(journey.OnwardCalls.size() >= 2);
    Double distanceFromRequestedStop = journey.MonitoredCall.Extensions.Distances.DistanceFromCall;
    assertTrue(journey.OnwardCalls.get(0).Extensions.Distances.DistanceFromCall > distanceFromRequestedStop);

  }

  private Siri getResponse(String query) throws IOException, HttpException {
    HttpClient client = new HttpClient();
    String url = "http://localhost:9000/onebusaway-api-webapp/siri/" + query;
    GetMethod get = new GetMethod(url);
    client.executeMethod(get);

    String response = get.getResponseBodyAsString();
    assertTrue(response.startsWith("<Siri"));

    XStream xstream = new XStream();
    xstream.processAnnotations(Siri.class);
    Siri siri = (Siri) xstream.fromXML(response);
    assertNotNull(siri);
    return siri;
  }
}
