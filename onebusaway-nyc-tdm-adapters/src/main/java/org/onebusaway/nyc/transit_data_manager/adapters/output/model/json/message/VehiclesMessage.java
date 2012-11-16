package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.DepotVehicleAssignments;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.Vehicle;

/**
 * A model object representing a message consisting of a set of
 * VehicleDepotAssignments. For use in making JSon with Gson.
 * 
 * @author sclark
 * 
 */
public class VehiclesMessage {
  public VehiclesMessage() {

  }

  private List<Vehicle> vehicles;
  private String status;

  public void setVehicles(List<Vehicle> vehicles) {
    this.vehicles = vehicles;
  }

  public void setStatus(String status) {
    this.status = status;
  }

}
