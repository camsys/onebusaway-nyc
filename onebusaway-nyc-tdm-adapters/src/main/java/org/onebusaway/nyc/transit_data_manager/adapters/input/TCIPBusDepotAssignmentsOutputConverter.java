package org.onebusaway.nyc.transit_data_manager.adapters.input;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;

import tcip_final_4_0_0_0.CPTFleetSubsetGroup;

public class TCIPBusDepotAssignmentsOutputConverter implements
    BusDepotAssignmentsOutputConverter {

  private Map<String, List<MtaBusDepotAssignment>> mtaDepotToBusesAtDepotMap = null;
  private DepotIdTranslator depotIdTranslator;

  public TCIPBusDepotAssignmentsOutputConverter(
      Map<String, List<MtaBusDepotAssignment>> data) {
    mtaDepotToBusesAtDepotMap = data;
  }

  public List<CPTFleetSubsetGroup> convertAssignments() {

    MtaDepotMapToTcipAssignmentConverter dataConverter = new MtaDepotMapToTcipAssignmentConverter();
    dataConverter.setDepotIdTranslator(depotIdTranslator);

    List<CPTFleetSubsetGroup> assigns = new ArrayList<CPTFleetSubsetGroup>();

    CPTFleetSubsetGroup opAssign = null;
    String mtaDepotStr = null;
    
    Iterator<String> mtaDepotItr = mtaDepotToBusesAtDepotMap.keySet().iterator();

    while (mtaDepotItr.hasNext()) {
      mtaDepotStr = mtaDepotItr.next();
      // Note that in dataConverter, the depot ids will get translated. I will stick with the source IDs here
      // So that all the translation happens in the same class.
      opAssign = dataConverter.ConvertToOutput(mtaDepotStr, mtaDepotToBusesAtDepotMap.get(mtaDepotStr));
      assigns.add(opAssign);
    }
    
    
    return assigns;
  }

  public void setDepotIdTranslator(DepotIdTranslator depotIdTranslator) {
    this.depotIdTranslator = depotIdTranslator;
  }
}
