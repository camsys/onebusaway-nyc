package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterBusDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterOperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.OperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.VehicleDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.input.BusDepotAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.BusDepotAssignsToDepotMapTranslator;
import org.onebusaway.nyc.transit_data_manager.adapters.input.CrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.GroupByPropInListObjectTranslator;
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPBusDepotAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPCrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsCrewAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.BusDepotAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CSVCrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.XMLBusDepotAssignsInputConverter;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;
import tcip_final_3_0_5_1.SCHOperatorAssignment;

public class UtsCrewAssignsToDataCreator {
  private File inputFile;

  public UtsCrewAssignsToDataCreator(File inputFile) {
    this.inputFile = inputFile;
  }

  public OperatorAssignmentData generateDataObject() throws IOException {
    FileReader inputFileReader = new FileReader(inputFile);

    CrewAssignsInputConverter inConv = new CSVCrewAssignsInputConverter(
        inputFileReader);

    List<MtaUtsCrewAssignment> crewAssignments = inConv.getCrewAssignments();

    inputFileReader.close();

    CrewAssignmentsOutputConverter converter = new TCIPCrewAssignmentsOutputConverter(
        crewAssignments);
    List<SCHOperatorAssignment> opAssignments = converter.convertAssignments();

    // Set up a data object to interface with the tcip data.
    OperatorAssignmentData data = new ImporterOperatorAssignmentData(
        opAssignments); // a data object to represent the data.

    return data;
  }
}
