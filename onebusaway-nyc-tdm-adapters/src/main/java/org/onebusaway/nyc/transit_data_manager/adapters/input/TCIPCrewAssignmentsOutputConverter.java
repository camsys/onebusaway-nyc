package org.onebusaway.nyc.transit_data_manager.adapters.input;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaUtsCrewAssignment;
import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tcip_final_4_0_0.SCHOperatorAssignment;

public class TCIPCrewAssignmentsOutputConverter implements
    CrewAssignmentsOutputConverter {
  
  private static Logger _log = LoggerFactory.getLogger(TCIPCrewAssignmentsOutputConverter.class);

  private List<MtaUtsCrewAssignment> crewAssignInputData = null;

  private DepotIdTranslator depotIdTranslator;

  public TCIPCrewAssignmentsOutputConverter(List<MtaUtsCrewAssignment> data) {
    crewAssignInputData = data;
  }

  public List<SCHOperatorAssignment> convertAssignments() {
    MtaUtsToTcipAssignmentConverter dataConverter = new MtaUtsToTcipAssignmentConverter();
    dataConverter.setDepotIdTranslator(depotIdTranslator);
    
    List<SCHOperatorAssignment> opAssigns = new ArrayList<SCHOperatorAssignment>();

    _log.debug("About to convert " + crewAssignInputData.size() + " items from UTS input objects to TCIP SCHOperatorAssignment objects using MtaUtsToTcipAssignmentConverter.");
    
    for (MtaUtsCrewAssignment utsAssignment : crewAssignInputData) {
      SCHOperatorAssignment opAssign = dataConverter.ConvertToOutput(utsAssignment);
      if (opAssign != null) {
        opAssigns.add(opAssign);
      }
    }

    _log.debug("Done conversions, returning List<SCHOperatorAssignment>.");
    
    return opAssigns;
  }

  public void setDepotIdTranslator(DepotIdTranslator depotIdTranslator) {
    this.depotIdTranslator = depotIdTranslator;
    
  }
}
