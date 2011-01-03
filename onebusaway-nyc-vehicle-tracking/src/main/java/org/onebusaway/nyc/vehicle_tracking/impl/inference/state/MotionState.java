package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import org.onebusaway.geospatial.model.CoordinatePoint;

public final class MotionState {

  private final long lastInMotionTime;

  private final CoordinatePoint lastInMotionLocation;

  private final boolean atBase;

  private final boolean atTerminal;

  public MotionState(long lastInMotionTime,
      CoordinatePoint lastInMotionLocation, boolean atBase, boolean atTerminal) {
    this.lastInMotionTime = lastInMotionTime;
    this.lastInMotionLocation = lastInMotionLocation;
    this.atBase = atBase;
    this.atTerminal = atTerminal;
  }

  public long getLastInMotionTime() {
    return lastInMotionTime;
  }

  public CoordinatePoint getLastInMotionLocation() {
    return lastInMotionLocation;
  }

  public boolean isAtBase() {
    return atBase;
  }

  public boolean isAtTerminal() {
    return atTerminal;
  }
}
