package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.data.ImporterVehiclePulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.data.PulloutData;
import org.onebusaway.nyc.transit_data_manager.adapters.input.TCIPVehicleAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.VehicleAssignmentsOutputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsVehiclePullInPullOut;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.CSVVehicleAssignsInputConverter;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.UTSVehiclePullInOutDataUtility;
import org.onebusaway.nyc.transit_data_manager.adapters.input.readers.VehicleAssignsInputConverter;

import tcip_final_3_0_5_1.SCHPullInOutInfo;

public class UtsPulloutsToDataCreator {
  private File inputFile;

  public UtsPulloutsToDataCreator(File inputFile) {
    this.inputFile = inputFile;
  }

  public PulloutData generateDataObject() throws FileNotFoundException  {
    VehicleAssignsInputConverter inConv = new CSVVehicleAssignsInputConverter(
        inputFile);

    List<MtaUtsVehiclePullInPullOut> vehPullouts = inConv.getVehicleAssignments();
    
    vehPullouts = UTSVehiclePullInOutDataUtility.filterOutStrangeRows(vehPullouts);

    VehicleAssignmentsOutputConverter converter = new TCIPVehicleAssignmentsOutputConverter(
        vehPullouts);

    List<SCHPullInOutInfo> tcipPullouts = converter.convertAssignments();

    PulloutData data = new ImporterVehiclePulloutData(tcipPullouts);

    return data;
  }
}
