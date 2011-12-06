/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.List;
import java.util.Set;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.transit_data_federation.impl.ProjectedPointFactory;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;

public class Observation {

  private final long _timestamp;

  private final NycRawLocationRecord _record;

  private final ProjectedPoint _point;

  private final String _lastValidDestinationSignCode;

  private final boolean atBase;

  private final boolean atTerminal;

  private final boolean outOfService;

  private Observation _previousObservation;

  private final Set<AgencyAndId> _dscImpliedRouteCollections;

  private int _fuzzyMatchDistance;

  public Observation(long timestamp, NycRawLocationRecord record,
      String lastValidDestinationSignCode, boolean atBase, boolean atTerminal,
      boolean outOfService, Observation previousObservation,
      Set<AgencyAndId> dscImpliedRoutes) {
    _timestamp = timestamp;
    _record = record;
    _point = ProjectedPointFactory.forward(record.getLatitude(),
        record.getLongitude());
    _lastValidDestinationSignCode = lastValidDestinationSignCode;
    _dscImpliedRouteCollections = dscImpliedRoutes;
    this.atBase = atBase;
    this.atTerminal = atTerminal;
    this.outOfService = outOfService;
    
    _previousObservation = previousObservation;
  }

  public long getTime() {
    return _timestamp;
  }

  public NycRawLocationRecord getRecord() {
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

  public NycRawLocationRecord getPreviousRecord() {
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

  public Set<AgencyAndId> getDscImpliedRouteCollections() {
    return _dscImpliedRouteCollections;
  }

  public void setFuzzyMatchDistance(int bestDist) {
    _fuzzyMatchDistance = bestDist;
  }
  
  public int getFuzzyMatchDistance() {
    return _fuzzyMatchDistance;
  }

}
