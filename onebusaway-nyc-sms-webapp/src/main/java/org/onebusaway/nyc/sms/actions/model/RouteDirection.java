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

  private HashMap<Double, String> distanceAwaysByDistanceFromStrings;
  
  public RouteDirection(StopGroupBean stopGroup, Boolean hasUpcomingScheduledService, HashMap<Double, String> distanceAwaysByDistanceFromString, List<NaturalLanguageStringBean> serviceAlerts) {
    this.destination = stopGroup.getName().getName();    
    this.hasUpcomingScheduledService = hasUpcomingScheduledService;
    this.distanceAwaysByDistanceFromStrings = distanceAwaysByDistanceFromString;
    this.serviceAlerts = serviceAlerts;
  }

  public String getDestination() {
    return destination;
  }

  public Boolean hasUpcomingScheduledService() {
    return hasUpcomingScheduledService;
  }
    
  public List<String> getDistanceAways() {
    if(distanceAwaysByDistanceFromStrings == null) 
      return new ArrayList<String>();
    else
      return new ArrayList<String>(distanceAwaysByDistanceFromStrings.values());
  }

  public HashMap<Double, String> getDistanceAwaysWithSortKey() {
    return distanceAwaysByDistanceFromStrings;
  }

  public List<NaturalLanguageStringBean> getSerivceAlerts() {
    return serviceAlerts;
  }

}
