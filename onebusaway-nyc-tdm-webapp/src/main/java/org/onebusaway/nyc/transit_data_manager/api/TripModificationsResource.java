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

package org.onebusaway.nyc.transit_data_manager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.onebusaway.nyc.transit_data_manager.api.service.CapiRetrievalService;
import org.onebusaway.nyc.transit_data_manager.api.service.TripModificationsRetreivalService;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

@Path("/trip-modifications")
@Component
@Scope("request")
public class TripModificationsResource {

    private static Logger _log = LoggerFactory.getLogger(TripModificationsResource.class);

    private TripModificationsRetreivalService _tripModificationsService;

    private final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));

    @Autowired
    public void setTripModificationsRetrievalService(
            TripModificationsRetreivalService tripModificationsService) {
        _tripModificationsService = tripModificationsService;
    }

    @Path("/feed")
    @GET
    public Response getTripModifications(
            @QueryParam("format") @DefaultValue("pb") String format) {

        try {
            var feed = _tripModificationsService.getTripModifications();

            if (format.equals("json")) {
                String json = com.google.protobuf.util.JsonFormat
                        .printer()
                        .includingDefaultValueFields()
                        .print(feed);

                return Response.ok(json, "application/json").build();
            }
            else {
                byte[] protoBytes = feed.toByteArray();

                return Response.ok(protoBytes, "application/x-protobuf")
                        .header("Content-Disposition", "inline; filename=trip-modifications.pb")
                        .build();

            }

        } catch (Exception e) {
            _log.error("Unable to process trip modifications request", e);
            return Response.serverError()
                    .entity("Failed to generate trip modifications feed")
                    .type("text/plain")
                    .build();
        }
    }
}
