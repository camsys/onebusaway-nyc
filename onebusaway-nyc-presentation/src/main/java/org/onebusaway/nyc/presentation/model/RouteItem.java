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
package org.onebusaway.nyc.presentation.model;

import java.util.List;

/**
 * Data transfer object for routes available to a stop,
 * and how far the next few vehicles are.
 */
public class RouteItem {

  private final String routeId;
  private final String directionId;
  private final String routeDescription;
  private final String routeHeadsign;
  private final List<DistanceAway> distanceAways;

  public RouteItem(String routeId, String routeDescription, String routeHeadsign, String directionId,
      List<DistanceAway> distanceAways) {
        this.routeId = routeId;
        this.directionId = directionId;
        this.routeDescription = routeDescription;
        this.routeHeadsign = routeHeadsign;
        this.distanceAways = distanceAways;
  }

  public String getHeadsign() {
    return routeHeadsign;
  }

  public String getRouteId() {
	    return routeId;
  }

  public String getDirectionId() {
	    return directionId;
  }

  public String getDescription() {
	    return routeDescription;
  }

  public List<DistanceAway> getDistanceAways() {
	    return distanceAways;
  }
}
