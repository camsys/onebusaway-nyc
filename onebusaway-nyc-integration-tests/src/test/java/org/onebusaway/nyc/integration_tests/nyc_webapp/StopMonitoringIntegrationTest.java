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
import java.util.Date;
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
    
    _vehicleLocationListener.resetVehicleLocation(_vehicleId);

    /**
     * We call this without first injecting real-time data first
     */
    Siri siri = getResponse("stop-monitoring.xml?key=TEST&MonitoringRef=305413&OperatorRef=2008&time="
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
    
    String timeString = "2010-12-07T11:29:25-08:00";
    Date time = DateLibrary.getIso8601StringAsTime(timeString);

    /**
     * Let's inject a location record
     */
    
    //2179,40.7372545433,-73.9557642325,2010-07-07 11:29:38,4430,2008_12023519,1278475200000,43753.36285532166,4430,40.7372545433,-73.9557642325,IN_PROGRESS

    VehicleLocationRecord record = new VehicleLocationRecord();
    record.setBlockId(new AgencyAndId("2008", "12888410"));
    record.setCurrentLocationLat(40.65266509229019);
    record.setCurrentLocationLon(-74.00245398573307);
    record.setDistanceAlongBlock(53097.667367660964);
    record.setServiceDate(1291698000000L);
    record.setTimeOfRecord(time.getTime());
    record.setVehicleId(_vehicleId);
    
    // Reset any existing data first
    _vehicleLocationListener.resetVehicleLocation(record.getVehicleId());
    _vehicleLocationListener.handleVehicleLocationRecord(record);

    Siri siri = getResponse("stop-monitoring.xml?key=TEST&MonitoringRef=305364&OperatorRef=2008&time=" + timeString);
    
    ServiceDelivery serviceDelivery = siri.ServiceDelivery;
    assertEquals(time.getTime(),
        serviceDelivery.ResponseTimestamp.getTimeInMillis());

    List<StopMonitoringDelivery> deliveries = serviceDelivery.stopMonitoringDeliveries;
    /* there's only one stop requested */
    assertTrue(deliveries.size() == 1);
    StopMonitoringDelivery delivery = deliveries.get(0);

    assertEquals(time.getTime(),delivery.ResponseTimestamp.getTimeInMillis());

    assertNotNull(delivery.visits);
    assertEquals(1,delivery.visits.size());
    
    MonitoredStopVisit visit = delivery.visits.get(0);
    assertEquals(record.getTimeOfRecord(),visit.RecordedAtTime.getTimeInMillis());
    
    // TODO : Should this be the case?
    // assertEquals("305286",visit.MonitoringRef);
    
    /* no onward calls requested so none returned */
    MonitoredVehicleJourney journey = visit.MonitoredVehicleJourney;
    assertEquals("20101024EE_082800_B63_0089_B35_45", journey.CourseOfJourneyRef);
    assertEquals("801041", journey.DestinationRef);
    assertEquals("0", journey.DirectionRef);
    assertEquals("B63",journey.LineRef);
    assertTrue(journey.Monitored);
    assertEquals("306619", journey.OriginRef);
    assertEquals("B63 COBBLE HILL COLUMBIA ST via 5 AV", journey.PublishedLineName);
    assertEquals(40.65266509229019, journey.VehicleLocation.Latitude, 1e-6);
    assertEquals(-74.00245398573307, journey.VehicleLocation.Longitude, 1e-6);
    assertEquals(_vehicleId.toString(),journey.VehicleRef);
    
    assertNull(journey.OnwardCalls);

    MonitoredCall mc = journey.MonitoredCall;
    assertEquals("305364", mc.StopPointRef);
    assertFalse(mc.VehicleAtStop);
    assertEquals(1,mc.VisitNumber);
    
    DistanceExtensions dex = mc.Extensions;
    assertEquals(5568.9, dex.Distances.CallDistanceAlongRoute, 0.1);
    assertEquals(27.7, dex.Distances.DistanceFromCall, 0.1);
    assertEquals(0, dex.Distances.StopsFromCall);
  }

  //@Test
  public void testOnwardCalls() throws HttpException, IOException {

    Siri siri = getResponse("stop-monitoring.xml?key=TEST&OperatorRef=2008&MonitoringRef=305286&StopMonitoringDetailLevel=calls");

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
