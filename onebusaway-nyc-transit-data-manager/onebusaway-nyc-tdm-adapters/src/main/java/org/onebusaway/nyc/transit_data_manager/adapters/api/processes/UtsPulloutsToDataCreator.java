package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterBusDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterOperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterVehiclePulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.OperatorAssignmentData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.PulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.VehicleDepotData;
import org.onebusaway.nyc.transit_data_manager.adapters.input.BusDepotAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.BusDepotAssignsToDepotMapTranslator;
import org.onebusaway.nyc.transit_data_manager.adapters.input.CrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.GroupByPropInListObjectTranslator;
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPBusDepotAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPCrewAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPVehicleAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.VehicleAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsCrewAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsVehiclePullInPullOut;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.BusDepotAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CSVCrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CSVVehicleAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CrewAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.VehicleAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.XMLBusDepotAssignsInputConverter;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;
import tcip_final_3_0_5_1.SCHOperatorAssignment;
import tcip_final_3_0_5_1.SCHPullInOutInfo;

public class UtsPulloutsToDataCreator {
  private File inputFile;

  public UtsPulloutsToDataCreator(File inputFile) {
    this.inputFile = inputFile;
  }

  public PulloutData generateDataObject() throws IOException {
    FileReader inputFileReader = new FileReader(inputFile);

    VehicleAssignsInputConverter inConv = new CSVVehicleAssignsInputConverter(
        inputFileReader);

    List<MtaUtsVehiclePullInPullOut> vehPullouts = inConv.getVehicleAssignments();

    inputFileReader.close();

    VehicleAssignmentsOutputConverter converter = new TCIPVehicleAssignmentsOutputConverter(
        vehPullouts);

    List<SCHPullInOutInfo> tcipPullouts = converter.convertAssignments();

    PulloutData data = new ImporterVehiclePulloutData(tcipPullouts);

    return data;
  }
}
