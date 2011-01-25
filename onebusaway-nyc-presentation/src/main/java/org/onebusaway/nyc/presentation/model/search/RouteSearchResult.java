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

import org.onebusaway.nyc.presentation.model.StopItem;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

/**
 * Data transfer object for route search results
 */
public class RouteSearchResult implements SearchResult {

  private final String routeId;
  private final String routeName;
  private final String routeDescription;
  private final String type;
  private final String tripHeadsign;
  private final String directionId;
  private final List<StopItem> stopItems;
  private final List<NaturalLanguageStringBean> serviceAlerts;

  public RouteSearchResult(String routeId, String routeName,
      String routeDescription, String tripHeadsign, String directionId, 
      	List<StopItem> stopItems, List<NaturalLanguageStringBean> serviceAlerts) {
        this.routeId = routeId;
        this.routeName = routeName;
        this.routeDescription = routeDescription;
        this.tripHeadsign = tripHeadsign;
        this.directionId = directionId;
        this.stopItems = stopItems;
        this.serviceAlerts = serviceAlerts;
        this.type = "route";
  }

  public String getRouteId() {
    return routeId;
  }

  public String getName() {
    return routeName;
  }

  public String getDescription() {
    return routeDescription;
  }

  public String getType() {
    return type;
  }

  public String getTripHeadsign() {
    return tripHeadsign;
  }

  public String getDirectionId() {
    return directionId;
  }

  public List<StopItem> getStopItems() {
    return stopItems;
  }

  public List<NaturalLanguageStringBean> getServiceAlerts() {
	return serviceAlerts;
  }
}
