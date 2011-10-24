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
package org.onebusaway.nyc.webapp.actions.m.model;

import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteItem;
import org.onebusaway.nyc.presentation.service.search.SearchResult;
import org.onebusaway.transit_data.model.RouteBean;

import java.util.List;

public class MobileWebRouteItem extends RouteItem implements SearchResult {

  public MobileWebRouteItem(RouteBean routeBean, List<RouteDestinationItem> destinations) {
    super(routeBean, destinations);
  }

  @Override
  public String getType() {
    return "routeItem";
  }

  @Override
  public String getName() {
    return getRouteIdWithoutAgency();
  }
  
}
