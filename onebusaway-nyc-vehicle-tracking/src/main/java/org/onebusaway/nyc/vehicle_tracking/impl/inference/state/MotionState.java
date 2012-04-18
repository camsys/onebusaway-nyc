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
package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import org.onebusaway.geospatial.model.CoordinatePoint;

import com.google.common.collect.ComparisonChain;

public final class MotionState implements Comparable<MotionState> {

  private final long lastInMotionTime;

  private final CoordinatePoint lastInMotionLocation;

  private final boolean vehicleHasNotMoved;

  public MotionState(long lastInMotionTime, CoordinatePoint lastInMotionLocation, boolean vehicleHasNotMoved) {
    this.lastInMotionTime = lastInMotionTime;
    this.lastInMotionLocation = lastInMotionLocation;
    this.vehicleHasNotMoved = vehicleHasNotMoved;
  }

  public long getLastInMotionTime() {
    return lastInMotionTime;
  }

  public CoordinatePoint getLastInMotionLocation() {
    return lastInMotionLocation;
  }

  @Override
  public int compareTo(MotionState rightMotion) {
    if (this == rightMotion)
      return 0;

    return ComparisonChain.start().compare(this.lastInMotionTime,
        rightMotion.getLastInMotionTime()).compare(
        this.lastInMotionLocation.getLat(),
        rightMotion.getLastInMotionLocation().getLat()).compare(
        this.lastInMotionLocation.getLon(),
        rightMotion.getLastInMotionLocation().getLon()).result();
  }

  private int _hash = 0;
  
  @Override
  public int hashCode() {
    if (_hash != 0)
      return _hash;
      
    final int prime = 31;
    int result = 1;
    result = prime
        * result
        + ((lastInMotionLocation == null) ? 0 : lastInMotionLocation.hashCode());
    result = prime * result
        + (int) (lastInMotionTime ^ (lastInMotionTime >>> 32));
    result = prime * result + (vehicleHasNotMoved ? 1231 : 1237);
    _hash = result;
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof MotionState)) {
      return false;
    }
    MotionState other = (MotionState) obj;
    if (lastInMotionLocation == null) {
      if (other.lastInMotionLocation != null) {
        return false;
      }
    } else if (!lastInMotionLocation.equals(other.lastInMotionLocation)) {
      return false;
    }
    if (lastInMotionTime != other.lastInMotionTime) {
      return false;
    }
    if (vehicleHasNotMoved != other.vehicleHasNotMoved) {
      return false;
    }
    return true;
  }

  public boolean hasVehicleNotMoved() {
    return vehicleHasNotMoved;
  }

}
