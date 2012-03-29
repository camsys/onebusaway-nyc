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
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RoutesBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SearchServiceImpl implements SearchService {

  // the pattern of what can be leftover after prefix/suffix matching for a route
  // to be a "suggestion" for a given search
  private static final Pattern leftOverMatchPattern = 
      Pattern.compile("^([A-Z]|-)+$");

  // when querying for routes from a lat/lng, use this distance in meters
  private static final double DISTANCE_TO_ROUTES = 100;

  // when querying for stops from a lat/lng, use this distance in meters
  private static final double DISTANCE_TO_STOPS = 100;

  @Autowired
  private NycGeocoderService _geocoderService;
  
  @Autowired
  private NycTransitDataService _nycTransitDataService;

  @Autowired
  private TransitDataService _transitDataService;

  private Map<String, String> _routeShortNameToIdMap = new HashMap<String, String>();

  private Map<String, String> _routeLongNameToIdMap = new HashMap<String, String>();

  private String _bundleIdForCaches = null;
  
  // we keep an internal cache of route short/long names because if we moved this into the
  // transit data federation, we'd also have to move the model factory and some other agency-specific
  // conventions, which wouldn't be pretty.
  //
  // long-term FIXME: figure out how to split apart the model creation a bit more from the actual
  // search process.
  public void refreshCachesIfNecessary() {
    String currentBundleId = _nycTransitDataService.getActiveBundleId();

    if((_bundleIdForCaches != null && _bundleIdForCaches.equals(currentBundleId)) || currentBundleId == null) {
      return;
    }
  
    _routeShortNameToIdMap.clear();
    _routeLongNameToIdMap.clear();
    
    for(AgencyWithCoverageBean agency : _transitDataService.getAgenciesWithCoverage()) {
      for(String routeId : _transitDataService.getRouteIdsForAgencyId(agency.getAgency().getId()).getList()) {
        RouteBean routeBean = _transitDataService.getRouteForId(routeId);        
        _routeShortNameToIdMap.put(routeBean.getShortName(), routeId);
        _routeLongNameToIdMap.put(routeBean.getLongName(), routeId);
      }
    }    
    
    _bundleIdForCaches = currentBundleId;
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
    refreshCachesIfNecessary();
    
    SearchResultCollection results = new SearchResultCollection();
    
    String normalizedQuery = normalizeQuery(results, query);
    
    tryAsRoute(results, normalizedQuery, resultFactory);
    
    if(results.isEmpty() && StringUtils.isNumeric(normalizedQuery)) {
      tryAsStop(results, normalizedQuery, resultFactory);
    }

    if(results.isEmpty()) {
      tryAsGeocode(results, normalizedQuery, resultFactory);
    }
    
    return results;
  }

  private String normalizeQuery(SearchResultCollection results, String q) {
    if(q == null) {
      return null;
    }
    
    q = q.trim();

    List<String> tokens = new ArrayList<String>();
    StringTokenizer tokenizer = new StringTokenizer(q, " +", true);
    while(tokenizer.hasMoreTokens()) {
      String token = tokenizer.nextToken().trim().toUpperCase();
      
      if(!StringUtils.isEmpty(token)) {
        tokens.add(token);
      }
    }
    
    String normalizedQuery = "";
    for(int i = 0; i < tokens.size(); i++) {
      String token = tokens.get(i);
      String lastItem = null;
      String nextItem = null;
      if(i - 1 >= 0) {
        lastItem = tokens.get(i - 1);
      }
      if(i + 1 < tokens.size()) {
        nextItem = tokens.get(i + 1);
      }
      
      // keep track of route tokens we found when parsing
      if(_routeShortNameToIdMap.containsKey(token)) {
        // if a route is included as part of another type of query, then it's a filter--
        // so remove it from the normalized query sent to the geocoder or stop service
        if((lastItem != null && !_routeShortNameToIdMap.containsKey(lastItem)) || (nextItem != null && !_routeShortNameToIdMap.containsKey(nextItem))) {
          results.addRouteIdFilter(_routeShortNameToIdMap.get(token));
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
  
  private void tryAsRoute(SearchResultCollection results, String routeQuery, SearchResultFactory resultFactory) {
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
      results.addMatch(resultFactory.getRouteResult(routeBean));
    }

    for(String routeShortName : _routeShortNameToIdMap.keySet()) {
      // if the route short name ends or starts with our query, and whatever's left over 
      // matches the regex
      String leftOvers = routeShortName.replace(routeQuery, "");
      Matcher matcher = leftOverMatchPattern.matcher(leftOvers);
      Boolean leftOversAreDiscardable = matcher.find();
      
      if(!routeQuery.equals(routeShortName) 
          && ((routeShortName.startsWith(routeQuery) && leftOversAreDiscardable)
          || (routeShortName.endsWith(routeQuery) && leftOversAreDiscardable))) {
        RouteBean routeBean = _transitDataService.getRouteForId(_routeShortNameToIdMap.get(routeShortName));
        results.addSuggestion(resultFactory.getRouteResult(routeBean));
        continue;
      }
    }
    
    // long name matching
    for(String routeLongName : _routeLongNameToIdMap.keySet()) {
      if(routeLongName.contains(routeQuery + " ") || routeLongName.contains(" " + routeQuery)) {
        RouteBean routeBean = _transitDataService.getRouteForId(_routeLongNameToIdMap.get(routeLongName));
        results.addSuggestion(resultFactory.getRouteResult(routeBean));
        continue;        
      }
    }
      
  }

  private void tryAsStop(SearchResultCollection results, String stopQuery, SearchResultFactory resultFactory) {
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
      results.addMatch(resultFactory.getStopResult(matches.get(0), results.getRouteIdFilter()));
    else {
      for(StopBean match : matches) {
        results.addSuggestion(resultFactory.getStopResult(match, results.getRouteIdFilter()));
      }
    }
  }
  
  private void tryAsGeocode(SearchResultCollection results, String query, SearchResultFactory resultFactory) {
    List<NycGeocoderResult> geocoderResults = _geocoderService.nycGeocode(query);
    
    for(NycGeocoderResult result : geocoderResults) {
      if(geocoderResults.size() == 1) {
        results.addMatch(resultFactory.getGeocoderResult(result, results.getRouteIdFilter()));
      } else {        
        results.addSuggestion(resultFactory.getGeocoderResult(result, results.getRouteIdFilter()));
      }
    }
  }
  
}