package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json;

import java.util.List;

/**
 * Model class representing all the operator assignments for a certain service
 * date. For use with Gson to make JSON.
 * 
 * @author sclark
 * 
 */
public class ServiceDateOperatorAssignments {
  private String serviceDate;
  private List<OperatorAssignment> crew;

  public void setServiceDate(String serviceDate) {
    this.serviceDate = serviceDate;
  }

  public void setCrew(List<OperatorAssignment> crew) {
    this.crew = crew;
  }
}
