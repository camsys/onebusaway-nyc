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
package org.onebusaway.nyc.transit_data_manager.api.cancelledTripsApi;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.onebusaway.nyc.transit_data_manager.config.ConfigurationDatastoreInterface;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CancelledTripsIntegratorTest {

    /**
     * unit testing for canceled trips integrator
     *
     * @author caylasavitzky
     *
     */

    @Mock
    ConfigurationDatastoreInterface datastoreInterface;

    @Mock
    ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Test
    public void testCapiOutToNYCCancelledTripBeans() throws JsonProcessingException, ParseException {

        MockitoAnnotations.initMocks(this);

        String fakeCAPIOutput = "{\"impacted\":[{\"block\":\"MTABC_JKPA2-JK_A2-Weekday-01-SDon_6193636\",\"trip\":\"MTABC_32246617-JKPA2-JK_A2-Weekday-01-SDon\",\"status\":\"canceled\",\"timestamp\":1642743832000,\"scheduledPullOut\":\"2022-01-21T07:03:00\",\"humanReadableTimestamp\":\"2022-01-21T00:43:52\",\"serviceDate\":\"2022-01-21\",\"route\":\"Q9\",\"firstStopId\":\"MTA_550031\",\"firstStopDepartureTime\":\"07:23:00\"},{\"block\":\"MTA NYCT_FB_A2-Weekday-SDon_E_FB_26580_B41-207\",\"trip\":\"MTA NYCT_FB_A2-Weekday-SDon-044900_B41_207\",\"status\":\"canceled\",\"timestamp\":1642734418000,\"scheduledPullOut\":\"2022-01-21T07:23:00\",\"humanReadableTimestamp\":\"2022-01-20T22:06:58\",\"serviceDate\":\"2022-01-21\",\"route\":\"B41\",\"firstStopId\":\"MTA_303215\",\"firstStopDepartureTime\":\"07:29:00\"},{\"block\":\"MTA NYCT_FP_A2-Weekday-SDon_E_FP_34740_Q54-721\",\"trip\":\"MTA NYCT_FP_A2-Weekday-SDon-058900_Q54_721\",\"status\":\"canceled\",\"timestamp\":1642734447000,\"scheduledPullOut\":\"2022-01-21T09:39:00\",\"humanReadableTimestamp\":\"2022-01-20T22:07:27\",\"serviceDate\":\"2022-01-21\",\"route\":\"Q54\",\"firstStopId\":\"MTA_308488\",\"firstStopDepartureTime\":\"09:49:00\"}],\"timestamp\":\"2022-01-21T10:40:51\"}";
        StringBuffer buffer = new StringBuffer();
        buffer.append(fakeCAPIOutput);
        CancelledTripsIntegrator integrator = new CancelledTripsIntegrator();

        List<NycCancelledTripBean> beans = integrator.makeCancelledTripBeansFromCapiOutput(buffer);

        assertTrue(beans.size()==3);
        NycCancelledTripBean bean = beans.get(1);


        assertEquals("MTA NYCT_FB_A2-Weekday-SDon_E_FB_26580_B41-207",bean.getBlock());
        assertEquals("MTA NYCT_FB_A2-Weekday-SDon-044900_B41_207",bean.getTrip());
        assertEquals("canceled",bean.getStatus());
        assertEquals(new SimpleDateFormat("yyyy-MM-dd").parse("2022-01-21"),bean.getServiceDate());
        assertEquals("B41",bean.getRoute());
        assertEquals("MTA_303215",bean.getFirstStopId());
        assertEquals(new SimpleDateFormat("HH:mm:ss").parse("07:29:00").getTime(),bean.getFirstStopDepartureTime().getTime());
        assertTrue(bean.getTimestamp()==Long.valueOf("1642734418000"));

//        todo: set datetime read in as the correct timezone
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss");
        DateTime dateTime = new DateTime(format.parse("2022-01-21T07:23:00"),DateTimeZone.getDefault());
        assertEquals(dateTime,bean.getScheduledPullOut());
        dateTime = new DateTime(format.parse("2022-01-20T22:06:58"),DateTimeZone.getDefault());
        assertEquals(dateTime,bean.getHumanReadableTimestamp());
    }


    @Test
    public void testDataCollectionToCancelledTripBeans() throws JsonProcessingException {
//        todo fix this test to work if the api isn't on'
        MockitoAnnotations.initMocks(this);

        CancelledTripsIntegrator integrator = new CancelledTripsIntegrator();

        integrator.setUrl("http://capi.dev.obanyc.com:8084/api/canceled-trips.json");
        StringBuffer buffer = integrator.getCancelledTripsData();
        String string = buffer.toString();
        List<NycCancelledTripBean> beans = integrator.makeCancelledTripBeansFromCapiOutput(buffer);
        assertTrue(beans.size()>0);

    }
}
