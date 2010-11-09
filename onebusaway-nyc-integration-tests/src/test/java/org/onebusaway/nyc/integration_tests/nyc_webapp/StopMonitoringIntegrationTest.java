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
import java.text.ParseException;
import java.util.List;

import org.apache.commons.httpclient.HttpException;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.siri.model.DistanceExtensions;
import org.onebusaway.siri.model.MonitoredCall;
import org.onebusaway.siri.model.MonitoredStopVisit;
import org.onebusaway.siri.model.MonitoredVehicleJourney;
import org.onebusaway.siri.model.ServiceDelivery;
import org.onebusaway.siri.model.Siri;
import org.onebusaway.siri.model.StopMonitoringDelivery;
import org.onebusaway.utility.DateLibrary;


public class StopMonitoringIntegrationTest extends SiriIntegrationTestBase {

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
    assertNotNull(journey.MonitoredCall);
    assertNotNull(journey.MonitoredCall.Extensions);
    Double distanceFromRequestedStop = journey.MonitoredCall.Extensions.Distances.DistanceFromCall;
    assertTrue(journey.OnwardCalls.get(0).Extensions.Distances.DistanceFromCall > distanceFromRequestedStop);

  }
}
