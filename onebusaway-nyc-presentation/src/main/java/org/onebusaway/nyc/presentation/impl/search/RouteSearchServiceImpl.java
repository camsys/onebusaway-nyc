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
import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.presentation.impl.sort.StopBeanComparator;
import org.onebusaway.nyc.presentation.model.EnumDisplayMedia;
import org.onebusaway.nyc.presentation.model.EnumFormattingContext;
import org.onebusaway.nyc.presentation.model.realtime_data.DistanceAway;
import org.onebusaway.nyc.presentation.model.realtime_data.StopItem;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteSearchResult;
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
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RouteSearchServiceImpl {
  
  private double _distanceToRoutes = 100;

  @Autowired
  private TransitDataService _transitDataService;
  
  @Autowired
  private ServiceAreaService _serviceArea;
  
  @Autowired
  private SearchSupport _searchSupport;

  private Date _now = null;
  
  public void setTime(Date time) {
    _now = time;
    _searchSupport.setTime(time);
  }

  private long getTime() {
    if(_now != null)
      return _now.getTime();
    else
      return System.currentTimeMillis();
  }
  
  public List<RouteSearchResult> resultsForLocation(Double lat, Double lng, EnumDisplayMedia media) {
    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(lat, lng, _distanceToRoutes);
    
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(bounds);
    queryBean.setMaxCount(100);
    
    RoutesBean routes = _transitDataService.getRoutes(queryBean);

    List<RouteSearchResult> results = routesBeanToRouteResults(routes, media);

    return results;
  }
  
  public List<RouteSearchResult> makeResultFor(String route, EnumDisplayMedia media) {
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(_serviceArea.getServiceArea());
    queryBean.setQuery(route);
    queryBean.setMaxCount(100);

    RoutesBean routes = _transitDataService.getRoutes(queryBean);

    List<RouteSearchResult> results = routesBeanToRouteResults(routes, media);

    return results;
  }

  private List<RouteSearchResult> routesBeanToRouteResults(RoutesBean routesBean, EnumDisplayMedia media) {
    ArrayList<RouteSearchResult> results = new ArrayList<RouteSearchResult>();
    for(RouteBean routeBean : routesBean.getRoutes()) {
      RouteSearchResult routeSearchResults = makeRouteSearchResult(routeBean, media);
      results.add(routeSearchResults);
    }
    return results;
  }
  
  private RouteSearchResult makeRouteSearchResult(RouteBean routeBean, EnumDisplayMedia m) {
    String routeId = routeBean.getId();
    String routeLongName = routeBean.getLongName();

    List<RouteDestinationItem> allDestinationsForThisRoute = 
        new ArrayList<RouteDestinationItem>();

    Map<String, String> headsignToEncodedPolylineMap = getHeadsignToPolylineMapForRoute(routeBean);
    
    Map<String, List<StopBean>> headsignToStopBeanMap = getHeadsignToStopBeanMapForRoute(routeBean);

    for(String headsign : getHeadsignsForRoute(routeBean)) {
      String directionId = getDirectionIdForRouteAndHeadsign(routeBean, headsign);

      // alerts
      List<NaturalLanguageStringBean> alerts = 
          getServiceAlertsForRouteAndDirection(routeBean, directionId);

      // destinations
      List<StopItem> stopItemsList = 
          getStopItemsForStopBeanListAndHeadsign(headsignToStopBeanMap.get(headsign), headsign);

      String polylines = headsignToEncodedPolylineMap.get(headsign);
      
      RouteDestinationItem routeDestination = 
          new RouteDestinationItem(headsign, directionId, stopItemsList, polylines, alerts);

      allDestinationsForThisRoute.add(routeDestination);
    }
    
    RouteSearchResult routeSearchResult = new RouteSearchResult(routeId, routeLongName, 
        allDestinationsForThisRoute);

    return routeSearchResult;
  }
  
  private List<StopItem> getStopItemsForStopBeanListAndHeadsign(List<StopBean> stopBeansList, 
      String headsign) {

    List<StopItem> stopItemsList = new ArrayList<StopItem>();
    
    for (StopBean stopBean : stopBeansList) {
      List<DistanceAway> distances = 
          _searchSupport.getDistanceAwaysForStopAndHeadsign(stopBean, headsign, 
              true, EnumFormattingContext.ROUTE);

      if(distances == null)
        distances = Collections.emptyList();
      
      // if there is more than one vehicle stopping at this stop, sort the
      // vehicles by distance away from this stop
      Collections.sort(distances);

      StopItem stopItem = new StopItem(stopBean, distances);
      stopItemsList.add(stopItem);
    }
    
    return stopItemsList;
  }
  
  @Cacheable
  private String getDirectionIdForRouteAndHeadsign(RouteBean routeBean, String headsign) {
    for (TripDetailsBean tripDetailsBean : getAllTripsForRoute(routeBean).getList()) {
      TripBean tripBean = tripDetailsBean.getTrip();
      if(tripBean.getRoute().equals(routeBean) && tripBean.getTripHeadsign().equals(headsign)) {
        return tripBean.getDirectionId();
      }
    }
   
    return null;
  }
  
  private List<String> getHeadsignsForRoute(RouteBean routeBean) {
    List<String> headsigns = new ArrayList<String>();
    
    for (TripDetailsBean tripDetailsBean : getAllTripsForRoute(routeBean).getList()) {
      TripBean tripBean = tripDetailsBean.getTrip();
      if(tripBean.getRoute().equals(routeBean)) {
        String headsign = tripBean.getTripHeadsign();

        if(!headsigns.contains(headsign))
          headsigns.add(headsign);
      }
    }
   
    return headsigns;
  }
  
  private Map<String, String> getHeadsignToPolylineMapForRoute(RouteBean routeBean) {
    Map<String, String> headsignToEncodedPolylines = new HashMap<String, String>();

    StopsForRouteBean stopsForRoute = _transitDataService.getStopsForRoute(routeBean.getId());

    List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
    for (StopGroupingBean stopGroupingBean : stopGroupings) {
      List<StopGroupBean> stopGroups = stopGroupingBean.getStopGroups();
      for (StopGroupBean stopGroupBean : stopGroups) {
        NameBean name = stopGroupBean.getName();
        String type = name.getType();
        List<String> headsigns = name.getNames();
        
        if (!type.equals("destination"))
          continue;

        List<EncodedPolylineBean> polylines = stopGroupBean.getPolylines();
        
        if(polylines.size() > 0) {
          // everything seems to have just one polyline?
          String polyline = polylines.get(0).getPoints();
        
          // (some directions have multiple active headsigns...)
          for(String headsign : headsigns)
            headsignToEncodedPolylines.put(headsign, polyline);
        }
      }
    }
    
    return headsignToEncodedPolylines;
  }
  
  private Map<String, List<StopBean>> getHeadsignToStopBeanMapForRoute(RouteBean routeBean) {
    Map<String, List<StopBean>> headsignToStopBeans = new HashMap<String, List<StopBean>>();

    StopsForRouteBean stopsForRoute = _transitDataService.getStopsForRoute(routeBean.getId());
    List<StopBean> stopBeansList = stopsForRoute.getStops();

    List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
    for (StopGroupingBean stopGroupingBean : stopGroupings) {
      List<StopGroupBean> stopGroups = stopGroupingBean.getStopGroups();
      for (StopGroupBean stopGroupBean : stopGroups) {
        NameBean name = stopGroupBean.getName();
        String directionId = stopGroupBean.getId();
        String type = name.getType();
        List<String> headsigns = name.getNames();
        
        if (!type.equals("destination"))
          continue;

        List<StopBean> stopsForHeadsign = new ArrayList<StopBean>();
        Set<String> stopIds = new HashSet<String>(stopGroupBean.getStopIds());

        for (StopBean stopBean : stopBeansList) {
          String stopBeanId = stopBean.getId();
          if (stopIds.contains(stopBeanId))
            stopsForHeadsign.add(stopBean);
        }
        
        // sort stops in the order they appear in a trip in the given direction
        HashMap<String, Double> stopIdToDistanceSortMap = 
            getStopBeanSortMap(routeBean, directionId);

        Collections.sort(stopsForHeadsign, new StopBeanComparator(stopIdToDistanceSortMap));

        // (some directions have multiple active headsigns...)
        for(String headsign : headsigns)
          headsignToStopBeans.put(headsign, stopsForHeadsign);
      }
    }
    
    return headsignToStopBeans;
  }
  
  // generate a map of stop IDs->distance along route for use in sorting stop beans
  @Cacheable
  private HashMap<String, Double> getStopBeanSortMap(RouteBean routeBean, String directionId) {
    HashMap<String, Double> output = new HashMap<String, Double>();

    for(TripDetailsBean tripDetails : getAllTripsForRoute(routeBean).getList()) {
      TripBean tripBean = tripDetails.getTrip();
      if(!tripBean.getRoute().equals(routeBean) || !tripBean.getDirectionId().equals(directionId))
        continue;
      
      TripStopTimesBean tripStopTimes = tripDetails.getSchedule();
      for(TripStopTimeBean tripStopTimeBean : tripStopTimes.getStopTimes()) {
        output.put(tripStopTimeBean.getStop().getId(), tripStopTimeBean.getDistanceAlongTrip());
      }
    }
    
    return output;
  }
  
  private ListBean<TripDetailsBean> getAllTripsForRoute(RouteBean routeBean) {
    TripsForRouteQueryBean tripRouteQueryBean = new TripsForRouteQueryBean();
    tripRouteQueryBean.setRouteId(routeBean.getId());
    tripRouteQueryBean.setMaxCount(100);
    tripRouteQueryBean.setTime(this.getTime());

    TripDetailsInclusionBean inclusionBean = new TripDetailsInclusionBean();
    inclusionBean.setIncludeTripBean(true);
    inclusionBean.setIncludeTripSchedule(true);
    inclusionBean.setIncludeTripStatus(true);
    tripRouteQueryBean.setInclusion(inclusionBean);

    return _transitDataService.getTripsForRoute(tripRouteQueryBean);
  }    

  private List<NaturalLanguageStringBean> getServiceAlertsForRouteAndDirection(RouteBean routeBean, 
      String directionId) {

    HashMap<String, List<NaturalLanguageStringBean>> serviceAlertIdsToDescriptions = 
        new HashMap<String, List<NaturalLanguageStringBean>>();

    for (TripDetailsBean tripDetailsBean : getAllTripsForRoute(routeBean).getList()) {
      TripStatusBean tripStatusBean = tripDetailsBean.getStatus();
      if(tripStatusBean == null || tripStatusBean.getSituations() == null 
          || !tripStatusBean.getActiveTrip().getDirectionId().equals(directionId)) 
        continue;

      for(ServiceAlertBean serviceAlert : tripStatusBean.getSituations()) {
        serviceAlertIdsToDescriptions.put(serviceAlert.getId(), serviceAlert.getDescriptions());
      }
    }
    
    // merge lists from unique service alerts together
    List<NaturalLanguageStringBean> output = new ArrayList<NaturalLanguageStringBean>();
    for(Collection<NaturalLanguageStringBean> descriptions : serviceAlertIdsToDescriptions.values()) {
      output.addAll(descriptions);
    }

    return output;
  }
}