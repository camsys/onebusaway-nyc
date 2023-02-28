package org.onebusaway.nyc.transit_data_manager.api;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_manager.adapters.data.VehicleDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.service.KneelingVehiclesDataExtractionService;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.*;

@Path("/kneeling")
@Component
@Scope("request")
public class KneelingVehiclesResource {

    /**
     * tdm api resource to expose kneeling bus data extracted from SPEAR
     * epic link: https://camsys.atlassian.net/browse/OBANYC-3296
     *
     * @author caylasavitzky
     *
     */

    private DepotIdTranslator depotIdTranslator = null;

    private ObjectMapper _mapper = new ObjectMapper();

    @Autowired
    private JsonTool jsonTool;

    public KneelingVehiclesResource() throws IOException {
        try {
            depotIdTranslator = new DepotIdTranslator(new File(System.getProperty("tdm.depotIdTranslationFile")));
        } catch (IOException | NullPointerException e) {
            // Set depotIdTranslator to null and otherwise do nothing.
            // Everything works fine without the depot id translator.
            depotIdTranslator = null;
        }
    }

    private static Logger _log = LoggerFactory.getLogger(KneelingVehiclesResource.class);

    @Autowired
    KneelingVehiclesDataExtractionService _kneelingVehiclesDataExtractionService;

    @Path("/set")
    @GET
    @Produces("application/json")
    public Response getKneelingVehiclesSet() {
        Set<AgencyAndId> kneelingVehiclesData = _kneelingVehiclesDataExtractionService.getKneelingVehiclesAsSet(depotIdTranslator);
        String output;
        try {
            StringWriter writer = new StringWriter();
            _mapper.writeValue(writer, kneelingVehiclesData);
            output = writer.toString();
        }catch (Exception e){
            _log.error("Unable to process kneeling vehicles data", e);
            return Response.serverError().build();
        }
        _log.debug("Returning response ok for url=" + null);
        return Response.ok(output).build();
    }

    public void setKneelingVehiclesDataExtractionService(KneelingVehiclesDataExtractionService kneelingVehiclesDataExtractionService) {
        _kneelingVehiclesDataExtractionService = kneelingVehiclesDataExtractionService;
    }

    public void setJsonTool(JsonTool tool){
        this.jsonTool=tool;
    }
}
