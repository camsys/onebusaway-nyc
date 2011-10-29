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

import java.util.List;

public class RouteDestinationItem {
  
  private String headsign;

  private String directionId;

  private List<String> polylines;
  
  private List<StopResult> stops;

  public RouteDestinationItem() {}

  public void setHeadsign(String headsign) {
    this.headsign = headsign;
  }

  public void setDirectionId(String directionId) {
    this.directionId = directionId;
  }

  public void setPolylines(List<String> polylines) {
    this.polylines = polylines;
  }

  public void setStops(List<StopResult> stops) {
    this.stops = stops;
  }
  
  public String getHeadsign() {
    return headsign;
  }

  public String getDirectionId() {
    return directionId;
  }
  
  public List<String> getPolylines() {
    return polylines;
  }
  
  public List<StopResult> getStops() {
    return stops;
  }

}