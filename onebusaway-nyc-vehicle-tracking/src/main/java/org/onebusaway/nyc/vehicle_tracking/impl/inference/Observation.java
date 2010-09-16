package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.transit_data_federation.impl.ProjectedPointFactory;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;

public class Observation {

  private final NycVehicleLocationRecord _record;

  private final ProjectedPoint _point;

  private NycVehicleLocationRecord _previousRecord;

  public Observation(NycVehicleLocationRecord record) {
    this(record,null);
  }
  public Observation(NycVehicleLocationRecord record, NycVehicleLocationRecord previousRecord) {
    _record = record;
    _point = ProjectedPointFactory.forward(record.getLatitude(),
        record.getLongitude());
    _previousRecord = previousRecord;
  }

  public NycVehicleLocationRecord getRecord() {
    return _record;
  }

  public ProjectedPoint getPoint() {
    return _point;
  }
  
  public NycVehicleLocationRecord getPreviousRecord() {
    return _previousRecord;
  }
}
