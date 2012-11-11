package org.onebusaway.nyc.transit_data_manager.adapters.input;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.tools.DepotIdTranslator;

import tcip_final_3_0_5_1.SCHOperatorAssignment;

public interface CrewAssignmentsOutputConverter {

  public List<SCHOperatorAssignment> convertAssignments();

  public void setDepotIdTranslator(DepotIdTranslator depotIdTranslator);
}
