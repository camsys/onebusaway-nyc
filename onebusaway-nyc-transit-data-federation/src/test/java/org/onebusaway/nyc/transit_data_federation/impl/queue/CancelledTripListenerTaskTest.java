/*
 * Copyright (C)  2011 Metropolitan Transportation Authority
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onebusaway.nyc.transit_data_federation.impl.queue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.impl.capi.CapiDaoFileImpl;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data_federation.impl.CancelledTripServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class CancelledTripListenerTaskTest {

    @Mock
    NycTransitDataService tds;

    @Spy
    CapiDaoFileImpl capiFileDao;

    @Spy
    CancelledTripServiceImpl cancelledTripService;

    @Before
    public void setup(){
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testCapiOutToNYCCancelledTripBeans() throws Exception {
        // Needed to pass validation
        when(tds.getTrip(anyString())).thenReturn(new TripBean());

        // Load sample capi input
        capiFileDao.setLocation("tdm_capi_input.json");

        CancelledTripHttpListenerTask listenerTask = new CancelledTripHttpListenerTask();
        listenerTask.setNycTransitDataService(tds);
        listenerTask.setCapiDao(capiFileDao);
        listenerTask.setCancelledTripService(cancelledTripService);
        listenerTask.updateData();
        ListBean<CancelledTripBean> beans = cancelledTripService.getAllCancelledTrips();

        assertEquals(3,beans.getList().size());

        // Check that deserialized values match expected values
        CancelledTripBean bean = beans.getList().get(1);
        assertEquals("MTA NYCT_FB_A2-Weekday-SDon_E_FB_26580_B41-207",bean.getBlock());
        assertEquals("MTA NYCT_FB_A2-Weekday-SDon-044900_B41_207",bean.getTrip());
        assertEquals("canceled",bean.getStatus());
        assertEquals("2022-01-21",bean.getServiceDate());
        assertEquals("B41",bean.getRoute());
        assertEquals("MTA NYCT_B41", bean.getRouteId());
        assertEquals("MTA_303215",bean.getFirstStopId());
        assertEquals("07:29:00",bean.getFirstStopDepartureTime());
        assertEquals("07:49:00",bean.getLastStopArrivalTime());
        assertTrue(bean.getTimestamp()==Long.valueOf("1642734418000"));
        assertEquals("2022-01-21T07:23:00",bean.getScheduledPullOut());
        assertEquals("2022-01-20T22:06:58",bean.getHumanReadableTimestamp());    }
}
