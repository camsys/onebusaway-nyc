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
import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.presentation.impl.DefaultModelFactory;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteItem;
import org.onebusaway.nyc.presentation.model.search.StopSearchResult;
import org.onebusaway.nyc.presentation.service.ModelFactory;
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
  
  private ModelFactory _modelFactory = new DefaultModelFactory();
  
  @Autowired
  private TransitDataService _transitDataService;

  @Autowired
  private ServiceAreaService _serviceArea;
  
  public void setModelFactory(ModelFactory factory) {
    _modelFactory = factory;
  }

  @Override
  public List<StopSearchResult> resultsForLocation(Double lat, Double lng) {
    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(lat, lng, _distanceToStops);
    
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(bounds);
    queryBean.setMaxCount(100);
    
    StopsBean stops = _transitDataService.getStops(queryBean);

    List<StopSearchResult> results = stopsBeanToStopResults(stops);

    return results;
  }

  @Override
  public List<StopSearchResult> makeResultFor(String stopId) {
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(_serviceArea.getServiceArea());
    queryBean.setQuery(stopId);
    queryBean.setMaxCount(100);

    StopsBean stops = _transitDataService.getStops(queryBean);

    List<StopSearchResult> results = stopsBeanToStopResults(stops);

    return results;
  }

  private List<StopSearchResult> stopsBeanToStopResults(StopsBean stopsBean) {
    ArrayList<StopSearchResult> results = new ArrayList<StopSearchResult>();
    
    for(StopBean stopBean : stopsBean.getStops()) {
      List<RouteItem> routesAvailable = new ArrayList<RouteItem>();
      for(RouteBean routeBean : stopBean.getRoutes()) {
        List<RouteDestinationItem> destinations = getRouteDestinationItemsForRouteAndStop(routeBean, stopBean);      
        routesAvailable.add(_modelFactory.getRouteItemModelFor(routeBean, destinations));      
      }
   
      results.add(_modelFactory.getSearchResultModelFor(stopBean, routesAvailable));
    }
    
    return results;
  }

  // return route destination items for the given route + stop
  private List<RouteDestinationItem> getRouteDestinationItemsForRouteAndStop(RouteBean routeBean, StopBean stopBean) {
    List<RouteDestinationItem> destinations = new ArrayList<RouteDestinationItem>();

    StopsForRouteBean stopsForRoute = _transitDataService.getStopsForRoute(routeBean.getId());
    List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
    for (StopGroupingBean stopGroupingBean : stopGroupings) {
      List<StopGroupBean> stopGroups = stopGroupingBean.getStopGroups();
      for (StopGroupBean stopGroupBean : stopGroups) {
        NameBean name = stopGroupBean.getName();
        String type = name.getType();
        if (!type.equals("destination"))
          continue;
     
        List<String> stopIdsInGroup = stopGroupBean.getStopIds();
        if(!stopIdsInGroup.contains(stopBean.getId()))
          continue;
        
        List<String> headsigns = name.getNames();
        String directionId = stopGroupBean.getId();

        // polyline--seems to always just be one?
        String polyline = null;
        List<EncodedPolylineBean> polylines = stopGroupBean.getPolylines();
        if(polylines.size() > 0)
          polyline = polylines.get(0).getPoints();

        // add data for all available headsigns
        for(String headsign: headsigns) {
          RouteDestinationItem routeDestination = 
              _modelFactory.getRouteDestinationItemModelFor(headsign, directionId, polyline);

          destinations.add(routeDestination);
        }
      }
    }
      
    return destinations;
  }
}
