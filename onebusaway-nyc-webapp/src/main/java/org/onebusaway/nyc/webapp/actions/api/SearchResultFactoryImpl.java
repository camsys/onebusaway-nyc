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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.search.SearchResultFactory;
import org.onebusaway.nyc.presentation.service.search.SearchService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
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

import uk.org.siri.siri.VehicleActivityStructure;

public class SearchResultFactoryImpl implements SearchResultFactory {

  private SearchService _searchService;

  private NycTransitDataService _nycTransitDataService;

  private RealtimeService _realtimeService;

  public SearchResultFactoryImpl(SearchService searchService, NycTransitDataService nycTransitDataService, RealtimeService realtimeService) {
    _searchService = searchService;
    _nycTransitDataService = nycTransitDataService;
    _realtimeService = realtimeService;
  }

  @Override
  public SearchResult getRouteResultForRegion(RouteBean routeBean) { 
    List<String> polylines = new ArrayList<String>();
    
    StopsForRouteBean stopsForRoute = _nycTransitDataService.getStopsForRoute(routeBean.getId());

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
    
    StopsForRouteBean stopsForRoute = _nycTransitDataService.getStopsForRoute(routeBean.getId());

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
            _nycTransitDataService.routeHasUpcomingScheduledService(System.currentTimeMillis(), routeBean.getId(), stopGroupBean.getId());

        // if there are buses on route, always have "scheduled service"
        List<VehicleActivityStructure> vehiclesOnRoute = 
        		_realtimeService.getVehicleActivityForRoute(routeBean.getId(), stopGroupBean.getId(), 0);
        
        if(!vehiclesOnRoute.isEmpty()) {
        	hasUpcomingScheduledService = true;
        }
        
        directions.add(new RouteDirection(stopGroupBean, polylines, null, hasUpcomingScheduledService));
      }
    }

    return new RouteResult(routeBean, directions);
  }

  @Override
  public SearchResult getStopResult(StopBean stopBean, Set<String> routeIdFilter) {
    List<RouteAtStop> routesAtStop = new ArrayList<RouteAtStop>();
    
    for(RouteBean routeBean : stopBean.getRoutes()) {
      StopsForRouteBean stopsForRoute = _nycTransitDataService.getStopsForRoute(routeBean.getId());

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

          Boolean hasUpcomingScheduledService = 
              _nycTransitDataService.stopHasUpcomingScheduledService(System.currentTimeMillis(), stopBean.getId(), routeBean.getId(), stopGroupBean.getId());

          // if there are buses on route, always have "scheduled service"
          List<VehicleActivityStructure> vehiclesOnRoute = 
          		_realtimeService.getVehicleActivityForRoute(routeBean.getId(), stopGroupBean.getId(), 0);
          
          if(!vehiclesOnRoute.isEmpty()) {
          	hasUpcomingScheduledService = true;
          }
          
          directions.add(new RouteDirection(stopGroupBean, polylines, null, hasUpcomingScheduledService));
        }
      }

      RouteAtStop routeAtStop = new RouteAtStop(routeBean, directions);
      routesAtStop.add(routeAtStop);
    }

    return new StopResult(stopBean, routesAtStop);
  }

  @Override
  public SearchResult getGeocoderResult(NycGeocoderResult geocodeResult, Set<String> routeIdFilter) {
    List<SearchResult> routesNearby = null;
    
    if(geocodeResult.isRegion()) {
       routesNearby = _searchService.findRoutesStoppingWithinRegion(geocodeResult.getBounds(), this).getMatches();
    } else {
      routesNearby = _searchService.findRoutesStoppingNearPoint(geocodeResult.getLatitude(), 
          geocodeResult.getLongitude(), this).getMatches();
    }
    
    return new GeocodeResult(geocodeResult, routesNearby);   
  }

}
