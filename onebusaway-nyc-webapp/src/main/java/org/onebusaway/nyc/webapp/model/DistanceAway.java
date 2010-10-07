package org.onebusaway.nyc.webapp.model;

import org.onebusaway.nyc.webapp.impl.DistancePresenter;

/**
 * Data transfer object for how far away a vehicle is
 */
public class DistanceAway implements Comparable<DistanceAway> {

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
  
  public String getPresentableDistance() {
    return DistancePresenter.displayFeet(feetAway) + " " +
           DistancePresenter.displayStopsAway(stopsAway);
  }

  @Override
  public int hashCode() {
    return 31*feetAway + 31*stopsAway;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DistanceAway))
      return false;
    DistanceAway rhs = (DistanceAway) obj;
    if (feetAway != rhs.feetAway)
      return false;
    if (stopsAway != rhs.stopsAway)
      return false;
    return true;
  }

  @Override
  public int compareTo(DistanceAway rhs) {
    if (feetAway < rhs.feetAway) return -1;
    if (feetAway > rhs.feetAway) return 1;
    if (stopsAway < rhs.stopsAway) return -1;
    if (stopsAway > rhs.stopsAway) return 1;
    return 0;
  }
}
