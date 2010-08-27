package org.onebusaway.nyc.webapp.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.geocoder.model.GeocoderResult;
import org.onebusaway.geocoder.services.GeocoderService;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.webapp.model.AvailableRoute;
import org.onebusaway.nyc.webapp.model.DistanceAway;
import org.onebusaway.nyc.webapp.model.search.RouteSearchResult;
import org.onebusaway.nyc.webapp.model.search.SearchResult;
import org.onebusaway.nyc.webapp.model.search.StopSearchResult;
import org.onebusaway.presentation.services.ServiceAreaService;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RoutesBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Handles requests for a generic search. Can return route/stop specific results.
 */
@ParentPackage("json-default")
@Result(type="json", params={"callbackParameter", "callback"})
public class SearchAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  private final List<SearchResult> searchResults = new ArrayList<SearchResult>();
  
  @Autowired
  private TransitDataService transitService;
  
  @Autowired
  private ServiceAreaService serviceArea;
  
  @Autowired
  private GeocoderService geocoderService;
  
  // from q variable in query string
  private String q;
  
  // when querying for stops from a lat/lng, use this distance in meters
  private double distanceToStops = 200;
  
  private final static Pattern routePattern = Pattern.compile("(?:[BMQS]|BX)[0-9]+");
  
  @Override
  public String execute() {
    if (q == null || q.isEmpty())
      return SUCCESS;
    
    String[] fields = q.split(" ");
    if (fields.length == 1) {
      // we are probably a query for a route or a stop
      if (isStop(q)) {
        SearchQueryBean queryBean = makeSearchQuery(q);
        StopsBean stops = transitService.getStops(queryBean);
        
        for (StopBean stopBean : stops.getStops()) {
          StopSearchResult stopSearchResult = makeStopSearchResult(stopBean);
          searchResults.add(stopSearchResult);
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
          searchResults.add(routeSearchResult);
        }
      }
    } else {
      // we should be either an intersection and route or an intersection
      
      // if we're a route, set route and the geocoder query appropriately
      String route = null;
      String queryNoRoute = null;
      if (isRoute(fields[0])) {
        route = fields[0];
        StringBuilder sb = new StringBuilder(16);
        for (int i = 1; i < fields.length; i++) {
          sb.append(fields[i]);
        }
        queryNoRoute = sb.toString();
      }
      else if (isRoute(fields[fields.length-1])) {
        route = fields[fields.length-1];
        StringBuilder sb = new StringBuilder(16);
        for (int i = 0; i < fields.length-1; i++) {
          sb.append(fields[i]);
        }
        queryNoRoute = sb.toString();
      }
      
      if (route != null) {
        // we are a route and intersection
        List<StopsBean> stopsList = fetchStopsFromGeocoder(queryNoRoute);
        for (StopsBean stopsBean : stopsList) {
          for (StopBean stopBean : stopsBean.getStops()) {
            boolean useStop = false;
            List<RouteBean> routes = stopBean.getRoutes();
            for (RouteBean routeBean : routes) {
              String idNoAgency = parseIdWithoutAgency(routeBean.getId());
              if (idNoAgency.equals(route)) {
                useStop = true;
                break;
              }
            }
            if (useStop) {
              StopSearchResult stopSearchResult = makeStopSearchResult(stopBean);
              searchResults.add(stopSearchResult);   
            }
          }
        }
      } else {
        // try an intersection search
        List<StopsBean> stopsList = fetchStopsFromGeocoder(q);
        for (StopsBean stopsBean : stopsList) {
          for (StopBean stopBean : stopsBean.getStops()) {
            StopSearchResult stopSearchResult = makeStopSearchResult(stopBean);
            searchResults.add(stopSearchResult);            
          }
        }
      }
    }

    return SUCCESS;
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
    List<Double> latLng = makeLatLng(stopBean.getLat(), stopBean.getLon());
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

  private boolean isRoute(String s) {
    Matcher matcher = routePattern.matcher(s);
    return matcher.matches();
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

  public List<SearchResult> getSearchResults() {
    return searchResults;
  }
  
  public void setQ(String query) {
    this.q = query.trim();
  }
  
}
