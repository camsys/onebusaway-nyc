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
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItemWithStops;
import org.onebusaway.nyc.presentation.model.search.RouteItem;
import org.onebusaway.nyc.presentation.model.search.RouteSearchResult;
import org.onebusaway.nyc.presentation.model.search.StopItem;
import org.onebusaway.nyc.presentation.service.ModelFactory;
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
  
  private ModelFactory _modelFactory = new DefaultModelFactory();

  @Autowired
  private TransitDataService _transitDataService;
  
  @Autowired
  private ServiceAreaService _serviceArea;
    
  public void setModelFactory(ModelFactory factory) {
    _modelFactory = factory;
  }

  @Override
  public List<RouteItem> itemsForLocation(Double latitude, Double longitude) {
    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(latitude, longitude, _distanceToRoutes);

    return itemsForLocation(bounds);
  }    
  
  @Override
  public List<RouteItem> itemsForLocation(CoordinateBounds bounds) {
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(bounds);
    queryBean.setMaxCount(100);
    
    RoutesBean routes = _transitDataService.getRoutes(queryBean);

    List<RouteItem> results = routesBeanToRouteItem(routes);

    return results;
  }
  
  @Override
  public List<RouteSearchResult> makeResultFor(String route) {
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(_serviceArea.getServiceArea());
    queryBean.setQuery(route);
    queryBean.setMaxCount(100);

    RoutesBean routes = _transitDataService.getRoutes(queryBean);

    List<RouteSearchResult> results = routesBeanToRouteSearchResults(routes);

    return results;
  }

  private List<RouteSearchResult> routesBeanToRouteSearchResults(RoutesBean routesBean) {
    ArrayList<RouteSearchResult> results = new ArrayList<RouteSearchResult>();

    List<RouteBean> routeBeans = routesBean.getRoutes();
    for(RouteBean routeBean : routeBeans) {
      List<RouteDestinationItemWithStops> destinations = 
          getRouteDestinationItemWithStopsForRoute(routeBean);
      
      results.add(_modelFactory.getSearchResultModelFor(routeBean, destinations));
    }
    
    return results;
  } 

  private List<RouteItem> routesBeanToRouteItem(RoutesBean routesBean) {
    ArrayList<RouteItem> results = new ArrayList<RouteItem>();

    List<RouteBean> routeBeans = routesBean.getRoutes();
    for(RouteBean routeBean : routeBeans) {
      List<RouteDestinationItem> destinations = getRouteDestinationItemsForRoute(routeBean);

      results.add(_modelFactory.getRouteItemModelFor(routeBean, destinations));
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

        // polyline--seems to always just be one?
        String polyline = null;
        List<EncodedPolylineBean> polylines = stopGroupBean.getPolylines();
        if(polylines.size() > 0)
          polyline = polylines.get(0).getPoints();

        // add data for all available headsigns
        for(String headsign: headsigns) {
          RouteDestinationItem routeDestination = 
              _modelFactory.getRouteDestinationItemModelFor(headsign, directionId, polyline);

          output.add(routeDestination);
        }
      }
    }
    
    return output;
  }
  
  private List<RouteDestinationItemWithStops> getRouteDestinationItemWithStopsForRoute(RouteBean routeBean) {
    List<RouteDestinationItemWithStops> output = new ArrayList<RouteDestinationItemWithStops>();

    List<RouteDestinationItem> destinations = getRouteDestinationItemsForRoute(routeBean);
    for(RouteDestinationItem destination : destinations) {
      List<StopBean> stopBeansForThisDirection = 
          getStopBeansForRouteAndHeadsign(routeBean, destination.getHeadsign());

      List<StopItem> stopItems = null;
      if(stopBeansForThisDirection != null) {     
        stopItems = new ArrayList<StopItem>();
        for(StopBean stopBean : stopBeansForThisDirection) {
          StopItem stopItem = _modelFactory.getStopItemModelFor(stopBean);
          stopItems.add(stopItem);
        }
      }
      
      RouteDestinationItemWithStops routeDestination = 
          _modelFactory.getRouteDestinationItemWithStopsModelFor(destination, stopItems);

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