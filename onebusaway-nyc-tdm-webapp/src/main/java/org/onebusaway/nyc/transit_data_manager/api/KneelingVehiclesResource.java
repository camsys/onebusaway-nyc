package org.onebusaway.nyc.transit_data_manager.api;


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
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    @Path("/list")
    @GET
    @Produces("application/json")
    public Response getKneelingVehiclesList() {
        List kneelingVehiclesData = _kneelingVehiclesDataExtractionService.getKneelingVehiclesAsList(depotIdTranslator);
        String output;
        try {
            StringWriter stringWriter = new StringWriter();

            jsonTool.writeJson(stringWriter, kneelingVehiclesData);

            output = stringWriter.toString();

            stringWriter.close();
        }catch (Exception e){
            _log.error("Unable to process cancelled trips list request", e);
            return Response.serverError().build();
        }
//            String output = processKneelingVehiclesData(kneelingVehiclesData);
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
