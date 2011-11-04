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
package org.onebusaway.nyc.webapp.actions.api;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.service.search.RouteSearchService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@ParentPackage("json-default")
@Result(type="json", params={"callbackParameter", "callback"})
public class RoutesWithinBoundsAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  @Autowired
  private RouteSearchService _routeSearchService;

  private List<?> _routeResults;
  
  private CoordinateBounds _bounds = null;

  public void setBounds(String bounds) {
    if(bounds == null)
      _bounds = null;
    
    String[] coordinates = bounds.split(",");
    if(coordinates.length == 4) {
      _bounds = new CoordinateBounds(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]),
          Double.parseDouble(coordinates[2]), Double.parseDouble(coordinates[3]));
    } else
      _bounds = null;
  }

  @Override
  public String execute() {
    if(_bounds != null) {
      List<RouteResult> routeItems = _routeSearchService.resultsForLocation(_bounds);
      if(routeItems.size() > 5) {
        _routeResults = routeItems;
        return SUCCESS;
      }

      List<RouteResult> routeSearchResults = new ArrayList<RouteResult>();        
      for(RouteResult routeItem : routeItems) {
        routeSearchResults.addAll(_routeSearchService.resultsForQuery(routeItem.getRouteIdWithoutAgency()));
      }
      
      _routeResults = routeSearchResults;
      return SUCCESS;
    }
    
    return ERROR;
  }

  public List<?> getSearchResults() {
    return _routeResults;
  }
}
