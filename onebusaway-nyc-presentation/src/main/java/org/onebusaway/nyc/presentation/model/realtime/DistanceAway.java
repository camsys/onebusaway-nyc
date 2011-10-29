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

import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

import java.util.Date;

public class DistanceAway implements Comparable<DistanceAway> {

  // FIXME move to config service?
  private final int atStopThresholdInFeet = 100;

  // FIXME move to config service?
  private final int arrivingThresholdInFeet = 500;

  // FIXME move to config service?
  private final int arrivingThresholdInStops = 0;

  // FIXME move to config service?
  private final int showDistanceInStopsThresholdInStops = 3;

  private int staleDataTimeout;      

  private int stopsAway;

  private int feetAway;

  private TripStatusBean statusBean;

  public DistanceAway() {}

  public void setConfigurationService(ConfigurationService configurationService) {
    staleDataTimeout = configurationService.getConfigurationValueAsInteger("display.staleTimeout", 120);
  }
  
  public void setStopsAway(int stopsAway) {
    this.stopsAway = stopsAway;
  }

  public void setFeetAway(int feetAway) {
    this.feetAway = feetAway;
  }

  public void setStatusBean(TripStatusBean statusBean) {
    this.statusBean = statusBean;
  }

  public int getStopsAway() {
    return stopsAway;
  }

  public int getFeetAway() {
    return feetAway;
  }

  public long getUpdateTimestampReference() {
    return new Date().getTime();
  }
  
  public Long getUpdateTimestamp() {
    return statusBean.getLastLocationUpdateTime();
  }

  // FIXME we're "at terminal" if vehicle is currently in layover--we filter out layover buses
  // we don't want to display in NycSearchServiceImpl.java, as appropriate.
  public Boolean getIsAtTerminal() {
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
    else if(getIsAtTerminal() == false 
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
  public String toString() {
    String s = getPresentableDistance();
    String subpart = new String();

    // at terminal
    if(getIsAtTerminal()) {
      subpart += "at terminal";
    }

    // old
    if(new Date().getTime() - getUpdateTimestamp() > 1000 * staleDataTimeout) {
      if(subpart.length() > 0)
        subpart += ", ";

      subpart += "old data";
    }

    if(subpart.length() > 0) {
      s += " (" + subpart + ")";
    }

    return s;
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
