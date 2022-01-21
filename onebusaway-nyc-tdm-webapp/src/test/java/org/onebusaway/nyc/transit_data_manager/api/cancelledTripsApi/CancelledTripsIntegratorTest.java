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
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.onebusaway.nyc.transit_data_manager.config.ConfigurationDatastoreInterface;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

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
    public void testCapiOutToNYCCancelledTripBeans() throws JsonProcessingException {

        MockitoAnnotations.initMocks(this);

        String fakeCAPIOutput = "{\"impacted\":{\"block\":\"MTABC_JKPA2-JK_A2-Weekday-01-SDon_6193636\"\"trip\":\"MTABC_32246617-JKPA2-JK_A2-Weekday-01-SDon\"\"status\":\"canceled\"\"timestamp\":1642743832000\"scheduledPullOut\":\"2022-01-21T07:03:00\"\"humanReadableTimestamp\":\"2022-01-21T00:43:52\"\"serviceDate\":\"2022-01-21\"\"route\":\"Q9\"\"firstStopId\":\"MTA_550031\"\"firstStopDepartureTime\":\"07:23:00\"},{\"block\":\"MTA NYCT_FB_A2-Weekday-SDon_E_FB_26580_B41-207\"\"trip\":\"MTA NYCT_FB_A2-Weekday-SDon-044900_B41_207\"\"status\":\"canceled\"\"timestamp\":1642734418000\"scheduledPullOut\":\"2022-01-21T07:23:00\"\"humanReadableTimestamp\":\"2022-01-20T22:06:58\"\"serviceDate\":\"2022-01-21\"\"route\":\"B41\"\"firstStopId\":\"MTA_303215\"\"firstStopDepartureTime\"}],\"timestamp\":\"2022-01-21T10:40:51\"}";
        StringBuffer buffer = new StringBuffer();
        buffer.append(fakeCAPIOutput);
        CancelledTripsIntegrator integrator = new CancelledTripsIntegrator();

        List<NycCancelledTripBean> beans = integrator.makeCancelledTripBeansFromCapiOutput(buffer);

        NycCancelledTripBean bean1 = beans.get(0);
        assertEquals(bean1.getBlock(),"MTA NYCT 1");
        assertEquals(bean1.getStatus(),"Cancelled");
        assertEquals(bean1.getTrip(),"MTA NYCT 1_1");
        Calendar cal = Calendar.getInstance();
        //20210114 12:45:00
        cal.set(2021,00,14,12,45,00);
        SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        System.out.println("The date 1 is: " + sdformat.format(cal.getTime()));
//        System.out.println("The date 2 is: " + sdformat.format(bean1.getTimestamp()));
//        int datesAreTheSame = bean1.getTimestamp().compareTo(cal.getTime());
////        todo find out why these are not the same WHEN THEY LOOK EXACTLY THE SAME??
//        assertTrue(datesAreTheSame==0);

        NycCancelledTripBean bean2 = beans.get(1);
        assertEquals(bean2.getBlock(),"MTA NYCT 2");
        assertEquals(bean2.getStatus(),"Cancelled");
        assertEquals(bean2.getTrip(),"MTA NYCT 2_1");
        cal.set(2021,01,14,12,46,00);
//        assertEquals(bean2.getTimestamp(),cal.getTime());
    }


    @Test
    public void testDataCollectionToCancelledTripBeans() throws JsonProcessingException {

        MockitoAnnotations.initMocks(this);

        CancelledTripsIntegrator integrator = new CancelledTripsIntegrator();

        integrator.setUrl("http://capi.dev.obanyc.com:8084/api/canceled-trips.json");
        StringBuffer buffer = integrator.getCancelledTripsData();
        String string = buffer.toString();
        List<NycCancelledTripBean> beans = integrator.makeCancelledTripBeansFromCapiOutput(buffer);
        assertTrue(beans.size()<0);

    }
}
