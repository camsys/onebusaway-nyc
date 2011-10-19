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

import org.onebusaway.nyc.presentation.impl.WebappSupportLibrary;
import org.onebusaway.nyc.presentation.service.search.SearchResult;

import java.util.List;

/**
 * Data transfer object used in the front-end interfaces to represent
 * a route result.
 * @author jmaki
 *
 */
public class RouteSearchResult implements SearchResult {

  // e.g. MTA NYCT_B63
  private final String routeId;

  // e.g. 5TH AVENUE
  private final String routeDescription;

  private final List<RouteDestinationItem> routeDestinations;
  
  private static final WebappSupportLibrary idParser = new WebappSupportLibrary();

  public RouteSearchResult(String routeId, String routeDescription, 
      List<RouteDestinationItem> routeDestinations) {
        this.routeId = routeId;
        this.routeDescription = routeDescription;
        this.routeDestinations = routeDestinations;
  }

  public String getRouteId() {
    return routeId;
  }

  public String getName() {
    return idParser.parseIdWithoutAgency(routeId);
  }

  public String getDescription() {
    return routeDescription;
  }

  public String getType() {
    return "route";
  }

  public List<RouteDestinationItem> getDestinations() {
    return routeDestinations;
  }
}
