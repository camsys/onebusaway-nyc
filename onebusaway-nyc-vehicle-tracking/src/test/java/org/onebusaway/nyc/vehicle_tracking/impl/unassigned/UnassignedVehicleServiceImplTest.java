/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.vehicle_tracking.impl.unassigned;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.vehicle_tracking.model.unassigned.UnassignedVehicleRecord;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.VehicleLocationRecord;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UnassignedVehicleServiceImplTest {

        private UnassignedVehicleRecord[] records;

        @InjectMocks
        private UnassignedVehicleServiceImpl service;

        @Mock
        private VehicleLocationInferenceService _vehicleLocationService;

        @Before
        public void prepare(){
            MockitoAnnotations.initMocks(this);
            this.service.setupMapper();
            URL url = getClass().getClassLoader().getResource("unassigned_vehicles.json");
            List<UnassignedVehicleRecord> direct = service.getUnassignedVehicleRecordsDirect(url);
            records = direct.toArray(new UnassignedVehicleRecord[direct.size()]);
        }

        @Test
        public void testVehiclePulloutEmptyList() throws Exception {

            assertEquals(8, records.length);

            UnassignedVehicleRecord record = records[0];

            assertEquals("MTA NYCT", record.getAgencyId());
            assertNull(record.getBlockId());
            assertNull(record.getDistanceAlongBlock());
            assertEquals(new Double(40.839765), record.getLatitude());
            assertEquals(new Double(-73.916263), record.getLongitude());
            assertEquals("SPOOKING", record.getPhase());
            assertEquals("MTA NYCT_WF_D9-Weekday-SDon-083100_BX35_218", record.getTripId());
            assertEquals("default", record.getStatus());

            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
            assertEquals(sdfDate.parse("2019-10-22").getTime(), record.getServiceDate().longValue());

            SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX'Z'");
            assertEquals(sdfDateTime.parse("2019-10-22T14:18:51-04:00Z").getTime(), record.getTimeReceived().longValue());

            assertEquals("301", record.getVehicleId());

        }

        @Test
        public void testActiveVehicle(){
            service.setMaxActiveVehicleAgeSecs(120); // 2 minutes

            long vehicleTimeReceived = 1569246300000l; // 09-23-2019 9:45:00
            when(_vehicleLocationService.getTimeReceivedByVehicleId((AgencyAndId) any())).thenReturn(vehicleTimeReceived);

            long currentTime = 1569246420000l; // 09-23-2019 9:47:00
            boolean isActive = service.vehicleActive(new AgencyAndId("agencyId","vehicleId"), currentTime);
            assertTrue(isActive);

            currentTime = 1569246421000l; // 09-23-2019 9:47:01
            isActive = service.vehicleActive(new AgencyAndId("agencyId","vehicleId"), currentTime);
            assertFalse(isActive);

            currentTime = 1569246419000l; // 09-23-2019 9:46:59
            isActive = service.vehicleActive(new AgencyAndId("agencyId","vehicleId"), currentTime);
            assertTrue(isActive);
        }

        @Test
        public void testInferredLocationBeanCreation(){
            UnassignedVehicleRecord record = records[0];

            NycQueuedInferredLocationBean inferredLocationBean = service.toNycQueueInferredLocationBean(record);

            assertEquals(record.getVehicleId(), AgencyAndId.convertFromString(inferredLocationBean.getVehicleId()).getId());
            assertEquals(record.getTimeReceived(), inferredLocationBean.getRecordTimestamp());
            assertEquals(record.getServiceDate(), inferredLocationBean.getServiceDate());
            assertEquals(record.getPhase(), inferredLocationBean.getPhase());
            assertEquals(record.getAgencyId(), AgencyAndId.convertFromString(inferredLocationBean.getVehicleId()).getAgencyId());
            assertEquals(record.getLatitude(), inferredLocationBean.getInferredLatitude());
            assertEquals(record.getLongitude(), inferredLocationBean.getInferredLongitude());
            assertEquals(record.getTripId(), inferredLocationBean.getTripId());
            assertEquals(record.getStatus(), inferredLocationBean.getStatus());
        }

        @Test
        public void testUnassignedVehicleToVLR(){
            UnassignedVehicleRecord record = records[0];

            NycQueuedInferredLocationBean inferredLocationBean = service.toNycQueueInferredLocationBean(record);
            VehicleLocationRecord vlr = inferredLocationBean.toVehicleLocationRecord();
            assertNotNull(vlr);
            assertEquals(vlr.getPhase(), EVehiclePhase.SPOOKING);
            assertTrue(Double.isNaN(vlr.getDistanceAlongBlock()));

            //assertNull(vlr.getScheduleDeviation());
        }


}
