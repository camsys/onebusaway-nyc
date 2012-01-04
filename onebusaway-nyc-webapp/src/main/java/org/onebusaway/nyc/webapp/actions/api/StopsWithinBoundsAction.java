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
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@ParentPackage("json-default")
@Result(type="json", params={"callbackParameter", "callback"})
public class StopsWithinBoundsAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  @Autowired
  private TransitDataService _transitDataService;

  private List<StopResult> _results = new ArrayList<StopResult>();

  private CoordinateBounds _bounds = new CoordinateBounds();
  
  public void setBounds(String bounds) {
    String[] coordinates = bounds.split(",");
    if(coordinates.length == 4) {
      _bounds = new CoordinateBounds(Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]),
          Double.parseDouble(coordinates[2]), Double.parseDouble(coordinates[3]));
    }
  }
  
  @Override
  public String execute() {    
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(_bounds);
    queryBean.setMaxCount(200);    
    
    StopsBean stops = _transitDataService.getStops(queryBean);
    
    for(StopBean stop : stops.getStops()) {
      List<RouteResult> routesAvailable = new ArrayList<RouteResult>();
      for(RouteBean routeBean : stop.getRoutes()) {
        RouteResult routeResult = new RouteResult(routeBean, null);
        routesAvailable.add(routeResult);
      }
      
      StopResult result = new StopResult(stop, routesAvailable);
      
      _results.add(result);
    }
    
    return SUCCESS;
  }   

  /** 
   * VIEW METHODS
   */
  public List<StopResult> getSearchResults() {
    return _results;
  }

}
