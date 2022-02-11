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

package org.onebusaway.nyc.report_archive.model;

import org.junit.Test;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.Assert.assertEquals;

public class NycCancelledTripRecordTest {

    @Test
    public void nycCancelledBeanToRecordTest() throws ParseException {
        String blockId = "MTA NYCT_FB_A2-Weekday-SDon_E_FB_26580_B41-207";
        String tripId = "MTA NYCT_FB_A2-Weekday-SDon-044900_B41_207";
        String route = "B41";
        String routeId = "MTA NYCT_B41";
        String firstStopId = "MTA_303215";
        String scheduledPullout = "2022-01-21T07:23:00";
        String status = "canceled";
        long timestamp = 1642734418000l;
        LocalTime firstDepartureTime = LocalTime.of(7,29,0,0);
        LocalTime lastStopArrivalTime = LocalTime.of(7,29,0,0);
        LocalDateTime humanReadableTimestamp = LocalDateTime.of(LocalDate.of(2022,1,20),
                LocalTime.of(22,6,58,0));
        LocalDate serviceDate = LocalDate.of(2022,1,21);


        NycCancelledTripBean nycCancelledTripBean = new NycCancelledTripBean();
        nycCancelledTripBean.setBlock(blockId);
        nycCancelledTripBean.setFirstStopDepartureTime(firstDepartureTime);
        nycCancelledTripBean.setFirstStopId(firstStopId);
        nycCancelledTripBean.setHumanReadableTimestamp(humanReadableTimestamp);
        nycCancelledTripBean.setLastStopArrivalTime(lastStopArrivalTime);
        nycCancelledTripBean.setRoute(route);
        nycCancelledTripBean.setRouteId(routeId);
        nycCancelledTripBean.setScheduledPullOut(scheduledPullout);
        nycCancelledTripBean.setServiceDate(serviceDate);
        nycCancelledTripBean.setStatus(status);
        nycCancelledTripBean.setTimestamp(timestamp);
        nycCancelledTripBean.setTrip(tripId);

        NycCancelledTripRecord record = new NycCancelledTripRecord(nycCancelledTripBean);
        assertEquals(blockId, record.getBlock());
    }
}
