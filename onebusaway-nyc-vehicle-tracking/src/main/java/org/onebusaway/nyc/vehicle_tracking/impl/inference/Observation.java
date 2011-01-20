package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.transit_data_federation.impl.ProjectedPointFactory;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;

public class Observation {

  private final long _timestamp;

  private final NycVehicleLocationRecord _record;

  private final ProjectedPoint _point;

  private final String _lastValidDestinationSignCode;

  private final boolean atBase;

  private final boolean atTerminal;

  private final boolean outOfService;

  private Observation _previousObservation;

  public Observation(long timestamp, NycVehicleLocationRecord record,
      String lastValidDestinationSignCode, boolean atBase, boolean atTerminal,
      boolean outOfService, Observation previousObservation) {
    _timestamp = timestamp;
    _record = record;
    _point = ProjectedPointFactory.forward(record.getLatitude(),
        record.getLongitude());
    _lastValidDestinationSignCode = lastValidDestinationSignCode;
    this.atBase = atBase;
    this.atTerminal = atTerminal;
    this.outOfService = outOfService;
    _previousObservation = previousObservation;
  }

  public long getTime() {
    return _timestamp;
  }

  public NycVehicleLocationRecord getRecord() {
    return _record;
  }

  public ProjectedPoint getPoint() {
    return _point;
  }

  public String getLastValidDestinationSignCode() {
    return _lastValidDestinationSignCode;
  }

  public boolean isAtBase() {
    return atBase;
  }

  public boolean isAtTerminal() {
    return atTerminal;
  }

  public boolean isOutOfService() {
    return outOfService;
  }

  public CoordinatePoint getLocation() {
    return _point.toCoordinatePoint();
  }

  public Observation getPreviousObservation() {
    return _previousObservation;
  }

  public NycVehicleLocationRecord getPreviousRecord() {
    if (_previousObservation == null)
      return null;
    return _previousObservation.getRecord();
  }

  public void clearPreviousObservation() {
    _previousObservation = null;
  }

  @Override
  public String toString() {
    return _record.toString();
  }
}
