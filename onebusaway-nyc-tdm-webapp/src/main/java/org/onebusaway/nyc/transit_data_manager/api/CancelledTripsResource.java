package org.onebusaway.nyc.transit_data_manager.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.onebusaway.nyc.transit_data.model.NycCancelledTripBean;
import org.onebusaway.nyc.transit_data_manager.api.service.CapiRetrievalService;
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

@Path("/cancelled-trips")
@Component
@Scope("singleton")
public class CancelledTripsResource {

    /**
     * pulls in data from Cancelledtrips API, generates beans, and pushes to queue
     *
     * @author caylasavitzky
     *
     */

    private static Logger _log = LoggerFactory.getLogger(CancelledTripsResource.class);

    private CapiRetrievalService _capiService;

    @Autowired
    public void setCapiService(CapiRetrievalService capiService){
        _capiService = capiService;
    }

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

    @Path("/list")
    @GET
    @Produces("application/json")
    public Response getCancelledTripsList() {
        try {
            List<NycCancelledTripBean> cancelledTripBeans = _capiService.getCancelledTripBeans();
            String output = getAsJson(cancelledTripBeans);
            _log.debug("Returning response ok for url=" + _capiService.getLocation());
            return Response.ok(output).build();
        } catch (Exception e){
            _log.error("Unable to process cancelled trips list request", e);
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