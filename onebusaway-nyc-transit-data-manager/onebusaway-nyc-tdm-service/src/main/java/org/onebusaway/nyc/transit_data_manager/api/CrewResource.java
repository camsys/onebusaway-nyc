package org.onebusaway.nyc.transit_data_manager.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
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
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.UtsCrewAssignsToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.data.OperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.OperatorAssignmentFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.OperatorAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.OperatorAssignmentsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import tcip_final_3_0_5_1.SCHOperatorAssignment;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.jersey.api.spring.Autowire;

@Path("/crew/{serviceDate}/list")
@Component
@Scope("request")
@Autowire
public class CrewResource {

  private static Logger _log = LoggerFactory.getLogger(CrewResource.class);

  @GET
  @Produces("application/json")
  public Response getCrewAssignments(
      @PathParam("serviceDate") String inputDateStr) {

    Response response = null;

    DateTimeFormatter dateDTF = ISODateTimeFormat.date();

    DateMidnight serviceDate = null;

    try {
      serviceDate = new DateMidnight(dateDTF.parseDateTime(inputDateStr));
    } catch (IllegalArgumentException e) {
      _log.debug(e.getMessage());
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    }

    File inputFile = new File(System.getProperty("tdm.dataPath")
        + System.getProperty("tdm.crewAssignFilename"));

    // First create a OperatorAssignmentData object
    UtsCrewAssignsToDataCreator process = new UtsCrewAssignsToDataCreator(
        inputFile);

    OperatorAssignmentData data;
    try {
      data = process.generateDataObject();
    } catch (IOException e1) {
      _log.info("Exception loading data. Verify input file " + inputFile.toString());
      _log.debug(e1.getMessage());
      throw new WebApplicationException(e1,
          Response.Status.INTERNAL_SERVER_ERROR);
    }

    List<OperatorAssignment> jsonOpAssigns = null;

    ModelCounterpartConverter<SCHOperatorAssignment, OperatorAssignment> tcipToJsonConverter = new OperatorAssignmentFromTcip();

    jsonOpAssigns = listConvertOpAssignTcipToJson(tcipToJsonConverter,
        data.getOperatorAssignmentsByServiceDate(serviceDate)); // grab the
                                                                // assigns for
                                                                // this date
                                                                // and
                                                                // convert to
                                                                // json

    OperatorAssignmentsMessage opAssignMessage = new OperatorAssignmentsMessage();
    opAssignMessage.setCrew(jsonOpAssigns);
    opAssignMessage.setStatus("OK");

    // Then I need to write it as json output.
    Gson gson = null;
    if (Boolean.parseBoolean(System.getProperty("tdm.prettyPrintOutput"))) {
      gson = new GsonBuilder().setFieldNamingPolicy(
          FieldNamingPolicy.LOWER_CASE_WITH_DASHES).setPrettyPrinting().create();
    } else {
      gson = new GsonBuilder().setFieldNamingPolicy(
          FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();
    }

    String output = gson.toJson(opAssignMessage);

    response = Response.ok(output).build();

    return response;

  }

  /*
   * This method also exists in the same form in
   * UtsCrewAssignsToJsonOutputProcess
   */
  private List<OperatorAssignment> listConvertOpAssignTcipToJson(
      ModelCounterpartConverter<SCHOperatorAssignment, OperatorAssignment> conv,
      List<SCHOperatorAssignment> inputAssigns) {
    List<OperatorAssignment> outputAssigns = new ArrayList<OperatorAssignment>();

    Iterator<SCHOperatorAssignment> assignTcipIt = inputAssigns.iterator();

    while (assignTcipIt.hasNext()) {
      outputAssigns.add(conv.convert(assignTcipIt.next()));
    }

    return outputAssigns;
  }

}
