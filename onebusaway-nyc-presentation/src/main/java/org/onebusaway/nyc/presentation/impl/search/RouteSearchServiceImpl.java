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
import org.onebusaway.nyc.presentation.impl.DefaultSearchModelFactory;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.SearchModelFactory;
import org.onebusaway.nyc.presentation.service.search.RouteSearchService;
import org.onebusaway.presentation.services.ServiceAreaService;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RoutesBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.TripStopTimesBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

@Component
public class RouteSearchServiceImpl implements RouteSearchService {
  
  // when querying for routes from a lat/lng, use this distance in meters
  private double _distanceToRoutes = 100;

  // number of periods to increment, looking for a trip used to order stops on a given route/direction
  private int _periodsToTryToFindATrip = 24;
  
  private SearchModelFactory _modelFactory = new DefaultSearchModelFactory();

  @Autowired
  private TransitDataService _transitDataService;
  
  @Autowired
  private ServiceAreaService _serviceArea;
    
  public void setModelFactory(SearchModelFactory factory) {
    _modelFactory = factory;
  }

  @Override
  public List<RouteResult> resultsForLocation(Double latitude, Double longitude) {
    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(latitude, longitude, _distanceToRoutes);

    return resultsForLocation(bounds);
  }    
  
  @Override
  public List<RouteResult> resultsForLocation(CoordinateBounds bounds) {
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(bounds);
    queryBean.setMaxCount(100);
    
    RoutesBean routes = _transitDataService.getRoutes(queryBean);

    List<RouteResult> results = routesBeanToRouteItem(routes);

    return results;
  }

  @Override
  public RouteResult makeResultForRouteId(String routeId) {
    RouteBean routeBean = _transitDataService.getRouteForId(routeId);
    
    if(routeBean != null) {
      List<RouteDestinationItem> destinations = 
          getRouteDestinationItemWithStopsForRoute(routeBean);
      
      RouteResult routeSearchResult = _modelFactory.getRouteSearchResultModel();
      routeSearchResult.setRouteBean(routeBean);
      routeSearchResult.setDestinations(destinations);
      
      return routeSearchResult;
    }
    
    return null;
  }
  
  @Override
  public List<RouteResult> resultsForQuery(String routeQuery) {
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(_serviceArea.getServiceArea());
    queryBean.setQuery(routeQuery);
    queryBean.setMaxCount(100);

    RoutesBean routes = _transitDataService.getRoutes(queryBean);

    List<RouteResult> results = routesBeanToRouteSearchResults(routes);

    return results;
  }

  private List<RouteResult> routesBeanToRouteSearchResults(RoutesBean routesBean) {
    ArrayList<RouteResult> results = new ArrayList<RouteResult>();

    List<RouteBean> routeBeans = routesBean.getRoutes();
    for(RouteBean routeBean : routeBeans) {
      List<RouteDestinationItem> destinations = 
          getRouteDestinationItemWithStopsForRoute(routeBean);
      
      RouteResult routeSearchResult = _modelFactory.getRouteSearchResultModel();
      routeSearchResult.setRouteBean(routeBean);
      routeSearchResult.setDestinations(destinations);
      
      results.add(routeSearchResult);
    }
    
    return results;
  } 

  private List<RouteResult> routesBeanToRouteItem(RoutesBean routesBean) {
    ArrayList<RouteResult> results = new ArrayList<RouteResult>();

    List<RouteBean> routeBeans = routesBean.getRoutes();
    for(RouteBean routeBean : routeBeans) {
      List<RouteDestinationItem> destinations = getRouteDestinationItemsForRoute(routeBean);

      RouteResult routeSearchResult = _modelFactory.getRouteSearchResultModel();
      routeSearchResult.setRouteBean(routeBean);
      routeSearchResult.setDestinations(destinations);
      
      results.add(routeSearchResult);
    }
    
    return results;
  } 
  
