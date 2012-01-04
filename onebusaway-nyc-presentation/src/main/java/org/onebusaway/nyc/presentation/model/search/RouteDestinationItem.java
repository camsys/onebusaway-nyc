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
package org.onebusaway.nyc.presentation.model.search;

import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.transit_data.model.StopGroupBean;

import java.util.ArrayList;
import java.util.List;

public class RouteDestinationItem {
 
  private final StopGroupBean group;
  
  private final List<StopResult> stops;
  
  private final Boolean hasUpcomingScheduledService;
  
  public RouteDestinationItem(StopGroupBean group, List<StopResult> stops, Boolean hasUpcomingScheduledService) {
    this.group = group;
    this.stops = stops;
    this.hasUpcomingScheduledService = hasUpcomingScheduledService;
  }

  public String getHeadsign() {
    return group.getName().getName();
  }

  public String getDirectionId() {
    return group.getId();
  }

  public Boolean getHasUpcomingScheduledService() {
    return hasUpcomingScheduledService;
  }

  public List<String> getPolylines() {
    List<String> output = new ArrayList<String>();
    for(EncodedPolylineBean polyline : group.getPolylines()) {
      output.add(polyline.getPoints());
    }
    return output;
  }
  
  public List<StopResult> getStops() {
    return stops;
  }

}