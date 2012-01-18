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

public final class JourneyStartState {

  private final CoordinatePoint journeyStart;

  public JourneyStartState(CoordinatePoint journeyStart) {
    this.journeyStart = journeyStart;
  }

  public CoordinatePoint getJourneyStart() {
    return journeyStart;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((journeyStart == null) ? 0 : journeyStart.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof JourneyStartState))
      return false;
    JourneyStartState other = (JourneyStartState) obj;
    if (journeyStart == null) {
      if (other.journeyStart != null)
        return false;
    } else if (!journeyStart.equals(other.journeyStart))
      return false;
    return true;
  }

}
