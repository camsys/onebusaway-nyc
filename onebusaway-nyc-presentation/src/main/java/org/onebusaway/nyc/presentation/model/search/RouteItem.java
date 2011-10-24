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
import org.onebusaway.transit_data.model.RouteBean;

import java.util.List;

public class RouteItem {

  private final RouteBean routeBean;
  
  private final List<RouteDestinationItem> destinations;

  private static final WebappSupportLibrary idParser = new WebappSupportLibrary();

  public RouteItem(RouteBean routeBean, List<RouteDestinationItem> destinations) {
    this.routeBean = routeBean;
    this.destinations = destinations;
  }

  public String getRouteId() {
    return routeBean.getId();
  }

  public String getRouteIdWithoutAgency() {
    return idParser.parseIdWithoutAgency(getRouteId());
  }

  public String getDescription() {
    return routeBean.getDescription();
  }

  public String getColor() {
    return routeBean.getColor();
  }
  
  public List<RouteDestinationItem> getDestinations() {
    return destinations;
  }
}
