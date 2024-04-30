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
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.service.StrollerVehiclesDataExtractionService;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.*;

@Path("/stroller")
@Component
@Scope("request")
public class StrollerVehiclesResource {

    /**
     * tdm api resource to expose stroller bus data extracted from SPEAR
     * epic link: https://camsys.atlassian.net/browse/OBANYC-3296
     *
     * @author caylasavitzky
     *
     */

    private DepotIdTranslator depotIdTranslator = null;

    private ObjectMapper _mapper = new ObjectMapper();

    @Autowired
    private JsonTool jsonTool;

    public StrollerVehiclesResource() throws IOException {
        try {
            depotIdTranslator = new DepotIdTranslator(new File(System.getProperty("tdm.depotIdTranslationFile")));
        } catch (IOException | NullPointerException e) {
            // Set depotIdTranslator to null and otherwise do nothing.
            // Everything works fine without the depot id translator.
            depotIdTranslator = null;
        }
    }

    private static Logger _log = LoggerFactory.getLogger(StrollerVehiclesResource.class);

    @Autowired
    StrollerVehiclesDataExtractionService _strollerVehiclesDataExtractionService;

    @Path("/set")
    @GET
    @Produces("application/json")
    public Response getStrollerVehiclesSet() {
        Set<AgencyAndId> strollerVehiclesData = _strollerVehiclesDataExtractionService.getStrollerVehiclesAsSet(depotIdTranslator);
        String output;
        try {
            StringWriter writer = new StringWriter();
            _mapper.writeValue(writer, strollerVehiclesData);
            output = writer.toString();
        }catch (Exception e){
            _log.error("Unable to process stroller vehicles data", e);
            return Response.serverError().build();
        }
        _log.debug("Returning response ok for url=" + null);
        return Response.ok(output).build();
    }

    public void setStrollerVehiclesDataExtractionService(StrollerVehiclesDataExtractionService strollerVehiclesDataExtractionService) {
        _strollerVehiclesDataExtractionService = strollerVehiclesDataExtractionService;
    }

    public void setJsonTool(JsonTool tool){
        this.jsonTool=tool;
    }
}
