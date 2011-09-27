package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json;

import java.util.List;

/**
 * A model object representing a single depot and all of the buses assigned to
 * it. For use with Gson to make Json.
 * 
 * @author sclark
 * 
 */
public class DepotVehicleAssignments {
  public DepotVehicleAssignments() {

  }

  private Depot depot;
  private List<Vehicle> vehicles;

  public void setDepot(Depot depot) {
    this.depot = depot;
  }

  public void setVehicles(List<Vehicle> vehicles) {
    this.vehicles = vehicles;
  }

}
