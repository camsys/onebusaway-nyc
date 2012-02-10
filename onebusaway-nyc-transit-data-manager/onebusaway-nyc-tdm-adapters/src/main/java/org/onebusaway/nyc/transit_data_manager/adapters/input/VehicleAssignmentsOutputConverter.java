package org.onebusaway.nyc.transit_data_manager.adapters.input;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;

import tcip_final_3_0_5_1.SCHPullInOutInfo;

public interface VehicleAssignmentsOutputConverter {
  List<SCHPullInOutInfo> convertAssignments();

  void setDepotIdTranslator(DepotIdTranslator depotIdTranslator);
}
