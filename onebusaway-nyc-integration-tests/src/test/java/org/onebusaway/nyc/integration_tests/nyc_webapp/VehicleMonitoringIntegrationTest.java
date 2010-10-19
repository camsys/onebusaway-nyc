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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.realtime.api.VehicleLocationRecord;
import org.onebusaway.siri.model.MonitoredVehicleJourney;
import org.onebusaway.siri.model.ServiceDelivery;
import org.onebusaway.siri.model.Siri;
import org.onebusaway.siri.model.VehicleActivity;
import org.onebusaway.utility.DateLibrary;

import org.apache.commons.httpclient.HttpException;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.util.GregorianCalendar;
import java.util.List;

public class VehicleMonitoringIntegrationTest extends SiriIntegrationTest {

  @Test
  public void testByVehicleId() throws HttpException, IOException,
      InterruptedException, ParseException {

    // 2179,40.7372545433,-73.9557642325,2010-07-07
    // 11:29:38,4430,2008_12023519,1278475200000,43753.36285532166,4430,40.7372545433,-73.9557642325,IN_PROGRESS

    VehicleLocationRecord record = new VehicleLocationRecord();
    record.setBlockId(new AgencyAndId("2008", "12023519"));
    record.setCurrentLocationLat(40.7372545433);
    record.setCurrentLocationLon(-73.9557642325);
    record.setDistanceAlongBlock(43753.36285532166);
    record.setServiceDate(1278475200000L);
    record.setTimeOfRecord(DateLibrary.getIso8601StringAsTime("2010-07-07T11:29:38-04:00").getTime());
    record.setVehicleId(_vehicleId);
    _vehicleLocationListener.handleVehicleLocationRecord(record);

    Siri siri = getResponse("vehicle-monitoring.xml?key=TEST&OperatorRef=2008&VehicleRef="
        + _vehicleId.getId() + "&time=" + _timeString);

    GregorianCalendar now = new GregorianCalendar();

    ServiceDelivery serviceDelivery = siri.ServiceDelivery;
    assertTrue(serviceDelivery.ResponseTimestamp.getTimeInMillis()
        - now.getTimeInMillis() < 1000);
    List<VehicleActivity> deliveries = serviceDelivery.VehicleMonitoringDelivery.deliveries;
    assertNotNull(serviceDelivery.VehicleMonitoringDelivery.deliveries);
    /* there's only one stop requested */
    assertTrue(deliveries.size() == 1);
    VehicleActivity delivery = deliveries.get(0);

    /* no onward calls requested so none returned */
    MonitoredVehicleJourney journey = delivery.MonitoredVehicleJourney;
    assertNull(journey.OnwardCalls);

    assertEquals("B43", journey.LineRef);
  }

}
