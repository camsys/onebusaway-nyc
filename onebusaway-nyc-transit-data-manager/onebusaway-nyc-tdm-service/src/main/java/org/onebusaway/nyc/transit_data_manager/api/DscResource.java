package org.onebusaway.nyc.transit_data_manager.api;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.CsvCrewAssignsToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.data.SignCodeData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.SignMessageFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.DestinationSign;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.DestinationSignMessage;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.DestinationSignsMessage;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import tcip_final_3_0_5_1.CCDestinationSignMessage;

@Path("/dsc")
@Component
@Scope("request")
public class DscResource {
  
  //private static Logger _log = LoggerFactory.getLogger(DscResource.class);
  
  @Autowired
  private JsonTool jsonTool;
  
  public void setJsonTool(JsonTool tool) {
    jsonTool = tool;
  }

  @Path("/{code}/sign")
  @GET
  @Produces("application/json")
  public String getDisplayForCode(@PathParam("code")
  Long dscCode) {

    //   Need to create a data object to be our interface to the TCIP sign code
    // data.
    SignCodeData data = null;
    try {
      data = getDataObject();
    } catch (IOException e1) {
      throw new WebApplicationException(e1, Response.Status.INTERNAL_SERVER_ERROR);
    }
    
    CCDestinationSignMessage messageTcip = data.getDisplayForCode(dscCode);

    ModelCounterpartConverter<CCDestinationSignMessage, DestinationSign> tcipToJsonConverter = new SignMessageFromTcip();

    DestinationSign messageJson = tcipToJsonConverter.convert(messageTcip);

    DestinationSignMessage outputMessage = new DestinationSignMessage();
    outputMessage.setSign(messageJson);
    outputMessage.setStatus("OK");

    String output = null;
    try {
      output = writeJsonObjectToString(outputMessage);
    } catch (IOException e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }

    return output;
  }
  
  @Path("/list")
  @GET
  @Produces("application/json")
  public String getAllDisplays() {
    SignCodeData data = null;
    try {
      data = getDataObject();
    } catch (IOException e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
    
    List<CCDestinationSignMessage> messages = data.getAllDisplays();
    
    ModelCounterpartConverter<CCDestinationSignMessage, DestinationSign> tcipToJsonConverter = new SignMessageFromTcip();
    
    List<DestinationSign> jsonSigns = new ArrayList<DestinationSign>();
    
    for (CCDestinationSignMessage message : messages) {
      jsonSigns.add(tcipToJsonConverter.convert(message));
    }
    
    DestinationSignsMessage outputMessage = new DestinationSignsMessage();
    outputMessage.setSigns(jsonSigns);
    outputMessage.setStatus("OK");
    
    String output = null;
    try {
      output = writeJsonObjectToString(outputMessage);
    } catch (IOException e) {
      throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
    }
    
    return output;
  }
  
  private SignCodeData getDataObject() throws IOException{
    File inputFile = new File(System.getProperty("tdm.dataPath")
        + System.getProperty("tdm.dscFilename"));
    
    CsvCrewAssignsToDataCreator process = new CsvCrewAssignsToDataCreator(
        inputFile);
    
    SignCodeData data = process.generateDataObject();
    
    return data;
  }
  
  private String writeJsonObjectToString (Object objectToWrite) throws IOException {
    StringWriter sWriter = new StringWriter();
    
    jsonTool.writeJson(sWriter, objectToWrite);
    
    String output = sWriter.toString();
    
    sWriter.close();
    
    return output;
  }
}
