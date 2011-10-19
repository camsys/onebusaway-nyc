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
import org.onebusaway.nyc.presentation.model.EnumDisplayMedia;
import org.onebusaway.nyc.presentation.model.EnumFormattingContext;
import org.onebusaway.nyc.presentation.model.realtime_data.DistanceAway;
import org.onebusaway.nyc.presentation.model.realtime_data.RouteItem;
import org.onebusaway.nyc.presentation.model.search.StopSearchResult;
import org.onebusaway.presentation.services.ServiceAreaService;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Component
public class StopSearchServiceImpl {
  
  // when querying for stops from a lat/lng, use this distance in meters
  private double _distanceToStops = 100;
  
  @Autowired
  private TransitDataService _transitDataService;
  
  @Autowired
  private ServiceAreaService _serviceArea;
  
  @Autowired
  private SearchSupport _searchSupport;
    
  public void setTime(Date time) {
    _searchSupport.setTime(time);
  }
  
  public List<StopSearchResult> resultsForLocation(Double lat, Double lng, EnumDisplayMedia media) {
    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(lat, lng, _distanceToStops);
    
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(bounds);
    queryBean.setMaxCount(100);
    
    StopsBean stops = _transitDataService.getStops(queryBean);

    List<StopSearchResult> results = stopsBeanToStopResults(stops, media);

    return results;
  }

  public StopSearchResult makeResultFor(String stopId, EnumDisplayMedia media) {
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(_serviceArea.getServiceArea());
    queryBean.setQuery(stopId);
    queryBean.setMaxCount(100);

    StopsBean stops = _transitDataService.getStops(queryBean);

    List<StopSearchResult> results = stopsBeanToStopResults(stops, media);

    if(results.size() > 0) {
      return results.get(0);
    } else
      return null;
  }

  private List<StopSearchResult> stopsBeanToStopResults(StopsBean stopsBean, EnumDisplayMedia media) {
    ArrayList<StopSearchResult> results = new ArrayList<StopSearchResult>();
    for(StopBean stopBean : stopsBean.getStops()) {
      StopSearchResult stopSearchResult = makeStopSearchResult(stopBean, media);
      results.add(stopSearchResult);
    }
    return results;
  }
  
  private StopSearchResult makeStopSearchResult(StopBean stopBean, EnumDisplayMedia m) {
    List<RouteItem> availableRoutes = getRouteItemsForStop(stopBean);
    List<NaturalLanguageStringBean> serviceAlerts = getServiceAlertsForStop(stopBean);
    
    StopSearchResult stopSearchResult = new StopSearchResult(
        stopBean.getId(), stopBean.getName(), stopBean.getLat(), stopBean.getLon(), stopBean.getDirection(), 
        availableRoutes, serviceAlerts);

    return stopSearchResult;    
  }
    
  private List<RouteItem> getRouteItemsForStop(StopBean stopBean) {
    List<RouteItem> output = new ArrayList<RouteItem>();
    
    for (RouteBean routeBean : stopBean.getRoutes()) {
      String shortName = routeBean.getShortName();
      String longName = routeBean.getLongName();

      // iterate through headsigns that appear at this stop on the given route
      for(String headsign : getHeadsignsForStopAndRoute(stopBean, routeBean)) {
        String directionId = getDirectionIdForStopAndHeadsign(stopBean, headsign);

        List<DistanceAway> distanceAways = _searchSupport.getDistanceAwaysForStopAndHeadsign(
                                              stopBean, headsign, 
                                              false, EnumFormattingContext.STOP);

        if (distanceAways != null)
          Collections.sort(distanceAways);

        RouteItem availableRoute = new RouteItem(shortName, longName, headsign, 
            directionId, distanceAways);

        output.add(availableRoute);
      }
    }    
    
    return output;
  }    
    
  @Cacheable
  private String getDirectionIdForStopAndHeadsign(StopBean stopBean, String headsign) {
    List<ArrivalAndDepartureBean> arrivalsAndDepartures = 
        _searchSupport.getArrivalsAndDeparturesForStop(stopBean);

    for (ArrivalAndDepartureBean arrivalAndDepartureBean : arrivalsAndDepartures) {
      TripBean tripBean = arrivalAndDepartureBean.getTrip();
      if(tripBean.getTripHeadsign().equals(headsign))
        return tripBean.getDirectionId();
    }
    
    return null;
  }

  private List<String> getHeadsignsForStopAndRoute(StopBean stopBean, RouteBean routeBean) {
    ArrayList<String> headsigns = new ArrayList<String>();
    
    List<ArrivalAndDepartureBean> arrivalsAndDepartures = 
        _searchSupport.getArrivalsAndDeparturesForStop(stopBean);

    for (ArrivalAndDepartureBean arrivalAndDepartureBean : arrivalsAndDepartures) {
      TripBean tripBean = arrivalAndDepartureBean.getTrip();
      if(tripBean.getRoute().equals(routeBean)) {
        String headsign = tripBean.getTripHeadsign();

        if(!headsigns.contains(headsign))
          headsigns.add(headsign);
      }
    }
    
    return headsigns;
  }
  
  private List<NaturalLanguageStringBean> getServiceAlertsForStop(StopBean stopBean) {
    HashMap<String, List<NaturalLanguageStringBean>> serviceAlertIdsToDescriptions = 
        new HashMap<String, List<NaturalLanguageStringBean>>();
    
    List<ArrivalAndDepartureBean> arrivalsAndDepartures = 
        _searchSupport.getArrivalsAndDeparturesForStop(stopBean);
    
    for (ArrivalAndDepartureBean arrivalAndDepartureBean : arrivalsAndDepartures) {
      TripStatusBean tripStatusBean = arrivalAndDepartureBean.getTripStatus();      
      if(tripStatusBean == null || tripStatusBean.getSituations() == null)
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
