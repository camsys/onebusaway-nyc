package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.OperatorAssignment;

public class OperatorAssignmentsMessage {
  private List<OperatorAssignment> crew;
  private String status;

  public void setCrew(List<OperatorAssignment> crew) {
    this.crew = crew;
  }

  public void setStatus(String status) {
    this.status = status;
  }

}
