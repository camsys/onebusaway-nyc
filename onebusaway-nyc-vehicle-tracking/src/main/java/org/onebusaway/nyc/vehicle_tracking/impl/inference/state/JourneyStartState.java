package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import org.onebusaway.geospatial.model.CoordinatePoint;

public final class JourneyStartState {

  private final CoordinatePoint journeyStart;

  public JourneyStartState(CoordinatePoint journeyStart) {
    this.journeyStart = journeyStart;
  }

  public CoordinatePoint getJourneyStart() {
    return journeyStart;
  }

}