  private List<RouteDestinationItem> getRouteDestinationItemsForRoute(RouteBean routeBean) {
    List<RouteDestinationItem> output = new ArrayList<RouteDestinationItem>();
    
    StopsForRouteBean stopsForRoute = _transitDataService.getStopsForRoute(routeBean.getId());
    List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
    for (StopGroupingBean stopGroupingBean : stopGroupings) {
      List<StopGroupBean> stopGroups = stopGroupingBean.getStopGroups();
      for (StopGroupBean stopGroupBean : stopGroups) {
        NameBean name = stopGroupBean.getName();
        String type = name.getType();
        if (!type.equals("destination"))
          continue;
        
        List<String> headsigns = name.getNames();
        String directionId = stopGroupBean.getId();

        // polylines
        List<String> polylines = new ArrayList<String>();
        for(EncodedPolylineBean polyline : stopGroupBean.getPolylines()) {
          polylines.add(polyline.getPoints());
        }

        // add data for all available headsigns
        for(String headsign: headsigns) {
          RouteDestinationItem routeDestination = _modelFactory.getRouteDestinationItemModel();
          routeDestination.setHeadsign(headsign);
          routeDestination.setDirectionId(directionId);
          routeDestination.setPolylines(polylines);

          output.add(routeDestination);
        }
      }
    }
    
    return output;
  }
  
  private List<RouteDestinationItem> getRouteDestinationItemWithStopsForRoute(RouteBean routeBean) {
    List<RouteDestinationItem> output = new ArrayList<RouteDestinationItem>();

    List<RouteDestinationItem> destinations = getRouteDestinationItemsForRoute(routeBean);
    for(RouteDestinationItem destination : destinations) {
      List<StopBean> stopBeansForThisDirection = 
          getStopBeansForRouteAndHeadsign(routeBean, destination.getHeadsign());

      List<StopResult> stopItems = null;
      
      if(stopBeansForThisDirection != null) {     
        stopItems = new ArrayList<StopResult>();
        for(StopBean stopBean : stopBeansForThisDirection) {
          StopResult stopItem = _modelFactory.getStopSearchResultModel();
          stopItem.setStopBean(stopBean);
          stopItems.add(stopItem);
        }
      }

      RouteDestinationItem routeDestination = _modelFactory.getRouteDestinationItemModel();
      routeDestination.setDirectionId(destination.getDirectionId());
      routeDestination.setHeadsign(destination.getHeadsign());
      routeDestination.setPolylines(destination.getPolylines());
      routeDestination.setStops(stopItems);

      output.add(routeDestination);
    }
    
    return output;
  }

  // get ordered stop beans for a given route + headsign, using a trip sampled from the schedule.
  // we try to look at noon on the current day, and then search in +8 hour blocks
  private List<StopBean> getStopBeansForRouteAndHeadsign(RouteBean routeBean, String headsign) {
    List<StopBean> output = new ArrayList<StopBean>();

    // start looking at noon today
    Calendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTime(new Date());
    gregorianCalendar.set(Calendar.HOUR, 12);
    gregorianCalendar.set(Calendar.MINUTE, 0);
    gregorianCalendar.set(Calendar.SECOND, 0);
    gregorianCalendar.set(Calendar.AM_PM, Calendar.PM);
    
    int tries = _periodsToTryToFindATrip;
    long time = gregorianCalendar.getTimeInMillis();
    while(tries-- > 0) {
      for(TripDetailsBean tripDetails : getAllTripsForRoute(routeBean, time).getList()) {
        TripBean tripBean = tripDetails.getTrip();
        if(!tripBean.getRoute().equals(routeBean) || !tripBean.getTripHeadsign().equals(headsign))
          continue;
        
        TripStopTimesBean tripStopTimes = tripDetails.getSchedule();
        for(TripStopTimeBean tripStopTimeBean : tripStopTimes.getStopTimes()) {
          output.add(tripStopTimeBean.getStop());
        }
        
        return output;
      }

      // try the next search period (8h)
      time += 8 * 60 * 60 * 1000;
    }
    
    return null;
  }
  
  private ListBean<TripDetailsBean> getAllTripsForRoute(RouteBean routeBean, long time) {
    TripsForRouteQueryBean tripRouteQueryBean = new TripsForRouteQueryBean();
    tripRouteQueryBean.setRouteId(routeBean.getId());
    tripRouteQueryBean.setMaxCount(100);
    tripRouteQueryBean.setTime(time);

    TripDetailsInclusionBean inclusionBean = new TripDetailsInclusionBean();
    inclusionBean.setIncludeTripBean(true);
    inclusionBean.setIncludeTripSchedule(true);
    tripRouteQueryBean.setInclusion(inclusionBean);

    return _transitDataService.getTripsForRoute(tripRouteQueryBean);
  }

}