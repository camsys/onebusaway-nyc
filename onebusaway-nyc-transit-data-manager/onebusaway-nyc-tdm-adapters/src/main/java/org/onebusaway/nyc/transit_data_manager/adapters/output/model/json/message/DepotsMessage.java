package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message;

import java.util.List;

/**
 * A model object representing a message consisting of a set of
 * VehicleDepotAssignments. For use in making JSon with Gson.
 * 
 * @author sclark
 * 
 */
public class DepotsMessage {
  public DepotsMessage() {
    super();
  }

  private List<String> depots;
  private String status;

  public void setDepots(List<String> depots) {
    this.depots = depots;
  }

  public void setStatus(String status) {
    this.status = status;
  }

}
