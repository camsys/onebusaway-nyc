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

import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.nyc.presentation.service.realtime.ScheduledServiceService;
import org.onebusaway.nyc.presentation.service.search.SearchResultFactory;
import org.onebusaway.nyc.presentation.service.search.SearchService;
import org.onebusaway.nyc.webapp.actions.api.model.GeocodeResult;
import org.onebusaway.nyc.webapp.actions.api.model.RouteAtStop;
import org.onebusaway.nyc.webapp.actions.api.model.RouteDirection;
import org.onebusaway.nyc.webapp.actions.api.model.RouteInRegionResult;
import org.onebusaway.nyc.webapp.actions.api.model.RouteResult;
import org.onebusaway.nyc.webapp.actions.api.model.StopResult;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.services.TransitDataService;

import java.util.ArrayList;
import java.util.List;

public class SearchResultFactoryImpl implements SearchResultFactory {

  private TransitDataService _transitDataService;

  private SearchService _searchService;

  private ScheduledServiceService _scheduledServiceService;

  public SearchResultFactoryImpl(TransitDataService transitDataService, SearchService searchService, ScheduledServiceService scheduledServiceService) {
    _transitDataService = transitDataService;
    _searchService = searchService;
    _scheduledServiceService = scheduledServiceService;
  }

  @Override
  public SearchResult getRouteResultForRegion(RouteBean routeBean) { 
    List<String> polylines = new ArrayList<String>();
    
    StopsForRouteBean stopsForRoute = _transitDataService.getStopsForRoute(routeBean.getId());

    List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
    for (StopGroupingBean stopGroupingBean : stopGroupings) {
      for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
        NameBean name = stopGroupBean.getName();
        String type = name.getType();

        if (!type.equals("destination"))
          continue;
        
        for(EncodedPolylineBean polyline : stopGroupBean.getPolylines()) {
          polylines.add(polyline.getPoints());
        }
      }
    }

    return new RouteInRegionResult(routeBean, polylines);
  }
  
  @Override
  public SearchResult getRouteResult(RouteBean routeBean) {    
    List<RouteDirection> directions = new ArrayList<RouteDirection>();
    
    StopsForRouteBean stopsForRoute = _transitDataService.getStopsForRoute(routeBean.getId());

    List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
    for (StopGroupingBean stopGroupingBean : stopGroupings) {
      for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
        NameBean name = stopGroupBean.getName();
        String type = name.getType();

        if (!type.equals("destination"))
          continue;
        
        List<String> polylines = new ArrayList<String>();
        for(EncodedPolylineBean polyline : stopGroupBean.getPolylines()) {
          polylines.add(polyline.getPoints());
        }

        Boolean hasUpcomingScheduledService = 
            _scheduledServiceService.hasUpcomingScheduledService(routeBean, stopGroupBean);

        directions.add(new RouteDirection(stopGroupBean, polylines, null, hasUpcomingScheduledService));
      }
    }

    return new RouteResult(routeBean, directions);
  }

  @Override
  public SearchResult getStopResult(StopBean stopBean) {
    StopBean stop = _transitDataService.getStop(stopBean.getId());    

    List<RouteAtStop> routesAtStop = new ArrayList<RouteAtStop>();
    
    for(RouteBean routeBean : stop.getRoutes()) {
      StopsForRouteBean stopsForRoute = _transitDataService.getStopsForRoute(routeBean.getId());

      List<RouteDirection> directions = new ArrayList<RouteDirection>();
      List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
      for (StopGroupingBean stopGroupingBean : stopGroupings) {
        for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
          NameBean name = stopGroupBean.getName();
          String type = name.getType();

          if (!type.equals("destination"))
            continue;
        
          List<String> polylines = new ArrayList<String>();
          for(EncodedPolylineBean polyline : stopGroupBean.getPolylines()) {
            polylines.add(polyline.getPoints());
          }

          directions.add(new RouteDirection(stopGroupBean, polylines, null, null));
        }
      }

      RouteAtStop routeAtStop = new RouteAtStop(routeBean, directions);
      routesAtStop.add(routeAtStop);
    }

    return new StopResult(stop, routesAtStop);
  }

  @Override
  public SearchResult getGeocoderResult(NycGeocoderResult geocodeResult) {
    List<SearchResult> routesNearby = null;
    
    if(geocodeResult.isRegion()) {
       routesNearby = _searchService.getRouteResultsStoppingWithinRegion(geocodeResult.getBounds(), this);
    } else {
      routesNearby = _searchService.getRouteResultsStoppingNearPoint(geocodeResult.getLatitude(), 
          geocodeResult.getLongitude(), this);
    }
    
    return new GeocodeResult(geocodeResult, routesNearby);   
  }

}
