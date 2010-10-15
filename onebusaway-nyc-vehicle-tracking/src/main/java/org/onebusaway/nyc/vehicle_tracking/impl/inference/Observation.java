package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.transit_data_federation.impl.ProjectedPointFactory;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;

public class Observation {

  private final NycVehicleLocationRecord _record;

  private final ProjectedPoint _point;

  private NycVehicleLocationRecord _previousRecord;

  public Observation(NycVehicleLocationRecord record) {
    this(record, null);
  }

  public Observation(NycVehicleLocationRecord record,
      NycVehicleLocationRecord previousRecord) {
    _record = record;
    _point = ProjectedPointFactory.forward(record.getLatitude(),
        record.getLongitude());
    _previousRecord = previousRecord;
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

  public CoordinatePoint getLocation() {
    return _point.toCoordinatePoint();
  }

  public NycVehicleLocationRecord getPreviousRecord() {
    return _previousRecord;
  }
}
