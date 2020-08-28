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

package org.onebusaway.nyc.vehicle_tracking.webapp.controllers;

import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Produces;
import java.io.IOException;
import java.io.OutputStreamWriter;

/**
 * Port of TripsForRouteAction from onebusaway-api.
 */
@Controller
public class TripsForRouteController {

    private ObjectMapper _mapper = new ObjectMapper();
    @Autowired
    private NycTransitDataService _service;
    private static final int VTW_MAX_COUNT = 100;

    @RequestMapping(value="/trips-for-route.do", method= RequestMethod.GET)
    @Produces("application/json")
    public void getTrips(HttpServletResponse response, @RequestParam(value = "id") String id) throws IOException {

        TripsForRouteQueryBean query = new TripsForRouteQueryBean();
        query.setRouteId(id);
        query.setTime(System.currentTimeMillis());
        query.setMaxCount(VTW_MAX_COUNT); // this would typically use MaxCount but NYC doesn't support it


        TripDetailsInclusionBean inclusion = query.getInclusion();
        inclusion.setIncludeTripBean(true);
        inclusion.setIncludeTripSchedule(true);
        inclusion.setIncludeTripStatus(false);


        ListBean<TripDetailsBean> tripsForRoute = _service.getTripsForRoute(query);
        OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream());
        _mapper.writeValue(writer, tripsForRoute);
    }
}
