package org.onebusaway.nyc.transit_data_manager.importers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.transit_data_manager.model.MtaBusDepotAssignment;

import tcip_final_3_0_5_1.CPTFleetSubsetGroup;

public class TCIPBusDepotAssignmentsOutputConverter implements
    BusDepotAssignmentsOutputConverter {

  private Map<String, List<MtaBusDepotAssignment>> assignInputMap = null;

  public TCIPBusDepotAssignmentsOutputConverter(
      Map<String, List<MtaBusDepotAssignment>> data) {
    assignInputMap = data;
  }

  public List<CPTFleetSubsetGroup> convertAssignments() {
    // MtaUtsToTcipAssignmentConverter dataConverter = new
    // MtaUtsToTcipAssignmentConverter();
    MtaDepotMapToTcipAssignmentConverter dataConverter = new MtaDepotMapToTcipAssignmentConverter();

    List<CPTFleetSubsetGroup> assigns = new ArrayList<CPTFleetSubsetGroup>();

    Iterator<String> itr = assignInputMap.keySet().iterator();

    CPTFleetSubsetGroup opAssign = null;
    String depot = null;
    while (itr.hasNext()) {
      depot = itr.next();
      opAssign = dataConverter.ConvertToOutput(depot, assignInputMap.get(depot));
      assigns.add(opAssign);
    }

    return assigns;
  }
}
