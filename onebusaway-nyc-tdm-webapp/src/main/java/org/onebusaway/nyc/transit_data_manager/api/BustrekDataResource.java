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
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.stifExtract.model.BustrekDatum;
import org.onebusaway.nyc.transit_data_federation.services.nyc.BusTrekDataService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

@Component
@Path("/bustrek")
@Scope("singleton")
public class BustrekDataResource {

    private static Logger _log = LoggerFactory.getLogger(BustrekDataResource.class);

    @Autowired
    BusTrekDataService busTrekDataService;

    private ObjectMapper _mapper;

    @PostConstruct
    public void setup() {
        setupObjectMapper();
    }

    protected void setupObjectMapper(){
        _mapper = new ObjectMapper();
        _mapper.registerModule(new JavaTimeModule());
        _mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
        _mapper.setTimeZone(Calendar.getInstance().getTimeZone());
    }

    @Path("/remarks")
    @GET
    @Produces("application/json")
    public Response getRemarksList(){
        return generateResponse(busTrekDataService.getStifRemarks());
    }

    @Path("/timepoints")
    @GET
    @Produces("application/json")
    public Response getTimepointsList(){
        return generateResponse(busTrekDataService.getStifTimePoints());
    }
    @Path("/tripinfo")
    @GET
    @Produces("application/json")
    public Response getTripInfoList(){
        return generateResponse(busTrekDataService.getStifTripInfos());
    }

    private Response generateResponse(ListBean<BustrekDatum> beans) {
        try {
            String output = getAsJson(beans);
            return Response.ok(output).build();
        } catch (Exception e) {
            _log.error("Unable to process remarks list request", e);
            return Response.serverError().build();
        }
    }

    public String getAsJson(Object object){
        StringWriter writer = null;
        String output = "";
        try {
            writer = new StringWriter();
            _mapper.writeValue(writer, object);
            output = writer.toString();
        } catch (IOException e) {
            _log.error("exception parsing json " + e, e);
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                _log.error("Error closing writer", e);
            }
            return output;
        }

    }
}
