package org.onebusaway.nyc.webapp.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onebusaway.geocoder.model.GeocoderResult;
import org.onebusaway.geocoder.services.GeocoderService;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.webapp.model.AvailableRoute;
import org.onebusaway.nyc.webapp.model.DistanceAway;
import org.onebusaway.nyc.webapp.model.search.RouteSearchResult;
import org.onebusaway.nyc.webapp.model.search.SearchResult;
import org.onebusaway.nyc.webapp.model.search.StopSearchResult;
import org.onebusaway.nyc.webapp.service.NycSearchService;
import org.onebusaway.presentation.services.ServiceAreaService;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RoutesBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

public class NycSearchServiceImpl implements NycSearchService {
  
  private final static Pattern routePattern = Pattern.compile("(?:[BMQS]|BX)[0-9]+");
  
  // when querying for stops from a lat/lng, use this distance in meters
  private double distanceToStops = 200;
  
  private WebappIdParser idParser = new WebappIdParser();

  @Autowired
  private TransitDataService transitService;
  
  @Autowired
  private GeocoderService geocoderService;
  
  @Autowired
  private ServiceAreaService serviceArea;

  @Override
  public List<SearchResult> search(String q) {
    List<SearchResult> results = new ArrayList<SearchResult>();
    
    if (q == null)
      return results;
    q = q.trim();
    if (q.isEmpty())
      return results;
    
    String[] fields = q.split(" ");
    if (fields.length == 1) {
      // we are probably a query for a route or a stop
      if (isStop(q)) {
        SearchQueryBean queryBean = makeSearchQuery(q);
        StopsBean stops = transitService.getStops(queryBean);
        
        for (StopBean stopBean : stops.getStops()) {
          StopSearchResult stopSearchResult = makeStopSearchResult(stopBean);
          results.add(stopSearchResult);
        }
      } else if (isRoute(q)) {
        SearchQueryBean queryBean = makeSearchQuery(q);
        RoutesBean routes = transitService.getRoutes(queryBean);
        
        for (RouteBean routeBean : routes.getRoutes()) {
          String routeId = routeBean.getId();
          String shortName = routeBean.getShortName();
          String longName = routeBean.getLongName();
          // FIXME last update time
          String lastUpdate = "one minute ago";
          RouteSearchResult routeSearchResult = new RouteSearchResult(routeId, shortName, longName, lastUpdate);
          results.add(routeSearchResult);
        }
      }
    } else {
      // we should be either an intersection and route or an intersection
      
      // if we're a route, set route and the geocoder query appropriately
      String route = null;
      String queryNoRoute = null;
      if (isRoute(fields[0])) {
        route = fields[0];
        StringBuilder sb = new StringBuilder(32);
        for (int i = 1; i < fields.length; i++) {
          sb.append(fields[i]);
          sb.append(" ");
        }
        queryNoRoute = sb.toString().trim();
      }
      else if (isRoute(fields[fields.length-1])) {
        route = fields[fields.length-1];
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < fields.length-1; i++) {
          sb.append(fields[i]);
          sb.append(" ");
        }
        queryNoRoute = sb.toString().trim();
      }
      
      if (route != null) {
        // we are a route and intersection
        List<StopsBean> stopsList = fetchStopsFromGeocoder(queryNoRoute);
        for (StopsBean stopsBean : stopsList) {
          for (StopBean stopBean : stopsBean.getStops()) {
            boolean useStop = false;
            List<RouteBean> routes = stopBean.getRoutes();
            for (RouteBean routeBean : routes) {
              String idNoAgency = idParser.parseIdWithoutAgency(routeBean.getId());
              if (idNoAgency.equals(route)) {
                useStop = true;
                break;
              }
            }
            if (useStop) {
              StopSearchResult stopSearchResult = makeStopSearchResult(stopBean);
              results.add(stopSearchResult);   
            }
          }
        }
      } else {
        // try an intersection search
        List<StopsBean> stopsList = fetchStopsFromGeocoder(q);
        for (StopsBean stopsBean : stopsList) {
          for (StopBean stopBean : stopsBean.getStops()) {
            StopSearchResult stopSearchResult = makeStopSearchResult(stopBean);
            results.add(stopSearchResult);            
          }
        }
      }
    }

    
      return sortSearchResults(results);
    }
  
  private boolean isStop(String s) {
    // stops are 6 digits
    int n = s.length();
    if (n != 6)
      return false;
    for (int i = 0; i < n; i++) {
      if (!Character.isDigit(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }
  
  private boolean isRoute(String s) {
    Matcher matcher = routePattern.matcher(s);
    return matcher.matches();
  }
  
  private List<StopsBean> fetchStopsFromGeocoder(String q) {
    List<StopsBean> result = new ArrayList<StopsBean>();
    List<GeocoderResult> geocoderResults = geocoderService.geocode(q).getResults();
    for (GeocoderResult geocoderResult : geocoderResults) {
      double lat = geocoderResult.getLatitude();
      double lng = geocoderResult.getLongitude();
      CoordinateBounds bounds = SphericalGeometryLibrary.bounds(lat, lng, distanceToStops);
      
      // and add any stops for it
      SearchQueryBean searchQueryBean = makeSearchQuery(bounds);
      StopsBean stops = transitService.getStops(searchQueryBean);
      result.add(stops);
    }
    return result;
  }

  private StopSearchResult makeStopSearchResult(StopBean stopBean) {
    String stopId = stopBean.getId();
    List<Double> latLng = Arrays.asList(new Double[] { stopBean.getLat(), stopBean.getLon() } );
    String stopName = stopBean.getName();
    List<AvailableRoute> availableRoutes = new ArrayList<AvailableRoute>();
    List<RouteBean> stopRouteBeans = stopBean.getRoutes();
    for (RouteBean routeBean : stopRouteBeans) {
      String shortName = routeBean.getShortName();
      String longName = routeBean.getLongName();
      List<DistanceAway> distanceAways = Arrays.asList(
          new DistanceAway[] { new DistanceAway(0, 0) });
      AvailableRoute availableRoute = new AvailableRoute(shortName, longName, distanceAways);
      availableRoutes.add(availableRoute);
    }
    StopSearchResult stopSearchResult = new StopSearchResult(stopId, stopName, latLng, availableRoutes);
    return stopSearchResult;
  }

  private SearchQueryBean makeSearchQuery(String q) {
    return makeSearchQuery(q, serviceArea.getServiceArea());
  }
  
  private SearchQueryBean makeSearchQuery(CoordinateBounds bounds) {
    return makeSearchQuery(null, bounds);
  }
  
  private SearchQueryBean makeSearchQuery(String q, CoordinateBounds bounds) {
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(bounds);
    queryBean.setMaxCount(5);
    if (q != null)
      queryBean.setQuery(q);
    return queryBean;
  }
  
  private List<SearchResult> sortSearchResults(List<SearchResult> searchResults) {
    Collections.sort(searchResults, new Comparator<SearchResult>() {

      @Override
      public int compare(SearchResult o1, SearchResult o2) {
        if ((o1 instanceof RouteSearchResult) && (o2 instanceof StopSearchResult))
          return -1;
        else if ((o1 instanceof StopSearchResult) && (o2 instanceof RouteSearchResult))
          return 1;
        else if ((o1 instanceof RouteSearchResult) && (o2 instanceof RouteSearchResult))
          return ((RouteSearchResult) o1).getName().compareTo(((RouteSearchResult) o2).getName());
        else if ((o1 instanceof StopSearchResult) && (o2 instanceof StopSearchResult))
          return ((StopSearchResult) o1).getName().compareTo(((StopSearchResult) o2).getName());
        else
          throw new IllegalStateException("Unknown search result types");
      }
    });
    
    return searchResults;
  }

}
