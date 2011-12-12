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

import org.onebusaway.container.cache.Cacheable;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.presentation.impl.AgencySupportLibrary;
import org.onebusaway.nyc.presentation.impl.DefaultPresentationModelFactory;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.PresentationModelFactory;
import org.onebusaway.nyc.presentation.service.search.RouteSearchService;
import org.onebusaway.presentation.services.ServiceAreaService;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RoutesBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RouteSearchServiceImpl implements RouteSearchService {
    
  // when querying for routes from a lat/lng, use this distance in meters
  private double _distanceToRoutes = 100;

  private PresentationModelFactory _modelFactory = new DefaultPresentationModelFactory();

  @Autowired
  private TransitDataService _transitDataService;
  
  @Autowired
  private ServiceAreaService _serviceArea;
    
  public void setModelFactory(PresentationModelFactory factory) {
    _modelFactory = factory;
  }
  
  @Override
  public boolean isRoute(String value) {
    return getAllRouteIds().contains(value);
  }
  
  @Override
  public List<RouteResult> resultsForLocation(Double latitude, Double longitude) {
    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(latitude, longitude, _distanceToRoutes);

    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(bounds);
    queryBean.setMaxCount(100);
    
    RoutesBean routes = _transitDataService.getRoutes(queryBean);

    return routesBeanToRouteSearchResults(routes, true);
  }    
  
  @Override
  public List<RouteResult> resultsForLocation(CoordinateBounds bounds) {
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(bounds);
    queryBean.setMaxCount(100);
    
    RoutesBean routes = _transitDataService.getRoutes(queryBean);

    return routesBeanToRouteSearchResults(routes, false);
  }

  @Override
  public RouteResult makeResultForRouteId(String routeId) {
    RouteBean routeBean = _transitDataService.getRouteForId(routeId);
    
    if(routeBean != null) {
      List<RouteDestinationItem> destinations = getRouteDestinationItemsForRoute(routeBean, true);
      return _modelFactory.getRouteModel(routeBean, destinations);
    }
    
    return null;
  }
  
  @Override
  public List<RouteResult> resultsForQuery(String routeQuery) {
    if(routeQuery != null) {
      routeQuery = routeQuery.toUpperCase();
    }
    
    // FIXME we have this in here to disable the "full text" search OBA has--we'll enable that later!
    if(!isRoute(routeQuery)) {
      return Collections.emptyList();
    }
    
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(_serviceArea.getServiceArea());
    queryBean.setQuery(routeQuery);
    queryBean.setMaxCount(100);

    RoutesBean routes = _transitDataService.getRoutes(queryBean);

    return routesBeanToRouteSearchResults(routes, true);
  }

  private List<RouteResult> routesBeanToRouteSearchResults(RoutesBean routesBean, boolean includeStops) {
    ArrayList<RouteResult> results = new ArrayList<RouteResult>();

    List<RouteBean> routeBeans = routesBean.getRoutes();
    for(RouteBean routeBean : routeBeans) {
      List<RouteDestinationItem> destinations = getRouteDestinationItemsForRoute(routeBean, includeStops);
      results.add(_modelFactory.getRouteModel(routeBean, destinations));
    }
    
    return results;
  } 
  
  private List<RouteDestinationItem> getRouteDestinationItemsForRoute(RouteBean routeBean, boolean includeStops) {
    List<RouteDestinationItem> output = new ArrayList<RouteDestinationItem>();
    
    StopsForRouteBean stopsForRoute = _transitDataService.getStopsForRoute(routeBean.getId());
    
    // create stop ID->stop bean map
    Map<String, StopBean> stopIdToStopBeanMap = new HashMap<String, StopBean>();
    for(StopBean stopBean : stopsForRoute.getStops()) {
      stopIdToStopBeanMap.put(stopBean.getId(), stopBean);
    }    
    
    // generate route destination groups with beans
    List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
    for (StopGroupingBean stopGroupingBean : stopGroupings) {
      for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
        NameBean name = stopGroupBean.getName();
        String type = name.getType();
        if (!type.equals("destination"))
          continue;
        
        List<StopResult> stopItems = null;
        if(includeStops == true && !stopGroupBean.getStopIds().isEmpty()) {
          stopItems = new ArrayList<StopResult>();

          for(String stopId : stopGroupBean.getStopIds()) {
            stopItems.add(_modelFactory.getStopModelForRoute(stopIdToStopBeanMap.get(stopId), routeBean));
          }
        }
          
        output.add(_modelFactory.getRouteDestinationModelForRoute(stopGroupBean, routeBean, stopItems));
      }
    }
    
    return output;
  }
  
  // build a list of all known routes for later tests for "routeness".
  @Cacheable
  private List<String> getAllRouteIds() {
    List<String> knownRouteIds = new ArrayList<String>();
    
    for(AgencyWithCoverageBean agency : _transitDataService.getAgenciesWithCoverage()) {
      ListBean<RouteBean> routes = _transitDataService.getRoutesForAgencyId(agency.getAgency().getId());
      for(RouteBean route : routes.getList()) {
        knownRouteIds.add(AgencySupportLibrary.parseIdWithoutAgency(route.getId()));
      }
    }
    
    return knownRouteIds;
  }

}