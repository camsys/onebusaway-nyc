package org.onebusaway.nyc.transit_data_manager.api;


import org.onebusaway.nyc.transit_data_manager.api.service.KneelingVehiclesDataExtractionService;
import org.onebusaway.transit_data.model.trips.CancelledTripBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.swing.text.html.parser.DTD;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

public class KneelingVehiclesResouce {

    /**
     * tdm api resource to expose kneeling bus data extracted form SPEAR
     * epic link: https://camsys.atlassian.net/browse/OBANYC-3296
     *
     * @author caylasavitzky
     *
     */

    @Autowired
    private static Logger _log = LoggerFactory.getLogger(KneelingVehiclesResouce.class);

    @Autowired
    KneelingVehiclesDataExtractionService _kneelingVehiclesDataExtractionService;

    @Path("/list")
    @GET
    @Produces("application/json")
    public Response getKneelingVehiclesList() {
        try {
            List kneelingVehiclesData = _kneelingVehiclesDataExtractionService.getKneelingVehiclesList();
            String output = processKneelingVehiclesData(kneelingVehiclesData);
            _log.debug("Returning response ok for url=" + null);
            return Response.ok(output).build();
        } catch (Exception e){
            _log.error("Unable to process cancelled trips list request", e);
            return Response.serverError().build();
        }
    }

    @Path("/map")
    @GET
    @Produces("application/json")
    public Response getKneelingVehiclesMap() {
        try {
            Map kneelingVehiclesData = _kneelingVehiclesDataExtractionService.getKneelingVehiclesMap();
            String output = processKneelingVehiclesMapData(kneelingVehiclesData);
            _log.debug("Returning response ok for url=" + null);
            return Response.ok(output).build();
        } catch (Exception e){
            _log.error("Unable to process cancelled trips list request", e);
            return Response.serverError().build();
        }
    }

    protected String processKneelingVehiclesData(List kneelingVehiclesData){
        _log.error("processKneelingVehiclesData not implemented yet");
        return null;
    }

    protected String processKneelingVehiclesMapData(Map kneelingVehiclesData){
        _log.error("processKneelingVehiclesData not implemented yet");
        return null;
    }

    public void setKneelingVehiclesDataExtractionService(KneelingVehiclesDataExtractionService kneelingVehiclesDataExtractionService) {
        _kneelingVehiclesDataExtractionService = kneelingVehiclesDataExtractionService;
    }


}
