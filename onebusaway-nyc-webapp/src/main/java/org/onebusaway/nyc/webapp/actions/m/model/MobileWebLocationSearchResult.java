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

import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;
import org.onebusaway.nyc.presentation.model.search.LocationSearchResult;
import org.onebusaway.nyc.presentation.model.search.RouteItem;

import java.util.List;

public class MobileWebLocationSearchResult extends LocationSearchResult {
  
  private List<RouteItem> nearbyRoutes;
  
  public MobileWebLocationSearchResult(NycGeocoderResult result, List<RouteItem> nearbyRoutes) {
    super(result);
    this.nearbyRoutes = nearbyRoutes;
  }

  public List<RouteItem> getNearbyRoutes() {
    return nearbyRoutes;
  }

}
