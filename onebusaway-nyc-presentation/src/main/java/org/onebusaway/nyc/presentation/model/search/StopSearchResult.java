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

import org.onebusaway.nyc.presentation.impl.WebappIdParser;
import org.onebusaway.nyc.presentation.model.RouteItem;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

/**
 * Data transfer object for route search results
 */
public class StopSearchResult implements SearchResult {

  private final String stopId;
  private final String name;
  private final List<Double> latlng;
  private final String type;
  private final List<RouteItem> availableRoutes;
  private final List<NaturalLanguageStringBean> serviceAlerts;
  private final String stopDirection;

  public StopSearchResult(String stopId, String name, List<Double> latlng, String stopDirection, 
		  List<RouteItem> availableRoutes, List<NaturalLanguageStringBean> serviceAlerts) {
    this.stopId = stopId;
    this.name = name;
    this.latlng = latlng;
    this.availableRoutes = availableRoutes;
    this.serviceAlerts = serviceAlerts;
    this.stopDirection = stopDirection;
    this.type = "stop";
  }

  public String getStopId() {
    return stopId;
  }
  
  public String getStopIdNoAgency() {
    WebappIdParser webappIdParser = new WebappIdParser();
    return webappIdParser.parseIdWithoutAgency(stopId);
  }

  public String getName() {
    return name;
  }

  public List<Double> getLatlng() {
    return latlng;
  }

  public String getType() {
    return type;
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
