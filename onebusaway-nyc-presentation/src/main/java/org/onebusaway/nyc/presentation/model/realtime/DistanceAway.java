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
package org.onebusaway.nyc.presentation.model.realtime;

import org.onebusaway.transit_data.model.trips.TripStatusBean;

import java.util.Date;

public class DistanceAway implements Comparable<DistanceAway> {

  // FIXME move to config service?
  private int atStopThresholdInFeet = 100;

  // FIXME move to config service?
  private int arrivingThresholdInFeet = 500;

  // FIXME move to config service?
  private int arrivingThresholdInStops = 0;

  // FIXME move to config service?
  private int showDistanceInStopsThresholdInStops = 3;

  private final int stopsAway;

  private final int feetAway;

  private final TripStatusBean statusBean;

  public DistanceAway(int stopsAway, int feetAway, TripStatusBean statusBean) {
    this.stopsAway = stopsAway;
    this.feetAway = feetAway;
    this.statusBean = statusBean;
  }

  public int getStopsAway() {
    return stopsAway;
  }

  public int getFeetAway() {
    return feetAway;
  }

  public Date getUpdateTimestamp() {
    return new Date(statusBean.getLastLocationUpdateTime());
  }

  // FIXME we're "at terminal" if vehicle is currently in layover--we filter out layover buses
  // we don't want to display in NycSearchServiceImpl.java, as appropriate.
  public Boolean isAtTerminal() {
    if(statusBean != null) {
      String phase = statusBean.getPhase();

      if (phase != null && 
          (phase.toLowerCase().equals("layover_during") || 
              phase.toLowerCase().equals("layover_before"))) {
        return true;
      } else
        return false;
    }

    return null;
  }
  
  public String getPresentableDistance() {
    String r = "";

    if(feetAway <= atStopThresholdInFeet)
      r = "at stop";		
    else if(isAtTerminal() == false 
        && feetAway <= arrivingThresholdInFeet && stopsAway <= arrivingThresholdInStops)

      r = "approaching";
    else {
      if(stopsAway <= showDistanceInStopsThresholdInStops) {
        if(stopsAway == 0)
          r = "< 1 stop away";
        else	  
          r = stopsAway == 1
          ? "1 stop away"
              : stopsAway + " stops away";			
      } else {
        double milesAway = (float)feetAway / 5280;
        r = String.format("%1.1f mi. away", milesAway);
      }
    }

    if(r.isEmpty())
      return null;
    else
      return r;
  }

  @Override
  public int hashCode() {
    return 31*feetAway + 31*stopsAway;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DistanceAway))
      return false;
    DistanceAway rhs = (DistanceAway) obj;
    if (feetAway != rhs.feetAway)
      return false;
    if (stopsAway != rhs.stopsAway)
      return false;
    return true;
  }

  @Override
  public int compareTo(DistanceAway rhs) {
    if (feetAway < rhs.feetAway) return -1;
    if (feetAway > rhs.feetAway) return 1;
    if (stopsAway < rhs.stopsAway) return -1;
    if (stopsAway > rhs.stopsAway) return 1;
    return 0;
  }
}
