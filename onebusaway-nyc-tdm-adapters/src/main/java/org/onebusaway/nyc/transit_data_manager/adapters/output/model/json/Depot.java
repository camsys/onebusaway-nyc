package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json;

/**
 * Model class representing a bus depot/garage, which is basically just the name
 * of the depot, for conversion to JSON with Gson.
 * 
 * @author sclark
 * 
 */
public class Depot {
  public Depot() {

  }

  private String name;

  public void setName(String name) {
    this.name = name;
  }
}
