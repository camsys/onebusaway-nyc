package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.transit_data_manager.adapters.ModelCounterpartConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterBusDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.VehicleDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.input.BusDepotAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.BusDepotAssignsToDepotMapTranslator;
import org.onebusaway.nyc.transit_data_manager.adapters.input.GroupByPropInListObjectTranslator;
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPBusDepotAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.BusDepotAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.XMLBusDepotAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.output.json.VehicleFromTcip;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Depot;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.DepotVehicleAssignments;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message.ListDepotVehicleAssignmentsMessage;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;
import tcip_final_3_0_5_1.CPTVehicleIden;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class MtaBusDepotsToJsonOutputProcess extends FileToFileConverterProcess {

  public MtaBusDepotsToJsonOutputProcess(File inputFile, File outputFile) {
    super(inputFile, outputFile);
  }

  public void executeProcess() throws FileNotFoundException {
    FileReader inputFileReader = new FileReader(inputFile);

    BusDepotAssignsInputConverter inConv = new XMLBusDepotAssignsInputConverter(
        inputFileReader);

    List<MtaBusDepotAssignment> assignments = inConv.getBusDepotAssignments();

    System.out.println("ran getVehicleAssignments and got "
        + assignments.size() + " results");

    // With Bus Depot Assignments we need to group MtaBusDepotAssignment s by
    // the depot field.
    // I'll add another class to do that.
    GroupByPropInListObjectTranslator<List<MtaBusDepotAssignment>, Map<String, List<MtaBusDepotAssignment>>> translator = new BusDepotAssignsToDepotMapTranslator();
    // Create a map to map depot codes to lists of MtaBusDepotAssignment s
    // corresponding to that depot.
    Map<String, List<MtaBusDepotAssignment>> depotBusesMap = translator.restructure(assignments);

    BusDepotAssignmentsOutputConverter converter = new TCIPBusDepotAssignmentsOutputConverter(
        depotBusesMap);
    List<CPTFleetSubsetGroup> fleetSSGroups = converter.convertAssignments();

    // At this point I've got a list of CPTFleetSubsetGroup, so create the data
    // object to hold/manage it.
    VehicleDepotData data = new ImporterBusDepotData(fleetSSGroups);

    // Now I'll loop a couple times again to first go through the list of all
    // depots, then for each depot, through the vehicles.
    // Converting them one by one.

    ModelCounterpartConverter<CPTVehicleIden, Vehicle> toJsonModelConv = new VehicleFromTcip();

    // First iterate through all the depots.

    Iterator<String> depNamesIt = data.getAllDepotNames().iterator(); // depNamesIt
                                                                      // is the
                                                                      // iterator
                                                                      // to
                                                                      // iterate
                                                                      // through
                                                                      // each
                                                                      // depot
                                                                      // name
                                                                      // string.
    String depName = ""; // Holds a single depot name string.
    List<CPTVehicleIden> vehiclesTcip = null; // Holds the TCIP representation
                                              // of all vehicles at a single
                                              // depot.
    List<Vehicle> vehiclesJson = null; // Holds the JSON model representation of
                                       // all vehicles at a single depot.

    List<DepotVehicleAssignments> depotAssignments = new ArrayList<DepotVehicleAssignments>();
    Depot depot = null;
    DepotVehicleAssignments depotAssign = null;

    while (depNamesIt.hasNext()) {
      depName = depNamesIt.next();

      depot = new Depot();
      depot.setName(depName);

      vehiclesTcip = data.getVehiclesByDepotNameStr(depName); // get all the
                                                              // vehicles for
                                                              // this depot.
      vehiclesJson = new ArrayList<Vehicle>();

      // now iterate through all the vehicles at that depot, converting each
      // vehicle to its json model representation.
      Iterator<CPTVehicleIden> vehIt = vehiclesTcip.iterator();
      while (vehIt.hasNext()) {
        vehiclesJson.add(toJsonModelConv.convert(vehIt.next()));
      }

      depotAssign = new DepotVehicleAssignments();
      depotAssign.setDepot(depot);
      depotAssign.setVehicles(vehiclesJson);

      depotAssignments.add(depotAssign);
    }

    ListDepotVehicleAssignmentsMessage allAssigns = new ListDepotVehicleAssignmentsMessage();
    allAssigns.setAssignments(depotAssignments);
    allAssigns.setStatus("OK");

    Gson gson = new GsonBuilder().setFieldNamingPolicy(
        FieldNamingPolicy.LOWER_CASE_WITH_DASHES).setPrettyPrinting().create();
    output = gson.toJson(allAssigns);
  }

}
