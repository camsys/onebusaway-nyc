package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.DepotVehicleAssignments;

/**
 * A model object representing a message consisting of a set of
 * VehicleDepotAssignments. For use in making JSon with Gson.
 * 
 * @author sclark
 * 
 */
public class ListDepotVehicleAssignmentsMessage {
  public ListDepotVehicleAssignmentsMessage() {

  }

  private List<DepotVehicleAssignments> assignments;
  private String status;

  public void setAssignments(List<DepotVehicleAssignments> assignments) {
    this.assignments = assignments;
  }

  public void setStatus(String status) {
    this.status = status;
  }

}
