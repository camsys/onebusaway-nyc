package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.transit_data_federation.impl.ProjectedPointFactory;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;

public class Observation {

  private final NycVehicleLocationRecord _record;

  private final ProjectedPoint _point;
  
  private final String _lastValidDestinationSignCode;

  private Observation _previousObservation;

  public Observation(NycVehicleLocationRecord record, String lastValidDestinationSignCode) {
    this(record, lastValidDestinationSignCode, null);
  }

  public Observation(NycVehicleLocationRecord record,
      String lastValidDestinationSignCode, Observation previousObservation) {
    _record = record;
    _point = ProjectedPointFactory.forward(record.getLatitude(),
        record.getLongitude());
    _previousObservation = previousObservation;
    _lastValidDestinationSignCode = lastValidDestinationSignCode;
  }

  public long getTime() {
    return _record.getTime();
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
