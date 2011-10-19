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

import org.onebusaway.nyc.presentation.impl.WebappSupportLibrary;
import org.onebusaway.nyc.presentation.model.realtime_data.RouteItem;
import org.onebusaway.nyc.presentation.service.search.SearchResult;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

/**
 * Data transfer object used in the front-end interfaces to represent
 * a stop result.
 * @author jmaki
 *
 */
public class StopSearchResult implements SearchResult {

  private final String stopId;

  private final String name;

  private final String stopDirection;

  private final Double latitude;
  
  private final Double longitude;
  
  private final List<RouteItem> availableRoutes;

  private final List<NaturalLanguageStringBean> serviceAlerts;

  public StopSearchResult(String stopId, String name, Double latitude, Double longitude, String stopDirection, 
      List<RouteItem> availableRoutes, List<NaturalLanguageStringBean> serviceAlerts) {
    this.stopId = stopId;
    this.name = name;
    this.latitude = latitude;
    this.longitude = longitude;
    this.availableRoutes = availableRoutes;
    this.serviceAlerts = serviceAlerts;
    this.stopDirection = stopDirection;
  }

  public String getStopId() {
    return stopId;
  }

  public String getStopIdWithoutAgency() {
    WebappSupportLibrary webappIdParser = new WebappSupportLibrary();
    return webappIdParser.parseIdWithoutAgency(stopId);
  }

  public String getName() {
    return name;
  }

  public Double getLatitude() {
    return latitude;
  }

  public Double getLongitude() {
    return longitude;
  }

  public String getType() {
    return "stop";
  }

  public String getStopDirection() {
    return stopDirection;
  }

  public List<RouteItem> getRoutesAvailable() {
    return availableRoutes;
  }

  public List<NaturalLanguageStringBean> getServiceAlerts() {
    return serviceAlerts;
  }
}
