/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.sms.actions.model;

import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import java.io.Serializable;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Route destination--without stops.
 * @author jmaki
 *
 */
public class RouteDirection implements Serializable {
  
  private static final long serialVersionUID = 1L;

  private String destination;
    
  private Boolean hasUpcomingScheduledService;

  private List<NaturalLanguageStringBean> serviceAlerts;

  private HashMap<Double, VehicleResult> distanceAwaysByVehicleResult;

  
  public RouteDirection(StopGroupBean stopGroup,
                        Boolean hasUpcomingScheduledService,
                        HashMap<Double, VehicleResult> distanceAwaysByVehicleResult,
                        List<NaturalLanguageStringBean> serviceAlerts) {
    this.destination = stopGroup.getName().getName();    
    this.hasUpcomingScheduledService = hasUpcomingScheduledService;
    this.distanceAwaysByVehicleResult = distanceAwaysByVehicleResult;
    this.serviceAlerts = serviceAlerts;
  }

  public String getDestination() {
    return destination;
  }

  public Boolean hasUpcomingScheduledService() {
    return hasUpcomingScheduledService;
  }
    
  public List<String> getDistanceAways() {
    if(distanceAwaysByVehicleResult == null)
      return new ArrayList<String>();
    else {
      ArrayList<String> distanceAwayStrings = new ArrayList<>();
      for (VehicleResult vr : distanceAwaysByVehicleResult.values()) {
        distanceAwayStrings.add(vr.getTimeOrDistance());
      }
      return distanceAwayStrings;
    }
  }

  public HashMap<Double, String> getDistanceAwaysWithSortKey() {
    if (distanceAwaysByVehicleResult == null)
      return null;
    HashMap<Double, String> distanceAwaysWithSortKey = new HashMap<>();
    for (Map.Entry<Double, VehicleResult> e: distanceAwaysByVehicleResult.entrySet()) {
      distanceAwaysWithSortKey.put(e.getKey(), e.getValue().getTimeOrDistance());
    }
    return distanceAwaysWithSortKey;
  }

  public HashMap<Double, String> getDistanceAwaysAndOccupancyWithSortKey() {
    if (distanceAwaysByVehicleResult == null)
      return null;
    HashMap<Double, String> distanceAwaysAndOccupancyWithSortKey = new HashMap<>();
    for (Map.Entry<Double, VehicleResult> e: distanceAwaysByVehicleResult.entrySet()) {
      distanceAwaysAndOccupancyWithSortKey.put(e.getKey(), e.getValue().getTimeOrDistanceAndOccupancy());
    }
    return distanceAwaysAndOccupancyWithSortKey;
  }


  public List<NaturalLanguageStringBean> getSerivceAlerts() {
    return serviceAlerts;
  }

  /**
   * does any of the results contain valid occupancy data
   * @return
   */
  public boolean hasApc() {
    if (distanceAwaysByVehicleResult == null) {
      return false;
    }
    for (VehicleResult values : distanceAwaysByVehicleResult.values()) {
      if (values.hasApc())
        return true;
    }
    return false;
  }

  /**
   * Does the value at distanceAway contain valid occupancy data
   * @param distanceAway
   * @return
   */
  public boolean hasApc(Double distanceAway) {
    if (distanceAway != null) {
      if (distanceAwaysByVehicleResult.containsKey(distanceAway)) {
        return distanceAwaysByVehicleResult.get(distanceAway).hasApc();
      }
    }
    return false;
  }
}
