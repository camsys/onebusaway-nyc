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
package org.onebusaway.nyc.presentation.impl.search;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.presentation.impl.DefaultPresentationModelFactory;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.PresentationModelFactory;
import org.onebusaway.nyc.presentation.service.search.StopSearchService;
import org.onebusaway.presentation.services.ServiceAreaService;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StopSearchServiceImpl implements StopSearchService {
  
  // when querying for stops from a lat/lng, use this distance in meters
  private double _distanceToStops = 100;
  
  private PresentationModelFactory _modelFactory = new DefaultPresentationModelFactory();
  
  @Autowired
  private TransitDataService _transitDataService;

  @Autowired
  private ServiceAreaService _serviceArea;
  
  public void setModelFactory(PresentationModelFactory factory) {
    _modelFactory = factory;
  }

  @Override
  public List<StopResult> resultsForLocation(Double lat, Double lng) {
    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(lat, lng, _distanceToStops);
    
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(bounds);
    queryBean.setMaxCount(100);
    
    StopsBean stops = _transitDataService.getStops(queryBean);

    List<StopResult> results = stopsBeanToStopResults(stops);

    return results;
  }

  @Override
  public StopResult makeResultForStopId(String stopId) {
    StopBean stopBean = _transitDataService.getStop(stopId);
    
    if(stopBean != null) {
      List<RouteResult> routesAvailable = new ArrayList<RouteResult>();
      
      for(RouteBean routeBean : stopBean.getRoutes()) {
        List<RouteDestinationItem> destinations = getRouteDestinationItemsForRouteAndStop(routeBean, stopBean);      
        routesAvailable.add(_modelFactory.getRouteModelForStop(routeBean, destinations));      
      }

      return _modelFactory.getStopModel(stopBean, routesAvailable);
    }
    
    return null;
  }
  
  @Override
  public List<StopResult> resultsForQuery(String stopQuery) {
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(_serviceArea.getServiceArea());
    queryBean.setQuery(stopQuery);
    queryBean.setMaxCount(100);

    StopsBean stops = _transitDataService.getStops(queryBean);

    List<StopResult> results = stopsBeanToStopResults(stops);

    return results;
  }

  private List<StopResult> stopsBeanToStopResults(StopsBean stopsBean) {
    ArrayList<StopResult> results = new ArrayList<StopResult>();
    
    for(StopBean stopBean : stopsBean.getStops()) {
      List<RouteResult> routesAvailable = new ArrayList<RouteResult>();
      
      for(RouteBean routeBean : stopBean.getRoutes()) {
        List<RouteDestinationItem> destinations = getRouteDestinationItemsForRouteAndStop(routeBean, stopBean);      
        routesAvailable.add(_modelFactory.getRouteModelForStop(routeBean, destinations));      
      }

      results.add(_modelFactory.getStopModel(stopBean, routesAvailable));
    }
    
    return results;
  }

  // return route destination items for the given route + stop
  private List<RouteDestinationItem> getRouteDestinationItemsForRouteAndStop(RouteBean routeBean, StopBean stopBean) {
    List<RouteDestinationItem> destinations = new ArrayList<RouteDestinationItem>();

    StopsForRouteBean stopsForRoute = _transitDataService.getStopsForRoute(routeBean.getId());
    List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
    for (StopGroupingBean stopGroupingBean : stopGroupings) {
      for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
        NameBean name = stopGroupBean.getName();
        String type = name.getType();
        if (!type.equals("destination"))
          continue;
     
        List<String> stopIdsInGroup = stopGroupBean.getStopIds();
        if(!stopIdsInGroup.contains(stopBean.getId()))
          continue;
        
        destinations.add(_modelFactory.getRouteDestinationModelForStop(stopGroupBean, routeBean, stopBean));
      }
    }
      
    return destinations;
  }
}
