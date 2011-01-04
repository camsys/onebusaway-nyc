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
import org.onebusaway.siri.model.MonitoredVehicleJourney;
import org.onebusaway.siri.model.ServiceDelivery;
import org.onebusaway.siri.model.Siri;
import org.onebusaway.siri.model.VehicleActivity;
import org.onebusaway.utility.DateLibrary;

public class VehicleMonitoringIntegrationTest extends SiriIntegrationTestBase {

  @Test
  public void testByVehicleId() throws HttpException, IOException,
      InterruptedException, ParseException {

    String timeString = "2010-12-07T11:29:25-08:00";
    Date time = DateLibrary.getIso8601StringAsTime(timeString);

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

    Siri siri = getResponse("vehicle-monitoring.xml?key=TEST&OperatorRef=2008&VehicleRef="
        + _vehicleId.getId() + "&time=" + timeString);

    ServiceDelivery serviceDelivery = siri.ServiceDelivery;
    assertEquals(time.getTime(),
        serviceDelivery.ResponseTimestamp.getTimeInMillis());

    List<VehicleActivity> deliveries = serviceDelivery.VehicleMonitoringDelivery.deliveries;
    assertNotNull(serviceDelivery.VehicleMonitoringDelivery.deliveries);
    /* there's only one stop requested */
    assertTrue(deliveries.size() == 1);
    VehicleActivity delivery = deliveries.get(0);

    /* no onward calls requested so none returned */
    MonitoredVehicleJourney journey = delivery.MonitoredVehicleJourney;
    assertNull(journey.OnwardCalls);

    assertEquals("B63", journey.LineRef);
  }

  @Test
  public void testOnwardCalls() throws HttpException, IOException,
      InterruptedException, ParseException {

    String timeString = "2010-12-07T11:29:25-08:00";
    Date time = DateLibrary.getIso8601StringAsTime(timeString);

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

    Siri siri = getResponse("vehicle-monitoring.xml?key=TEST&OperatorRef=2008&VehicleMonitoringDetailLevel=calls&VehicleRef="
        + _vehicleId.getId() + "&time=" + timeString);

    ServiceDelivery serviceDelivery = siri.ServiceDelivery;
    assertEquals(time.getTime(),
        serviceDelivery.ResponseTimestamp.getTimeInMillis());

    List<VehicleActivity> deliveries = serviceDelivery.VehicleMonitoringDelivery.deliveries;
    assertNotNull(serviceDelivery.VehicleMonitoringDelivery.deliveries);
    /* there's only one stop requested */
    assertTrue(deliveries.size() == 1);
    VehicleActivity delivery = deliveries.get(0);

    /* onward calls requested */
    MonitoredVehicleJourney journey = delivery.MonitoredVehicleJourney;
    assertNotNull(journey.OnwardCalls);

    assertEquals("B63", journey.LineRef);
  }
}
