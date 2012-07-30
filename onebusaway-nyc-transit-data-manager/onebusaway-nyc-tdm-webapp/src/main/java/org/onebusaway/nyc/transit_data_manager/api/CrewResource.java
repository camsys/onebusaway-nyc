package org.onebusaway.nyc.transit_data_manager.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.joda.time.DateMidnight;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UTSUtil;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UtsCrewAssignsToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.data.OperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.OperatorAssignmentFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.OperatorAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.OperatorAssignmentsMessage;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.onebusaway.nyc.transit_data_manager.api.sourceData.MostRecentFilePicker;
import org.onebusaway.nyc.transit_data_manager.api.sourceData.UtsCrewUploadsFilePicker;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import tcip_final_3_0_5_1.SCHOperatorAssignment;

import com.sun.jersey.api.spring.Autowire;

@Path("/crew/{serviceDate}/list")
@Component
@Scope("request")
@Autowire
public class CrewResource {

  public CrewResource() throws IOException {
    if (System.getProperty("tdm.crewAssignsUploadDir") != null) {
      mostRecentPicker = new UtsCrewUploadsFilePicker(System.getProperty("tdm.crewAssignsUploadDir"));
    }
    
    try {
      if (System.getProperty("tdm.depotIdTranslationFile") != null) {
        depotIdTranslator = new DepotIdTranslator(new File(System.getProperty("tdm.depotIdTranslationFile")));
      }
    } catch (IOException e) {
      // Set depotIdTranslator to null and otherwise do nothing.
      // Everything works fine without the depot id translator.
      depotIdTranslator = null;
    }
  }
  
  private static Logger _log = LoggerFactory.getLogger(CrewResource.class);

  @Autowired
  JsonTool jsonTool;
  
  
  private DepotIdTranslator depotIdTranslator = null;
  
  private MostRecentFilePicker mostRecentPicker;
  
  public void setJsonTool(JsonTool jsonTool) {
    this.jsonTool = jsonTool;
  }
  
  @GET
  @Produces("application/json")
  public Response getCrewAssignments(
      @PathParam("serviceDate") String inputDateStr) {

    _log.info("Starting getCrewAssignments for input date " + inputDateStr);
    
    DateTimeFormatter dateDTF = ISODateTimeFormat.date();

    DateMidnight serviceDate = null;

    try {
      serviceDate = new DateMidnight(dateDTF.parseDateTime(inputDateStr));
    } catch (IllegalArgumentException e) {
      _log.debug(e.getMessage());
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    }

    File inputFile = mostRecentPicker.getMostRecentSourceFile();
    
    if (inputFile == null) {
      throw new WebApplicationException(new IOException("No Source file found."), Response.Status.INTERNAL_SERVER_ERROR);
    }

    _log.debug("Generating OperatorAssignmentData object from file " + inputFile.getPath());
    // First create a OperatorAssignmentData object
    
    OperatorAssignmentData data;
    
    try {
      data = getOpAssignDataObjectForFile(inputFile);
    } catch (IOException e1) {
      _log.info("Exception loading data. Verify input file " + inputFile.toString());
      _log.debug(e1.getMessage());
      throw new WebApplicationException(e1,
          Response.Status.INTERNAL_SERVER_ERROR);
    }

    ModelCounterpartConverter<SCHOperatorAssignment, OperatorAssignment> tcipToJsonConverter = new OperatorAssignmentFromTcip();

    List<OperatorAssignment> jsonOpAssigns = new UTSUtil().listConvertOpAssignTcipToJson(tcipToJsonConverter,
        data.getOperatorAssignmentsByServiceDate(serviceDate)); // grab the
                                                                // assigns for
                                                                // this date
                                                                // and
                                                                // convert to
                                                                // json

    OperatorAssignmentsMessage opAssignMessage = new OperatorAssignmentsMessage();
    opAssignMessage.setCrew(jsonOpAssigns);
    opAssignMessage.setStatus("OK");

    StringWriter writer = null;
    String output = null;
    try {
      writer = new StringWriter();
      jsonTool.writeJson(writer, opAssignMessage);
      output = writer.toString();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } finally {
      try {
        writer.close();
      } catch (IOException e) {}
    }
    
    Response response = Response.ok(output).build();

    _log.info("Returning response ok.");
    
    return response;

  }

  private OperatorAssignmentData getOpAssignDataObjectForFile(File inputFile) throws FileNotFoundException {
    UtsCrewAssignsToDataCreator process = new UtsCrewAssignsToDataCreator(
        inputFile);
    
    // If depotIdTranslator is null, print a message saying the depot id translation tool is not set up.
    if (depotIdTranslator == null) {
      _log.info("DepotIdTranslator has not been set up. Depot IDs will not be translated.");
    } else {
      _log.info("Using depot ID translation.");
    }
    process.setDepotIdTranslator(depotIdTranslator);
    
    return process.generateDataObject();
  }


}
