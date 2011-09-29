package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterBusDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.VehicleDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.input.BusDepotAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.BusDepotAssignsToDepotMapTranslator;
import org.onebusaway.nyc.transit_data_manager.adapters.input.GroupByPropInListObjectTranslator;
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPBusDepotAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.BusDepotAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.XMLBusDepotAssignsInputConverter;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;

public class MtaBusDepotFileToDataCreator {

  private File inputFile;

  public MtaBusDepotFileToDataCreator(File inputFile) {
    this.inputFile = inputFile;
  }

  public VehicleDepotData generateDataObject() throws IOException {
    FileReader inputFileReader = new FileReader(inputFile);

    BusDepotAssignsInputConverter inConv = new XMLBusDepotAssignsInputConverter(
        inputFileReader);

    List<MtaBusDepotAssignment> assignments = inConv.getBusDepotAssignments();

    inputFileReader.close();

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

    return data;
  }
}
