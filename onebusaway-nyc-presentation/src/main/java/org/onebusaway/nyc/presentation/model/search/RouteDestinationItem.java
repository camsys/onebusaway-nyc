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

import org.onebusaway.nyc.presentation.model.realtime_data.StopItem;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import java.util.List;

/**
 * (Subitem of RouteSearchResult)
 * @author jmaki
 *
 */
public class RouteDestinationItem {

  private final String headsign;
  
  private final String directionId;
  
  private final List<StopItem> stopItems;

  private final String polyline;

  private final List<NaturalLanguageStringBean> serviceAlerts;

  public RouteDestinationItem(String headsign, String directionId, List<StopItem> stopItems,
      String polyline, List<NaturalLanguageStringBean> serviceAlerts) {
    this.headsign = headsign;
    this.directionId = directionId;
    this.stopItems = stopItems;
    this.polyline = polyline;
    this.serviceAlerts = serviceAlerts;
  }
  
  public String getHeadsign() {
    return headsign;
  }

  public String getDirectionId() {
    return directionId;
  }

  public List<StopItem> getStopItems() {
    return stopItems;
  }
  
  public String getPolyline() {
    return polyline;
  }
  
  public List<NaturalLanguageStringBean> getServiceAlerts() {
    return serviceAlerts;
  }

}