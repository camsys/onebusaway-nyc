package org.onebusaway.nyc.presentation.impl.search;

import org.onebusaway.exceptions.NoSuchStopServiceException;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;
import org.onebusaway.nyc.geocoder.service.NycGeocoderService;
import org.onebusaway.nyc.presentation.model.SearchResultCollection;
import org.onebusaway.nyc.presentation.service.search.SearchResultFactory;
import org.onebusaway.nyc.presentation.service.search.SearchService;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RoutesBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

@Component
public class SearchServiceImpl implements SearchService {

  private static Logger _log = LoggerFactory.getLogger(SearchServiceImpl.class);

  // when querying for routes from a lat/lng, use this distance in meters
  private static final double DISTANCE_TO_ROUTES = 100;

  // when querying for stops from a lat/lng, use this distance in meters
  private static final double DISTANCE_TO_STOPS = 100;

  @Autowired
  private NycGeocoderService _geocoderService;
  
  @Autowired
  private TransitDataService _transitDataService;

  private SearchResultCollection _results = null;

  private Map<String, String> _routeShortNameToIdMap = new HashMap<String, String>();

  private Map<String, String> _routeLongNameToIdMap = new HashMap<String, String>();

  public void refreshCaches() {
    synchronized(_routeShortNameToIdMap) {
      _routeShortNameToIdMap.clear();
      _routeLongNameToIdMap.clear();
      
      for(AgencyWithCoverageBean agency : _transitDataService.getAgenciesWithCoverage()) {
        for(String routeId : _transitDataService.getRouteIdsForAgencyId(agency.getAgency().getId()).getList()) {
          RouteBean routeBean = _transitDataService.getRouteForId(routeId);        
          _routeShortNameToIdMap.put(routeBean.getShortName(), routeId);
          _routeLongNameToIdMap.put(routeBean.getLongName(), routeId);
        }
      }    
    }
  }
  
  @PostConstruct
  public void setup() {
    Thread bootstrapThread = new BootstrapThread();
    bootstrapThread.start();
  }
  
  @Override
  public SearchResultCollection findStopsNearPoint(Double latitude, Double longitude, SearchResultFactory resultFactory, Set<String> routeIdFilter) {
    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(latitude, longitude, DISTANCE_TO_STOPS);

    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(bounds);
    queryBean.setMaxCount(100);
    
    StopsBean stops = _transitDataService.getStops(queryBean);

    SearchResultCollection results = new SearchResultCollection();
    results.addRouteIdFilters(routeIdFilter);
    
    for(StopBean stop : stops.getStops()) {
      results.addMatch(resultFactory.getStopResult(stop, routeIdFilter));
    }
    
    return results;    
  }

  @Override
  public SearchResultCollection findRoutesStoppingWithinRegion(CoordinateBounds bounds, SearchResultFactory resultFactory) {
    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(bounds);
    queryBean.setMaxCount(100);
    
    RoutesBean routes = _transitDataService.getRoutes(queryBean);

    SearchResultCollection results = new SearchResultCollection();

    for(RouteBean route : routes.getRoutes()) {
      results.addMatch(resultFactory.getRouteResultForRegion(route));
    }
    
    return results;
  }
  
  @Override
  public SearchResultCollection findRoutesStoppingNearPoint(Double latitude, Double longitude, SearchResultFactory resultFactory) {
    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(latitude, longitude, DISTANCE_TO_ROUTES);

    SearchQueryBean queryBean = new SearchQueryBean();
    queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
    queryBean.setBounds(bounds);
    queryBean.setMaxCount(100);
    
    RoutesBean routes = _transitDataService.getRoutes(queryBean);

    SearchResultCollection results = new SearchResultCollection();

    for(RouteBean route : routes.getRoutes()) {
      results.addMatch(resultFactory.getRouteResult(route));
    }
    
    return results;
  }
  
  @Override
  public SearchResultCollection getSearchResults(String query, SearchResultFactory resultFactory) {
    _results = new SearchResultCollection();
    
    String normalizedQuery = normalizeQuery(query);
    
    tryAsRoute(normalizedQuery, resultFactory);
    
    if(_results.isEmpty() && StringUtils.isNumeric(normalizedQuery)) {
      tryAsStop(normalizedQuery, resultFactory);
    }

    if(_results.isEmpty()) {
      tryAsGeocode(normalizedQuery, resultFactory);
    }
    
    return _results;
  }

