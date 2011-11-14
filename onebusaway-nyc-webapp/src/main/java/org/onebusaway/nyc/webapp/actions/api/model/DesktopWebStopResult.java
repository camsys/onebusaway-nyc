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
package org.onebusaway.nyc.webapp.actions.api.model;

import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.transit_data.model.StopBean;

import java.util.List;

public class DesktopWebStopResult extends StopResult {

  private List<RouteResult> nearbyRoutes;
  
  public DesktopWebStopResult(StopBean stopBean, List<RouteResult> routesAvailable) {
    super(stopBean, routesAvailable);
  }

  public void setNearbyRoutes(List<RouteResult> nearbyRoutes) {
    this.nearbyRoutes = nearbyRoutes;
  }
  
  public List<RouteResult> getNearbyRoutes() {
    return nearbyRoutes;
  }
  
}
