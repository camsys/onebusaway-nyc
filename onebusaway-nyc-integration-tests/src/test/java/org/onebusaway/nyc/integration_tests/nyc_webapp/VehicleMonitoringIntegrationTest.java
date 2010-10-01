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

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.TraceSupport;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.siri.model.MonitoredVehicleJourney;
import org.onebusaway.siri.model.ServiceDelivery;
import org.onebusaway.siri.model.Siri;
import org.onebusaway.siri.model.VehicleActivity;
import com.thoughtworks.xstream.XStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.List;

public class VehicleMonitoringIntegrationTest {

  @Test
  public void testByVehicleId() throws HttpException, IOException,
      InterruptedException {

    TraceSupport traceSupport = new TraceSupport();
    traceSupport.setShiftStartTime(true);
    File trace = new File(
        "src/integration-test/resources/traces/2008_20100627CC_097800_B62_0013_B62_18-lateStart.csv.gz");

    String taskId = traceSupport.uploadTraceForSimulation(trace);

    while (true) {
      List<NycTestLocationRecord> actual = traceSupport.getSimulationResults(taskId);
      if (actual.size() > 30) {
        break;
      }
      Thread.sleep(1000);
    }

    Siri siri = getResponse("vehicle-monitoring.xml?key=TEST&OperatorRef=2008&VehicleRef=9292");

    GregorianCalendar now = new GregorianCalendar();

    ServiceDelivery serviceDelivery = siri.ServiceDelivery;
    assertTrue(serviceDelivery.ResponseTimestamp.getTimeInMillis()
        - now.getTimeInMillis() < 1000);
    List<VehicleActivity> deliveries = serviceDelivery.VehicleMonitoringDelivery.deliveries;
    /* there's only one stop requested */
    assertTrue(deliveries.size() == 1);
    VehicleActivity delivery = deliveries.get(0);

    /* no onward calls requested so none returned */
    MonitoredVehicleJourney journey = delivery.MonitoredVehicleJourney;
    assertNull(journey.OnwardCalls);

    assertEquals("B62", journey.LineRef);
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