  private String normalizeQuery(String q) {
    if(q == null) {
      return null;
    }
    
    q = q.trim();
    
    String normalizedQuery = "";
    String[] tokens = q.split(" ");
    for(int i = 0; i < tokens.length; i++) {
      String token = tokens[i].trim().toUpperCase();

      String lastItem = null;
      String nextItem = null;
      if(i - 1 >= 0) {
        lastItem = tokens[i - 1].trim().toUpperCase();
      }
      
      if(i + 1 < tokens.length) {
        nextItem = tokens[i + 1].trim().toUpperCase();
      }
      
      // keep track of route tokens we found when parsing
      if(_routeShortNameToIdMap.containsKey(token)) {
        // if a route is included as part of another type of query, then it's a filter--
        // so remove it from the normalized query sent to the geocoder or stop service
        if((lastItem != null && !_routeShortNameToIdMap.containsKey(lastItem)) || (nextItem != null && !_routeShortNameToIdMap.containsKey(nextItem))) {
          _results.addRouteIdFilter(_routeShortNameToIdMap.get(token));
          continue;
        }
      }

      // allow the plus sign instead of "and"
      if(token.equals("+")) {
        // if a user is prepending a route filter with a plus sign, chop it off
        // e.g. main and craig + B63
        if(_routeShortNameToIdMap.containsKey(nextItem)) {
          continue;
        }

        token = "and";
      }
      
      normalizedQuery += token + " ";
    }
    
    return normalizedQuery.trim();
  }
  
  private void tryAsRoute(String routeQuery, SearchResultFactory resultFactory) {
    if(routeQuery == null || StringUtils.isEmpty(routeQuery)) {
      return;
    }
    
    routeQuery = routeQuery.toUpperCase().trim();
    
    if(routeQuery.length() < 2) {
      return;
    }
    
    // short name matching
    if(_routeShortNameToIdMap.get(routeQuery) != null) {
      RouteBean routeBean = _transitDataService.getRouteForId(_routeShortNameToIdMap.get(routeQuery));
      _results.addMatch(resultFactory.getRouteResult(routeBean));
    }

    for(String routeShortName : _routeShortNameToIdMap.keySet()) {
      // if the route short name ends or starts with our query, and whatever's left over is a letter
      if(!routeQuery.equals(routeShortName) 
          && ((routeShortName.startsWith(routeQuery) && StringUtils.isAlpha(routeShortName.replace(routeQuery, "")))
          || (routeShortName.endsWith(routeQuery) && StringUtils.isAlpha(routeShortName.replace(routeQuery, ""))))) {
        RouteBean routeBean = _transitDataService.getRouteForId(_routeShortNameToIdMap.get(routeShortName));
        _results.addSuggestion(resultFactory.getRouteResult(routeBean));
        continue;
      }
    }
    
    // long name matching
    for(String routeLongName : _routeLongNameToIdMap.keySet()) {
      if(routeLongName.contains(routeQuery + " ") || routeLongName.contains(" " + routeQuery)) {
        RouteBean routeBean = _transitDataService.getRouteForId(_routeLongNameToIdMap.get(routeLongName));
        _results.addSuggestion(resultFactory.getRouteResult(routeBean));
        continue;        
      }
    }
      
  }

  private void tryAsStop(String stopQuery, SearchResultFactory resultFactory) {
    if(stopQuery == null || StringUtils.isEmpty(stopQuery)) {
      return;
    }
    
    stopQuery = stopQuery.trim();
    
    // try to find a stop ID for all known agencies
    List<StopBean> matches = new ArrayList<StopBean>();
    for(AgencyWithCoverageBean agency : _transitDataService.getAgenciesWithCoverage()) {
      AgencyAndId potentialStopId = new AgencyAndId(agency.getAgency().getId(), stopQuery);

      try {
        StopBean potentialStop = _transitDataService.getStop(potentialStopId.toString());      

        if(potentialStop != null) {
          matches.add(potentialStop);
        }
      } catch(NoSuchStopServiceException ex) {
        continue;
      }
    }
    
    if(matches.size() == 1)
      _results.addMatch(resultFactory.getStopResult(matches.get(0), _results.getRouteIdFilter()));
    else {
      for(StopBean match : matches) {
        _results.addSuggestion(resultFactory.getStopResult(match, _results.getRouteIdFilter()));
      }
    }
  }
  
  private void tryAsGeocode(String query, SearchResultFactory resultFactory) {
    List<NycGeocoderResult> geocoderResults = _geocoderService.nycGeocode(query);
    
    for(NycGeocoderResult result : geocoderResults) {
      if(geocoderResults.size() == 1) {
        _results.addMatch(resultFactory.getGeocoderResult(result, _results.getRouteIdFilter()));
      } else {        
        _results.addSuggestion(resultFactory.getGeocoderResult(result, _results.getRouteIdFilter()));
      }
    }
  }
  
  // a thread that can block for network IO while tomcat starts
  private class BootstrapThread extends Thread {

    @Override
    public void run() {     
      try {
        refreshCaches();
      } catch (Exception e) {
        _log.error("Error updating cache: " + e.getMessage());
      }
    }   

  }
  
}