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

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.MtaBusDepotFileToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.data.VehicleDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.VehicleFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.VehiclesMessage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import tcip_final_3_0_5_1.CPTVehicleIden;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Path("/depot/{depotName}/vehicles/list")
@Component
@Scope("request")
public class DepotResource {

  @GET
  @Produces("application/json")
  public String getDepotAssignments(@PathParam("depotName")
  String depotName) {

    File inputFile = new File(System.getProperty("tdm.dataPath")
        + System.getProperty("tdm.depotAssignFilename"));

    VehicleDepotData data = null;

    // First I need to create a VehicleDepotData object from the input file.
    MtaBusDepotFileToDataCreator process = new MtaBusDepotFileToDataCreator(
        inputFile);

    try {
      data = process.generateDataObject();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      return e.getMessage();
    }

    // Then I need to get the data for the input depot
    List<CPTVehicleIden> depotVehicles = data.getVehiclesByDepotNameStr(depotName);

    // Now convert the data to my json model.
    ModelCounterpartConverter<CPTVehicleIden, Vehicle> toJsonModelConv = new VehicleFromTcip();

    List<Vehicle> depotVehiclesJson = new ArrayList<Vehicle>();

    // now iterate through all the vehicles at that depot, converting each
    // vehicle to its json model representation.
    Iterator<CPTVehicleIden> vehIt = depotVehicles.iterator();
    while (vehIt.hasNext()) {
      depotVehiclesJson.add(toJsonModelConv.convert(vehIt.next()));
    }

    // Now add it to a message object
    VehiclesMessage message = new VehiclesMessage();
    message.setVehicles(depotVehiclesJson);
    message.setStatus("OK");

    // Then I need to write it as json output.
    Gson gson = null;
    if (Boolean.parseBoolean(System.getProperty("tdm.prettyPrintOutput"))) {
      gson = new GsonBuilder().setFieldNamingPolicy(
          FieldNamingPolicy.LOWER_CASE_WITH_DASHES).setPrettyPrinting().create();
    } else {
      gson = new GsonBuilder().setFieldNamingPolicy(
          FieldNamingPolicy.LOWER_CASE_WITH_DASHES).create();
    }

    String output = gson.toJson(message);

    return output;

  }
}
