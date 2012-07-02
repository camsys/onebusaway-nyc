package org.onebusaway.nyc.presentation.impl.search;

import org.onebusaway.exceptions.NoSuchStopServiceException;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.onebusaway.nyc.geocoder.service.NycGeocoderService;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.nyc.presentation.model.SearchResultCollection;
import org.onebusaway.nyc.presentation.service.search.SearchResultFactory;
import org.onebusaway.nyc.presentation.service.search.SearchService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RoutesBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SearchServiceImpl implements SearchService {

	// the pattern of what can be leftover after prefix/suffix matching for a
	// route
	// to be a "suggestion" for a given search
	private static final Pattern leftOverMatchPattern = Pattern.compile("^([A-Z]|-)+$");

	// when querying for routes from a lat/lng, use this distance in meters
	private static final double DISTANCE_TO_ROUTES = 600;
	
	// The max number of closest routes to display
	private static final int MAX_ROUTES = 10;

	// when querying for stops from a lat/lng, use this distance in meters
	private static final int DISTANCE_TO_STOPS = 600;
	
	// The max number of closest stops to display
	private static final int MAX_STOPS = 10;

	@Autowired
	private NycGeocoderService _geocoderService;

	@Autowired
	private NycTransitDataService _nycTransitDataService;

	private Map<String, String> _routeShortNameToIdMap = new HashMap<String, String>();

	private Map<String, String> _routeLongNameToIdMap = new HashMap<String, String>();

	private String _bundleIdForCaches = null;

	// we keep an internal cache of route short/long names because if we moved
	// this into the
	// transit data federation, we'd also have to move the model factory and
	// some other agency-specific
	// conventions, which wouldn't be pretty.
	//
	// long-term FIXME: figure out how to split apart the model creation a bit
	// more from the actual
	// search process.
	public void refreshCachesIfNecessary() {
		String currentBundleId = _nycTransitDataService.getActiveBundleId();

		if ((_bundleIdForCaches != null && _bundleIdForCaches.equals(currentBundleId)) || currentBundleId == null) {
			return;
		}

		_routeShortNameToIdMap.clear();
		_routeLongNameToIdMap.clear();

		for (AgencyWithCoverageBean agency : _nycTransitDataService.getAgenciesWithCoverage()) {
			for (String routeId : _nycTransitDataService.getRouteIdsForAgencyId(agency.getAgency().getId()).getList()) {
				RouteBean routeBean = _nycTransitDataService.getRouteForId(routeId);
				_routeShortNameToIdMap.put(routeBean.getShortName(), routeId);
				_routeLongNameToIdMap.put(routeBean.getLongName(), routeId);
			}
		}

		_bundleIdForCaches = currentBundleId;
	}

	@Override
	public SearchResultCollection findStopsNearPoint(Double latitude, Double longitude, SearchResultFactory resultFactory,
			Set<String> routeIdFilter) {
	  
	  CoordinateBounds bounds = SphericalGeometryLibrary.bounds(latitude, longitude, DISTANCE_TO_STOPS);

		SearchQueryBean queryBean = new SearchQueryBean();
		queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
		queryBean.setBounds(bounds);
		queryBean.setMaxCount(100);

		StopsBean stops = _nycTransitDataService.getStops(queryBean);

		List<SearchResult> stopResults = new ArrayList<SearchResult>();
		Map<SearchResult, StopBean> stopBeanBySearchResult = new HashMap<SearchResult, StopBean>();

		for (StopBean stop : stops.getStops()) {
			SearchResult result = resultFactory.getStopResult(stop, routeIdFilter);
			result.setDistanceToQueryLocation(SphericalGeometryLibrary.distanceFaster(stop.getLat(), stop.getLon(), latitude, longitude));
			stopResults.add(result);
			stopBeanBySearchResult.put(result, stop);
		}
		
		Collections.sort(stopResults, new SearchResultDistanceComparator());
		
		SearchResultCollection results = new SearchResultCollection();
    results.addRouteIdFilters(routeIdFilter);
    
		// Keep track of which routes are already in our search results by direction
    Map<String, List<RouteBean>> routesByDirectionAlreadyInResults = new HashMap<String, List<RouteBean>>();
    
    // Cache stops by route so we don't need to call the transit data service repeatedly for the same route
    Map<String, StopsForRouteBean> stopsForRouteLookup = new HashMap<String, StopsForRouteBean>();
		
    // Iterate through each stop and see if it adds additional routes for a direction to our final results.
    for (SearchResult stopResult : stopResults) {
		  
      // Get the stop bean that is actually inside this search result. We kept track of it earlier.
      StopBean stopBeanForSearchResult = stopBeanBySearchResult.get(stopResult);
      
      // Record of routes by direction id for this stop
      Map<String, List<RouteBean>> routesByDirection = new HashMap<String, List<RouteBean>>();
      
		  for (RouteBean route : stopBeanForSearchResult.getRoutes()) {
		    // route is a route serving the current stopBeanForSearchResult
		    
		    // Query for all stops on this route
		    StopsForRouteBean stopsForRoute = stopsForRouteLookup.get(route.getId());
		    if (stopsForRoute == null) {
		      stopsForRoute = _nycTransitDataService.getStopsForRoute(route.getId());
		      stopsForRouteLookup.put(route.getId(), stopsForRoute);
		    }
		    
		    // Get the groups of stops on this route. The id of each group corresponds to a GTFS direction id for this route.
		    for (StopGroupingBean stopGrouping : stopsForRoute.getStopGroupings()) {
		      for (StopGroupBean stopGroup : stopGrouping.getStopGroups()) {
		        
		        String directionId = stopGroup.getId();
		        
		        // Check if the current stop is served in this direction. If so, record it.
		        if (stopGroup.getStopIds().contains(stopBeanForSearchResult.getId())) {
		          if (!routesByDirection.containsKey(directionId)) {
		            routesByDirection.put(directionId, new ArrayList<RouteBean>());
		          }
		          routesByDirection.get(directionId).add(route);
		        }
		      }
		    }
		  }
		  
		  // Iterate over routes binned by direction for this stop and compare to routes by direction already in our search results
		  boolean shouldAddStopToResults = false;
		  for (Map.Entry<String, List<RouteBean>> entry : routesByDirection.entrySet()) {
		    String directionId = entry.getKey();
		    List<RouteBean> routesForThisDirection = entry.getValue();
		    
		    if (!routesByDirectionAlreadyInResults.containsKey(directionId)) {
		      routesByDirectionAlreadyInResults.put(directionId, new ArrayList<RouteBean>());
		    }
		    
		    @SuppressWarnings("unchecked")
	      List<RouteBean> additionalRoutes = ListUtils.subtract(routesForThisDirection, routesByDirectionAlreadyInResults.get(directionId));
	      if (additionalRoutes.size() > 0) {
	        // This stop is contributing new routes in this direction, so add these additional
	        // stops to our record of stops by direction already in search results and toggle
	        // flag that tells to to add the stop to the search results.
	        routesByDirectionAlreadyInResults.get(directionId).addAll(additionalRoutes);
	        // We use this flag because we want to add new routes to our record potentially for each
	        // direction id, but we only want to add the stop to the search results once. It happens below.
	        shouldAddStopToResults = true;
	      }
		  }
		  if (shouldAddStopToResults) {
		    // Add the stop to our search results
        results.addMatch(stopResult);
		  }
		}
		
		if (results.getMatches().size() > MAX_STOPS) {
			results.getMatches().subList(MAX_STOPS, results.getMatches().size()).clear();
		}

		return results;
	}

	@Override
	public SearchResultCollection findRoutesStoppingWithinRegion(CoordinateBounds bounds, SearchResultFactory resultFactory) {
		SearchQueryBean queryBean = new SearchQueryBean();
		queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
		queryBean.setBounds(bounds);
		queryBean.setMaxCount(100);

		RoutesBean routes = _nycTransitDataService.getRoutes(queryBean);

		SearchResultCollection results = new SearchResultCollection();

		for (RouteBean route : routes.getRoutes()) {
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

		RoutesBean routes = _nycTransitDataService.getRoutes(queryBean);

		SearchResultCollection results = new SearchResultCollection();

		for (RouteBean route : routes.getRoutes()) {
			
			StopsForRouteBean stopsBean = _nycTransitDataService.getStopsForRoute(route.getId());
			
			Double minDistanceToRoute = null;
			for (StopBean stop : stopsBean.getStops()) {
				Double distance = SphericalGeometryLibrary.distanceFaster(stop.getLat(), stop.getLon(), latitude, longitude);
				if (minDistanceToRoute == null) {
					minDistanceToRoute = distance;
					continue;
				}
				if (distance < minDistanceToRoute) minDistanceToRoute = distance;
			}
			
			SearchResult result = resultFactory.getRouteResult(route);
			result.setDistanceToQueryLocation(minDistanceToRoute);
			
			results.addMatch(result);
		}
		
		Collections.sort(results.getMatches(), new SearchResultDistanceComparator());
		if (results.getMatches().size() > MAX_ROUTES) {
			results.getMatches().subList(MAX_ROUTES, results.getMatches().size()).clear();
		}

		return results;
	}

	@Override
	public SearchResultCollection getSearchResults(String query, SearchResultFactory resultFactory) {
		refreshCachesIfNecessary();

		SearchResultCollection results = new SearchResultCollection();

		String normalizedQuery = normalizeQuery(results, query);

		tryAsRoute(results, normalizedQuery, resultFactory);

		if (results.isEmpty() && StringUtils.isNumeric(normalizedQuery)) {
			tryAsStop(results, normalizedQuery, resultFactory);
		}

		if (results.isEmpty()) {
			tryAsGeocode(results, normalizedQuery, resultFactory);
		}

		return results;
	}

	private String normalizeQuery(SearchResultCollection results, String q) {
		if (q == null) {
			return null;
		}

		q = q.trim();

		List<String> tokens = new ArrayList<String>();
		StringTokenizer tokenizer = new StringTokenizer(q, " +", true);
		while (tokenizer.hasMoreTokens()) {
			String token = tokenizer.nextToken().trim().toUpperCase();

			if (!StringUtils.isEmpty(token)) {
				tokens.add(token);
			}
		}

		String normalizedQuery = "";
		for (int i = 0; i < tokens.size(); i++) {
			String token = tokens.get(i);
			String lastItem = null;
			String nextItem = null;
			if (i - 1 >= 0) {
				lastItem = tokens.get(i - 1);
			}
			if (i + 1 < tokens.size()) {
				nextItem = tokens.get(i + 1);
			}

			// keep track of route tokens we found when parsing
			if (_routeShortNameToIdMap.containsKey(token)) {
				// if a route is included as part of another type of query, then
				// it's a filter--
				// so remove it from the normalized query sent to the geocoder
				// or stop service
				if ((lastItem != null && !_routeShortNameToIdMap.containsKey(lastItem))
						|| (nextItem != null && !_routeShortNameToIdMap.containsKey(nextItem))) {
					results.addRouteIdFilter(_routeShortNameToIdMap.get(token));
					continue;
				}
			} else {
			  // if the token is not a route and the next or last token is a valid stop id,
			  // consider the token a bad filter and remove it from the query.
			  if ((lastItem != null && stopsForId(lastItem).size() > 0) || (nextItem != null && stopsForId(nextItem).size() > 0)) {
			    continue;
			  }
			}

			// allow the plus sign instead of "and"
			if (token.equals("+")) {
				// if a user is prepending a route filter with a plus sign, chop
				// it off
				// e.g. main and craig + B63
				if (_routeShortNameToIdMap.containsKey(nextItem)) {
					continue;
				}

				token = "and";
			}

			normalizedQuery += token + " ";
		}

		return normalizedQuery.trim();
	}

	private void tryAsRoute(SearchResultCollection results, String routeQuery, SearchResultFactory resultFactory) {
		if (routeQuery == null || StringUtils.isEmpty(routeQuery)) {
			return;
		}

		routeQuery = routeQuery.toUpperCase().trim();

		if (routeQuery.length() < 2) {
			return;
		}

		// short name matching
		if (_routeShortNameToIdMap.get(routeQuery) != null) {
			RouteBean routeBean = _nycTransitDataService.getRouteForId(_routeShortNameToIdMap.get(routeQuery));
			results.addMatch(resultFactory.getRouteResult(routeBean));
		}

		for (String routeShortName : _routeShortNameToIdMap.keySet()) {
			// if the route short name ends or starts with our query, and
			// whatever's left over
			// matches the regex
			String leftOvers = routeShortName.replace(routeQuery, "");
			Matcher matcher = leftOverMatchPattern.matcher(leftOvers);
			Boolean leftOversAreDiscardable = matcher.find();

			if (!routeQuery.equals(routeShortName)
					&& ((routeShortName.startsWith(routeQuery) && leftOversAreDiscardable) || (routeShortName.endsWith(routeQuery) && leftOversAreDiscardable))) {
				RouteBean routeBean = _nycTransitDataService.getRouteForId(_routeShortNameToIdMap.get(routeShortName));
				results.addSuggestion(resultFactory.getRouteResult(routeBean));
				continue;
			}
		}

		// long name matching
		for (String routeLongName : _routeLongNameToIdMap.keySet()) {
			if (routeLongName.contains(routeQuery + " ") || routeLongName.contains(" " + routeQuery)) {
				RouteBean routeBean = _nycTransitDataService.getRouteForId(_routeLongNameToIdMap.get(routeLongName));
				results.addSuggestion(resultFactory.getRouteResult(routeBean));
				continue;
			}
		}

	}

	private void tryAsStop(SearchResultCollection results, String stopQuery, SearchResultFactory resultFactory) {
		if (stopQuery == null || StringUtils.isEmpty(stopQuery)) {
			return;
		}

		stopQuery = stopQuery.trim();

		// try to find a stop ID for all known agencies
		List<StopBean> matches = stopsForId(stopQuery);

		if (matches.size() == 1)
			results.addMatch(resultFactory.getStopResult(matches.get(0), results.getRouteIdFilter()));
		else {
			for (StopBean match : matches) {
				results.addSuggestion(resultFactory.getStopResult(match, results.getRouteIdFilter()));
			}
		}
	}

	private void tryAsGeocode(SearchResultCollection results, String query, SearchResultFactory resultFactory) {
		List<NycGeocoderResult> geocoderResults = _geocoderService.nycGeocode(query);

		for (NycGeocoderResult result : geocoderResults) {
			if (geocoderResults.size() == 1) {
				results.addMatch(resultFactory.getGeocoderResult(result, results.getRouteIdFilter()));
			} else {
				results.addSuggestion(resultFactory.getGeocoderResult(result, results.getRouteIdFilter()));
			}
		}
	}
	
	// Utility method for getting all known stops for an id with no agency
	private List<StopBean> stopsForId(String id) {
	  List<StopBean> matches = new ArrayList<StopBean>();
    for (AgencyWithCoverageBean agency : _nycTransitDataService.getAgenciesWithCoverage()) {
      AgencyAndId potentialStopId = new AgencyAndId(agency.getAgency().getId(), id);

      try {
        StopBean potentialStop = _nycTransitDataService.getStop(potentialStopId.toString());

        if (potentialStop != null) {
          matches.add(potentialStop);
        }
      } catch (NoSuchStopServiceException ex) {
        continue;
      }
    }
    return matches;
	}

	private class SearchResultDistanceComparator implements Comparator<SearchResult> {

		@Override
		public int compare(SearchResult o1, SearchResult o2) {
			
			if (o1.getDistanceToQueryLocation() == null && o2.getDistanceToQueryLocation() != null) {
				return +1;
			} else if (o1.getDistanceToQueryLocation() != null && o2.getDistanceToQueryLocation() == null) {
				return -1;
			} else if (o1.getDistanceToQueryLocation() > o2.getDistanceToQueryLocation()) {
				return +1;
			} else if (o1.getDistanceToQueryLocation() < o2.getDistanceToQueryLocation()) {
				return -1;
			}
			return 0;
		}
	}
}