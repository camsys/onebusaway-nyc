package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.ServiceDateOperatorAssignments;

/**
 * A model object representing a message consisting of complete service date
 * assignments. Also a status field. For use with Gson to make JSON.
 * 
 * @author sclark
 * 
 */
public class ListServiceDateOperatorAssignmentsMessage {
  public ListServiceDateOperatorAssignmentsMessage() {
  }

  private List<ServiceDateOperatorAssignments> assignments;
  private String status;

  public void setAssignments(List<ServiceDateOperatorAssignments> assignments) {
    this.assignments = assignments;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
