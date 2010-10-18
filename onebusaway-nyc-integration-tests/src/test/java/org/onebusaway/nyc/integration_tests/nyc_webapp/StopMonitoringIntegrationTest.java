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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.siri.model.DistanceExtensions;
import org.onebusaway.siri.model.MonitoredCall;
import org.onebusaway.siri.model.MonitoredStopVisit;
import org.onebusaway.siri.model.MonitoredVehicleJourney;
import org.onebusaway.siri.model.ServiceDelivery;
import org.onebusaway.siri.model.Siri;
import org.onebusaway.siri.model.StopMonitoringDelivery;
import org.onebusaway.utility.DateLibrary;

import com.caucho.hessian.client.HessianProxyFactory;
import com.thoughtworks.xstream.XStream;

public class StopMonitoringIntegrationTest {

  /**
   * July 7, 2010 - 11:00 am in NYC
   */
  private String _timeString = "2010-07-07T11:30:00-04:00";

  private Date _time;

  private VehicleLocationListener _vehicleLocationListener;
  
  private AgencyAndId _vehicleId = new AgencyAndId("2008","4444");

  @Before
  public void before() throws ParseException, MalformedURLException {
    
    _time = DateLibrary.getIso8601StringAsTime(_timeString);
    
    String federationPort = System.getProperty("org.onebusaway.transit_data_federation_webapp.port","9905");
    HessianProxyFactory factory = new HessianProxyFactory();
    
    // Connect the VehicleLocationListener for directly injecting location records
    _vehicleLocationListener = (VehicleLocationListener) factory.create(VehicleLocationListener.class, "http://localhost:" + federationPort + "/onebusaway-nyc-vehicle-tracking-webapp/remoting/vehicle-location-listener");
    
    // Reset any location records between test runs
    _vehicleLocationListener.resetVehicleLocation(_vehicleId);
  }

  @Test
  public void testEmptyResults() throws HttpException, IOException {

    /**
     * We call this without first injecting real-time data first
     */
    Siri siri = getResponse("stop-monitoring.xml?key=TEST&MonitoringRef=305175&OperatorRef=2008&time="
        + _timeString);

    ServiceDelivery serviceDelivery = siri.ServiceDelivery;
    assertEquals(_time.getTime(),
        serviceDelivery.ResponseTimestamp.getTimeInMillis());

    List<StopMonitoringDelivery> deliveries = serviceDelivery.stopMonitoringDeliveries;
    /* there's only one stop requested */
    assertTrue(deliveries.size() == 1);
    StopMonitoringDelivery delivery = deliveries.get(0);

    assertEquals(_time.getTime(),delivery.ResponseTimestamp.getTimeInMillis());
    
    // We shouldn't actually have any results
    assertNull(delivery.visits);
  }

  @Test
  public void test() throws HttpException, IOException, ParseException {

    /**
     * Let's inject a location record
     */
    
    //2179,40.7372545433,-73.9557642325,2010-07-07 11:29:38,4430,2008_12023519,1278475200000,43753.36285532166,4430,40.7372545433,-73.9557642325,IN_PROGRESS
    
    VehicleLocationRecord record = new VehicleLocationRecord();
    record.setBlockId(new AgencyAndId("2008","12023519"));
    record.setCurrentLocationLat(40.7372545433);
    record.setCurrentLocationLon(-73.9557642325);
    record.setDistanceAlongBlock(43753.36285532166);
    record.setServiceDate(1278475200000L);
    record.setTimeOfRecord(DateLibrary.getIso8601StringAsTime("2010-07-07T11:29:38-04:00").getTime());
    record.setVehicleId(_vehicleId);
    _vehicleLocationListener.handleVehicleLocationRecord(record);
    
    /**
     * We call this without first injecting real-time data first
     */
    Siri siri = getResponse("stop-monitoring.xml?key=TEST&MonitoringRef=305175&OperatorRef=2008&time="
        + _timeString);

    ServiceDelivery serviceDelivery = siri.ServiceDelivery;
    assertEquals(_time.getTime(),
        serviceDelivery.ResponseTimestamp.getTimeInMillis());

    List<StopMonitoringDelivery> deliveries = serviceDelivery.stopMonitoringDeliveries;
    /* there's only one stop requested */
    assertTrue(deliveries.size() == 1);
    StopMonitoringDelivery delivery = deliveries.get(0);

    assertEquals(_time.getTime(),delivery.ResponseTimestamp.getTimeInMillis());

    assertNotNull(delivery.visits);
    assertEquals(1,delivery.visits.size());
    
    MonitoredStopVisit visit = delivery.visits.get(0);
    assertEquals(record.getTimeOfRecord(),visit.RecordedAtTime.getTimeInMillis());
    
    // TODO : Should this be the case?
    // assertEquals("305175",visit.MonitoringRef);
    
    /* no onward calls requested so none returned */
    MonitoredVehicleJourney journey = visit.MonitoredVehicleJourney;
    assertEquals("20100627CC_069000_B43_0033_B62_8",journey.CourseOfJourneyRef);
    assertEquals("801027",journey.DestinationRef);
    assertEquals("1",journey.DirectionRef);
    assertEquals("B43",journey.LineRef);
    assertTrue(journey.Monitored);
    assertEquals("305287",journey.OriginRef);
    assertEquals("B43 LEFRTS GDNS PROSPCT PK STA",journey.PublishedLineName);
    assertEquals(40.737194,journey.VehicleLocation.Latitude,1e-6);
    assertEquals(-73.955673,journey.VehicleLocation.Longitude,1e-6);
    assertEquals(_vehicleId.toString(),journey.VehicleRef);
    
    assertNull(journey.OnwardCalls);

    MonitoredCall mc = journey.MonitoredCall;
    assertEquals("305175",mc.StopPointRef);
    assertFalse(mc.VehicleAtStop);
    assertEquals(1,mc.VisitNumber);
    
    DistanceExtensions dex = mc.Extensions;
    assertEquals(1762.7,dex.Distances.CallDistanceAlongRoute,0.1);
    assertEquals(1762.8,dex.Distances.DistanceFromCall,0.1);
    assertEquals(9,dex.Distances.StopsFromCall);
  }

  //@Test
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
    String port = System.getProperty("org.onebusaway.webapp.port", "9000");
    String url = "http://localhost:" + port + "/onebusaway-api-webapp/siri/"
        + query;
    GetMethod get = new GetMethod(url);
    client.executeMethod(get);

    String response = get.getResponseBodyAsString();
    assertTrue(response, response.startsWith("<Siri"));

    XStream xstream = new XStream();
    xstream.processAnnotations(Siri.class);
    Siri siri = (Siri) xstream.fromXML(response);
    assertNotNull(siri);
    return siri;
  }
}
