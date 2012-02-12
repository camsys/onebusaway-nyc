package org.onebusaway.nyc.transit_data_manager.adapters.input;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsVehiclePullInPullOut;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;

import tcip_final_3_0_5_1.SCHPullInOutInfo;

public class TCIPVehicleAssignmentsOutputConverter implements
    VehicleAssignmentsOutputConverter {
  
  private DepotIdTranslator depotIdTranslator = null;

  private List<MtaUtsVehiclePullInPullOut> vehicleAssignInputData = null;

  public TCIPVehicleAssignmentsOutputConverter(
      List<MtaUtsVehiclePullInPullOut> data) {
    vehicleAssignInputData = data;
  }

  public List<SCHPullInOutInfo> convertAssignments() {

    MtaUtsToTcipVehicleAssignmentConverter dataConverter = new MtaUtsToTcipVehicleAssignmentConverter();
    dataConverter.setDepotIdTranslator(depotIdTranslator);

    List<SCHPullInOutInfo> vehAssigns = new ArrayList<SCHPullInOutInfo>();

    Iterator<MtaUtsVehiclePullInPullOut> itr = vehicleAssignInputData.iterator();

    SCHPullInOutInfo vehAssign = null;

    MtaUtsVehiclePullInPullOut input = null;

    while (itr.hasNext()) {
      input = itr.next();
      vehAssign = dataConverter.convertToPullOut(input);
      vehAssigns.add(vehAssign);
      vehAssign = dataConverter.convertToPullIn(input);
      vehAssigns.add(vehAssign);
    }

    return vehAssigns;
  }

  public void setDepotIdTranslator(DepotIdTranslator depotIdTranslator) {
    this.depotIdTranslator = depotIdTranslator;    
  }
}
