package org.onebusaway.nyc.transit_data_manager.api;

import java.io.File;
import java.io.IOException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.CsvCrewAssignsToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.data.SignCodeData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.SignMessageFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.DestinationSign;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.DestinationSignMessage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import tcip_final_3_0_5_1.CCDestinationSignMessage;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Path("/dsc/{code}/sign")
@Component
@Scope("request")
public class DscResource {

  @GET
  @Produces("application/json")
  public String getDisplayForCode(@PathParam("code")
  Long dscCode) {

    File inputFile = new File(System.getProperty("tdm.dataPath")
        + System.getProperty("tdm.dscFilename"));

    // Need to create a data object to be our interface to the TCIP sign code
    // data.
    SignCodeData data = null;

    // Create an object to represent the data.
    CsvCrewAssignsToDataCreator process = new CsvCrewAssignsToDataCreator(
        inputFile);

    try {
      data = process.generateDataObject();
    } catch (IOException e) {
      return e.getMessage();
    }

    CCDestinationSignMessage messageTcip = data.getDisplayForCode(dscCode);

    ModelCounterpartConverter<CCDestinationSignMessage, DestinationSign> tcipToJsonConverter = new SignMessageFromTcip();

    DestinationSign messageJson = tcipToJsonConverter.convert(messageTcip);

    DestinationSignMessage outputMessage = new DestinationSignMessage();
    outputMessage.setSign(messageJson);
    outputMessage.setStatus("OK");

    // Then I need to write it as json output.
    Gson gson = null;
    if (Boolean.parseBoolean(System.getProperty("tdm.prettyPrintOutput"))) {
      gson = new GsonBuilder().setFieldNamingPolicy(
          FieldNamingPolicy.LOWER_CASE_WITH_DASHES).setPrettyPrinting().create();
    } else {
      gson = new GsonBuilder().setFieldNamingPolicy(
          FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();
    }

    String output = gson.toJson(outputMessage);

    return output;
  }
}
