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

import com.google.common.base.Function;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

public class Observation implements Comparable<Observation> {

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
  
  enum PointFunction implements Function<ProjectedPoint, Double> {
    getY {
      @Override
      public Double apply(final ProjectedPoint p) {
        return p.getY();
      }
    },
    getX {
      @Override
      public Double apply(final ProjectedPoint p) {
        return p.getX();
      }
    }
  }
  
  static private final Ordering<ProjectedPoint> _orderByXandY = 
      Ordering.natural().nullsLast().onResultOf(PointFunction.getX)
      .compound(Ordering.natural().nullsLast().onResultOf(PointFunction.getY));
  
  @Override
  public int compareTo(Observation o2) {
    
    if (this == o2)
      return 0;
    
    int res = ComparisonChain.start()
        .compare(outOfService, o2.outOfService)
        .compare(atTerminal, o2.atTerminal)
        .compare(atBase, o2.atBase)
        .compare(_timestamp, o2._timestamp)
        .compare(_record, o2._record)
        .compare(_point, o2._point, _orderByXandY)
        .compare(_lastValidDestinationSignCode, o2._lastValidDestinationSignCode, 
            Ordering.natural().nullsLast())
        .compare(_fuzzyMatchDistance, o2._fuzzyMatchDistance)
        .result();
    
    return res;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + _fuzzyMatchDistance;
    result = prime
        * result
        + ((_lastValidDestinationSignCode == null) ? 0
            : _lastValidDestinationSignCode.hashCode());
    result = prime * result + ((_point == null) ? 0 : _point.hashCode());
    result = prime * result + ((_record == null) ? 0 : _record.hashCode());
    result = prime * result + (int) (_timestamp ^ (_timestamp >>> 32));
    result = prime * result + (atBase ? 1231 : 1237);
    result = prime * result + (atTerminal ? 1231 : 1237);
    result = prime * result + (outOfService ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof Observation))
      return false;
    Observation other = (Observation) obj;
    if (_fuzzyMatchDistance != other._fuzzyMatchDistance)
      return false;
    if (_lastValidDestinationSignCode == null) {
      if (other._lastValidDestinationSignCode != null)
        return false;
    } else if (!_lastValidDestinationSignCode.equals(other._lastValidDestinationSignCode))
      return false;
    if (_point == null) {
      if (other._point != null)
        return false;
    } else if (!_point.equals(other._point))
      return false;
    if (_record == null) {
      if (other._record != null)
        return false;
    } else if (!_record.equals(other._record))
      return false;
    if (_timestamp != other._timestamp)
      return false;
    if (atBase != other.atBase)
      return false;
    if (atTerminal != other.atTerminal)
      return false;
    if (outOfService != other.outOfService)
      return false;
    return true;
  }

}
