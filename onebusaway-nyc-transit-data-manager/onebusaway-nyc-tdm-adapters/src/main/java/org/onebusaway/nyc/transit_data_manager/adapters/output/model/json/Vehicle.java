package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json;

/**
 * A model object representing a vehicle (most likely a bus!) For use with Gson
 * to make Json.
 * 
 * @author sclark
 * 
 */
public class Vehicle {
  public Vehicle() {

  }

  private String agencyId;
  private String vehicleId;
  private String depotId;

  public void setAgencyId(String agencyId) {
    this.agencyId = agencyId;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public void setDepotId(String depot) {
    this.depotId = depot;
  }

}
