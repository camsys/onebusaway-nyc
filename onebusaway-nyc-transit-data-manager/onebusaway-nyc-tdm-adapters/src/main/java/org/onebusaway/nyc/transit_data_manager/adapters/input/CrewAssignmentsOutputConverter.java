package org.onebusaway.nyc.transit_data_manager.adapters.input;

import java.util.List;

import tcip_final_3_0_5_1.SCHOperatorAssignment;

public interface CrewAssignmentsOutputConverter {

  public List<SCHOperatorAssignment> convertAssignments();
}
