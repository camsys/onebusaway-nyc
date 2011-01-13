package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import org.onebusaway.geospatial.model.CoordinatePoint;

public final class MotionState {

  private final long lastInMotionTime;

  private final CoordinatePoint lastInMotionLocation;

  public MotionState(long lastInMotionTime, CoordinatePoint lastInMotionLocation) {
    this.lastInMotionTime = lastInMotionTime;
    this.lastInMotionLocation = lastInMotionLocation;
  }

  public long getLastInMotionTime() {
    return lastInMotionTime;
  }

  public CoordinatePoint getLastInMotionLocation() {
    return lastInMotionLocation;
  }
}
