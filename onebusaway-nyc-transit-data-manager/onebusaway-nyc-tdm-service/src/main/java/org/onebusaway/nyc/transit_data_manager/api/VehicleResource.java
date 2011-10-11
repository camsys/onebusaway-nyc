package org.onebusaway.nyc.transit_data_manager.api;

import org.onebusaway.nyc.transit_data_manager.adapters.api.processes.MtaBusDepotFileToDataCreator;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.VehiclesMessage;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/vehicles/list")
@Component
@Scope("request")
public class VehicleResource {

  @GET
  @Produces("application/json")
  public String getVehicles() throws IOException {

    MtaBusDepotFileToDataCreator process = getDataCreator();
    List<MtaBusDepotAssignment> assignments = process.loadDepotAssignments();

    List<Vehicle> vehicles = new ArrayList<Vehicle>();
    
    for (MtaBusDepotAssignment assignment: assignments) {
      Vehicle v = new Vehicle();
      v.setAgencyId(assignment.getAgencyId().toString());
      v.setVehicleId(Integer.toString(assignment.getBusNumber()));
      v.setDepotId(assignment.getDepot());
      vehicles.add(v);
    }

    // Now add it to a message object
    VehiclesMessage message = new VehiclesMessage();
    message.setVehicles(vehicles);
    message.setStatus("OK");

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

  public MtaBusDepotFileToDataCreator getDataCreator()
      throws FileNotFoundException {
    File inputFile = new File(System.getProperty("tdm.dataPath")
        + System.getProperty("tdm.depotAssignFilename"));

    MtaBusDepotFileToDataCreator process = new MtaBusDepotFileToDataCreator();
    process.setReader(new FileReader(inputFile));
    return process;
  }

}
