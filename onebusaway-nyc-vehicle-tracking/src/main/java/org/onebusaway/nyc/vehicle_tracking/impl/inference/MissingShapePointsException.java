package org.onebusaway.nyc.vehicle_tracking.impl.inference;

public class MissingShapePointsException extends Exception {

  private static final long serialVersionUID = -767038558128080637L;

  public MissingShapePointsException(String message) {
    super(message);
  }
}
