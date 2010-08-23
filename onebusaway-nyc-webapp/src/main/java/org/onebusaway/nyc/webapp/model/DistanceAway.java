package org.onebusaway.nyc.webapp.model;

/**
 * Data transfer object for how far away a vehicle is
 */
public class DistanceAway {

  private final int stopsAway;
  private final int feetAway;

  public DistanceAway(int stopsAway, int feetAway) {
    this.stopsAway = stopsAway;
    this.feetAway = feetAway;
  }

  public int getStops() {
    return stopsAway;
  }

  public int getFeet() {
    return feetAway;
  }

}
