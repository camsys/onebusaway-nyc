package org.onebusaway.nyc.report_archive.api;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.report_archive.api.json.JsonTool;
import org.onebusaway.nyc.report_archive.api.json.LastKnownRecordsMessage;
import org.onebusaway.nyc.report_archive.model.ArchivedInferredLocationRecord;
import org.onebusaway.nyc.report_archive.queue.ArchivingInferenceQueueListenerTask;
import org.onebusaway.nyc.report_archive.services.NycQueuedInferredLocationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.spring.Autowire;

@Path("/archive/lastknown")
@Autowire
@Component
public class LastKnownLocationResource {
  
  private static Logger _log = LoggerFactory.getLogger(ArchivingInferenceQueueListenerTask.class);

  @Autowired
  private NycQueuedInferredLocationDao _locationDao;
  
  @Autowired
  private JsonTool _jsonTool;
  
  @Path("/all")
  @GET
  @Produces("application/json")
  public Response getAllLastLocationRecords() {
    
    // TODO: Build List<ArchivedInferredLocationRecord> here
    List<ArchivedInferredLocationRecord> lastKnownRecords = _locationDao.getAllLastKnownRecords();
    
    // now set that list 
    LastKnownRecordsMessage message = new LastKnownRecordsMessage();
    message.setRecords(lastKnownRecords);
    message.setStatus("OK");
    
    String outputJson = null;
    
    try {
      StringWriter writer = new StringWriter();
      
      _jsonTool.writeJson(writer, message);
      
      outputJson = writer.toString();
      
      writer.close();
      
      if (outputJson == null) throw new IOException("After calling writeJson, outputJson is still null.");
    } catch (IOException e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
    
    Response response = Response.ok(outputJson, "application/json").build();
    
    return response;
  }
}
