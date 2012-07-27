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
package org.onebusaway.nyc.sms.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.nyc.presentation.model.SearchResultCollection;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.search.SearchResultFactory;
import org.onebusaway.nyc.presentation.service.search.SearchService;
import org.onebusaway.nyc.sms.actions.model.GeocodeResult;
import org.onebusaway.nyc.sms.actions.model.RouteAtStop;
import org.onebusaway.nyc.sms.actions.model.RouteDirection;
import org.onebusaway.nyc.sms.actions.model.RouteResult;
import org.onebusaway.nyc.sms.actions.model.ServiceAlertResult;
import org.onebusaway.nyc.sms.actions.model.StopResult;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion;

public class IndexAction extends SessionedIndexAction implements InitializingBean {
  
  private static final long serialVersionUID = 1L;

  private static final int MAX_SMS_CHARACTER_COUNT = 160;
  
  @Autowired
  private RealtimeService _realtimeService;

  @Autowired
  private ConfigurationService _configurationService;

  @Autowired
  private SearchService _searchService;

  @Autowired
  private NycTransitDataService _nycTransitDataService;

  private JGoogleAnalyticsTracker _googleAnalytics = null;
  
  private String _response = null;
  
  @Override
  public void initializeSession(String sessionId) {
    
    super.initializeSession(sessionId);
    
    // send an initial visit to google analytics to tie our events to
    if(_googleAnalytics != null) {
      try {
        _googleAnalytics.trackPageView("/sms", "New SMS Session", "");
      } catch(Exception e) {
        //discard
      }
    }
  };
  
  public String execute() throws Exception {
    
    SearchResultFactory _resultFactory = new SearchResultFactoryImpl(_nycTransitDataService, _realtimeService, _configurationService);
    
    String commandString = getCommand(_query);    
    String queryString = getQuery(_query);

    if(queryString != null && !queryString.isEmpty()) {
      _lastQuery = queryString;
      _searchResults = _searchService.getSearchResults(queryString, _resultFactory);
    } else if (commandString != null && commandString.equals("R") && _lastQuery != null && !_lastQuery.isEmpty()) {
      _searchResults = _searchService.getSearchResults(_lastQuery, _resultFactory);
    }
          
    while(true) {
      if(_searchResults.getMatches().size() > 0) {
        // route identifier search
        if(_searchResults.getMatches().size() == 1 && _searchResults.getResultType().equals("RouteResult")) {
          RouteResult route = (RouteResult)_searchResults.getMatches().get(0);

          // If we get a route back, but there is no direction information in it,
          // this is a route in the bundle with no trips/stops/etc.
          // Show no results.
          if (route.getDirections() != null && route.getDirections().size() == 0) {
            _response = errorResponse("No results.");
            break;
            
          } else if (commandString != null && commandString.equals("C")) {
            // find a unique set of service alerts for the route found
            Set<String> alerts = new HashSet<String>();
            for(RouteDirection direction : route.getDirections()) {
              for(NaturalLanguageStringBean alert : direction.getSerivceAlerts()) {
                alerts.add(alert.getValue());
              }
            }

            // make the alerts into results
            SearchResultCollection newResults = new SearchResultCollection();                        
            for(String alert : alerts) {
              newResults.addMatch(new ServiceAlertResult(alert));
            }
            
            if (newResults.getMatches().size() == 0) {
              _response = errorResponse("No " + route.getShortName() + " alerts.");
              break;
            }

            _searchResults = newResults;
            continue;
            
            // route schedule status
          } else {
            _response = routeResponse(route);
            break;
          }
          
        // paginated service alerts
        } else if(_searchResults.getResultType().equals("ServiceAlertResult")) {
          if(commandString != null && commandString.equals("N")) {
            _response = serviceAlertResponse(_searchResultsCursor);
          } else {
            _response = serviceAlertResponse(0);
          }
          break;
          
        // one or more paginated stops
        } else if(_searchResults.getResultType().equals("StopResult")) {
          if (commandString != null && commandString.equals("N")) {
            
            _response = directionDisambiguationResponse();
            
          } else if (_searchResults.getMatches().size() > 1 && commandString != null && getRoutesInSearchResults().contains(commandString)) {
            // We presented a list of routes for nearby stops to the user and they chose a route.
            // Filter the search results to those that contain the chosen route and have the user
            // pick a direction.
            
            // Filter the result set to only the ones containing the chosen route in each direction
            // might have to do a quick fake search to get a filter object
            SearchResultCollection justToGetAFilter = _searchService.getSearchResults("00000 " + commandString, _resultFactory);
            _searchResults.getRouteIdFilter().clear();
            _searchResults.addRouteIdFilters(justToGetAFilter.getRouteIdFilter());
            
            // Filter the stop results down to the provided route for only up to one stop in each direction
            filterStopSearchResultsToRouteFilterAndDirection();
            
            // If there is only one stop after filtering, choose it for the user and don't display the direction disambiguation screen
            if (_searchResults.getMatches().size() == 1) {
              commandString = "1";
              continue;
            }
            
            _response = directionDisambiguationResponse();
            
          } else if(StringUtils.isNumeric(commandString)) {
            
            StopResult selectedStop = (StopResult)_searchResults.getMatches().get(Integer.parseInt(commandString) - 1);
            _searchResults.getMatches().clear();
            _searchResults.getMatches().add(selectedStop);
            
            AgencyAndId id = AgencyAndIdLibrary.convertFromString((String)_searchResults.getRouteIdFilter().toArray()[0]);
            
            _lastQuery = selectedStop.getIdWithoutAgency() + " " + id.getId();
            _response = singleStopResponse(null);
            _searchResults = null;
            
          } else if(_searchResults.getMatches().size() > 1) {
            
            // See if there is a stop in search results that matches our route if filter
            StopResult aStopServingRouteInFilter = (StopResult)CollectionUtils.find(_searchResults.getMatches(), new Predicate() {
              
              @Override
              public boolean evaluate(Object object) {
                StopResult stopResult = (StopResult)object;
                if (stopResult.matchesRouteIdFilter()) {
                  return true;
                }
                return false;
              }
            });
            
            // If there is a stop matching our route id filter and there is a route id filter (if filter is empty or null, everything in
            // search results 'matches' the filter) present the direction disambiguation view.
            if (aStopServingRouteInFilter != null && _searchResults.getRouteIdFilter() != null && _searchResults.getRouteIdFilter().size() > 0) {
              _response = directionDisambiguationResponse();
              
              // If there is only one route served by the stops in our search results, set the command
              // string as if the user had chosen this route. We are skipping asking them to do that.
            } else if (getRoutesInSearchResults().size() == 1) {
              
              commandString = getRoutesInSearchResults().first();
              continue;
              
              // There is more than one route served by the stops in our results and 
              // either there is no route id filter or none of our search results match the filter, so present
              // the multiple stop response
            } else {
              _response = multipleStopResponse();
            }
          } else if (_searchResults.getMatches().size() == 1) {
            
            StopResult stopResult = (StopResult)_searchResults.getMatches().get(0);
            
            // Check for case where there is a route id filter, but no service for
            // that route for the stop in question.
            if (!stopResult.matchesRouteIdFilter()) {
              
              // Search for nearby stops
              SearchResultCollection nearbyStops = _searchService.
                  findStopsNearPoint(stopResult.getStop().getLat(), stopResult.getStop().getLon(), _resultFactory, _searchResults.getRouteIdFilter());
              
              // See if there is at least one stop of the nearby stops that serves the route in the filter
              StopResult aNearbyStopServingRouteInFilter = (StopResult)CollectionUtils.find(nearbyStops.getMatches(), new Predicate() {
                
                @Override
                public boolean evaluate(Object object) {
                  StopResult stopResult = (StopResult)object;
                  if (stopResult.matchesRouteIdFilter()) {
                    return true;
                  }
                  return false;
                }
              });
              
              // If we found nearby stops that service the route(s) in the filter, add those stops to our
              // search results and call the method that knows how to display them.
              if (aNearbyStopServingRouteInFilter != null) {
                
                _response = singleStopWrongFilterNearbyResponse();
                _searchResults = nearbyStops;
                
                // We can't find nearby stops that service the route in the filter
                // so call the method that lets the user know that.
              } else {
                AgencyAndId id = AgencyAndIdLibrary.convertFromString((String)_searchResults.getRouteIdFilter().toArray()[0]);
                _searchResults = _searchService.getSearchResults(stopResult.getIdWithoutAgency(), _resultFactory);
                _lastQuery = stopResult.getIdWithoutAgency();
                _response = singleStopResponse("No " + id.getId() + " stops nearby");
                _searchResults = null;
              }
            } else {
              _response = singleStopResponse(null);
              _searchResults = null;
            }
          }
          break;
          
        // an exact match for a location--i.e. location isn't ambiguous
        } else if(_searchResults.getMatches().size() == 1 && _searchResults.getResultType().equals("GeocodeResult")) {
          GeocodeResult geocodeResult = (GeocodeResult)_searchResults.getMatches().get(0);

          // we don't do anything with regions--too much information to show via SMS.
          if(geocodeResult.isRegion()) {
            _response = errorResponse("Too general.");
            break;

          // find stops near the point/address/intersection the user provided
          } else {
            _searchResults = _searchService.findStopsNearPoint(geocodeResult.getLatitude(), geocodeResult.getLongitude(), 
                _resultFactory, _searchResults.getRouteIdFilter());
            _searchResults.setQueryLat(geocodeResult.getLatitude());
            _searchResults.setQueryLon(geocodeResult.getLongitude());
            continue;
            
          }
        }
      }
    
      // process suggestions: suggestions can be ambiguous locations, or 
      // multiple routes--e.g. X17 -> X17A,C,J
      if(_searchResults.getSuggestions().size() > 0) {
        if(_searchResults.getResultType().equals("RouteResult")) {
          _response = didYouMeanResponse();

        // if we get a geocode result, the user is choosing among multiple
        // ambiguous addresses. we also recognize a numeric input that
        // represents which ambiguous location number the user wants to use.
        } else if(_searchResults.getResultType().equals("GeocodeResult")) {
          if(commandString != null) {
            if(commandString.equals("M")) {
              _response = locationDisambiguationResponse(_searchResultsCursor);
              break;
              
            // choosing an ambiguous choice by number
            } else if(StringUtils.isNumeric(commandString)) {
              GeocodeResult chosenLocation = (GeocodeResult)_searchResults.getSuggestions().get(Integer.parseInt(commandString) - 1);

              if(chosenLocation != null) {
                _searchResults = _searchService.findStopsNearPoint(chosenLocation.getLatitude(), chosenLocation.getLongitude(), 
                    _resultFactory, _searchResults.getRouteIdFilter());
                _searchResults.setQueryLat(chosenLocation.getLatitude());
                _searchResults.setQueryLon(chosenLocation.getLongitude());
                commandString = null;
                continue;
              }
            }

          } else {
            _response = locationDisambiguationResponse(0);            
            break;
          }
        }
      }
      
      break;
    }
 
    // no response generated--no results or unrecognized query
    if(StringUtils.isEmpty(_response)) {
      _response = errorResponse("No results.");
        
      if(_googleAnalytics != null) {
        try {
          _googleAnalytics.trackEvent("SMS", "No Results", _query);
        } catch(Exception e) {
          //discard
        }
      }
    }
      
    return SUCCESS;
  }

  /**
   * RESPONSE GENERATION METHODS
   */
  private String serviceAlertResponse(int offset) throws Exception {
    if(offset >= _searchResults.getMatches().size()) {
      return errorResponse("No more.");
    }
    
    // worst case in terms of footer length
    String footer = "Send:\n";
    footer += "N for next alert\n";

    String body = ""; 
    int i = offset;
    while(i < _searchResults.getMatches().size()) {
      ServiceAlertResult serviceAlert = (ServiceAlertResult)_searchResults.getMatches().get(i);
      String textToAdd = serviceAlert.getAlert() + "\n\n";      

      // if the alert alone is too long, we have to chop it
      if(textToAdd.length() > MAX_SMS_CHARACTER_COUNT - footer.length() - 5) {
        textToAdd = textToAdd.substring(0, MAX_SMS_CHARACTER_COUNT - footer.length() - 22) + "... more at MTA.info\n\n";
      }
      
      if(body.length() + footer.length() + textToAdd.length() <= MAX_SMS_CHARACTER_COUNT) {
        body += textToAdd;
      } else {
        break;
      }
      
      i++;
    }
    _searchResultsCursor = i;    
    
    if(_googleAnalytics != null) {
      try {
      _googleAnalytics.trackEvent("SMS", "Service Alert", _query + " [" + _searchResults.getMatches().size() + "]");
      } catch(Exception e) {
        //discard
      }
    }
    
    if(i < _searchResults.getMatches().size()) {
      return body + footer;
    } else {
      return body;
    }    
  }
  
  // whether a route has scheduled service or not--always returns a message that fits into one SMS.
  private String routeResponse(RouteResult result) throws Exception {
    String header = result.getShortName() + "\n\n";

    String footer = "\nSend:\n";
    footer += "STOPCODE or INTERSECTION\n";      
    footer += "Add '" + result.getShortName() + "' for best results\n";
    
    RouteDirection aDirectionWithServiceAlerts = (RouteDirection)CollectionUtils.find(result.getDirections(), new Predicate() {
      
      @Override
      public boolean evaluate(Object object) {
        RouteDirection routeDirection = (RouteDirection)object;
        if (routeDirection.getSerivceAlerts().size() > 0) {
          return true;
        }
        return false;
      }
    });
    
    if (aDirectionWithServiceAlerts != null) {
      footer += "\nC " + result.getShortName() + " for *svc alert";
    }
    
    // find biggest headsign
    int routeDirectionTruncationLength = -1;
    for(RouteDirection direction : result.getDirections()) {
      routeDirectionTruncationLength = Math.max(routeDirectionTruncationLength, direction.getDestination().length());
    }

    // try to fit the entire headsign, but if we can't, start chopping from the longest one down
    String body = null;    
    while(body == null || header.length() + body.length() + footer.length() >= MAX_SMS_CHARACTER_COUNT) {
      body = "";
      for(RouteDirection direction : result.getDirections()) {
        String headsign = direction.getDestination();
        
        String alertString = "";
        if (direction.getSerivceAlerts().size() > 0) {
          alertString = "*";
        }

        if(headsign.length() + alertString.length() > routeDirectionTruncationLength) {
          body += "to " + headsign.substring(0, Math.min(routeDirectionTruncationLength, headsign.length())) + "..." + alertString;
        } else {
          body += "to " + headsign + alertString;
        }        
        body += "\n";
        
        if(direction.hasUpcomingScheduledService() == false) {
          body += "not scheduled\n";
        } else {
          body += "is scheduled\n";        
        }
      }
      
      routeDirectionTruncationLength--;
    }
    
    if(routeDirectionTruncationLength <= 0) {
      throw new Exception("Couldn't fit any route names!?");
    }
    
    if(_googleAnalytics != null) {
      try {
        _googleAnalytics.trackEvent("SMS", "Route Response", _query);
      } catch(Exception e) {
        //discard
      }
    }
    
    return header + body + footer;
  }
  
  private String locationDisambiguationResponse(int offset) throws Exception {
    if(offset >= _searchResults.getSuggestions().size()) {
      return errorResponse("No more.");
    }
    
    String header = "Did you mean?\n\n";
    
    String footer = "\n";        
    footer += "Send:\n";
    footer += "1-" + _searchResults.getSuggestions().size() + "\n";

    // the worst case in terms of length for footer
    String moreFooter = footer + "M for more\n";
    
    String body = ""; 
    int i = offset;
    while(i < _searchResults.getSuggestions().size()) {
      GeocodeResult serviceAlert = (GeocodeResult)_searchResults.getSuggestions().get(i);

      String textToAdd = (i + 1) + ") " + serviceAlert.getFormattedAddress() + "\n";
      textToAdd += "(" + serviceAlert.getNeighborhood() + ")\n";

      if(header.length() + body.length() + moreFooter.length() + textToAdd.length() < MAX_SMS_CHARACTER_COUNT) {
        body += textToAdd;
      } else {
        break;
      }
      
      i++;
    }
    _searchResultsCursor = i;    
          
    if(_googleAnalytics != null) {
      try {
        _googleAnalytics.trackEvent("SMS", "Location Disambiguation", _query + " [" + _searchResults.getSuggestions().size() + "]");
      } catch(Exception e) {
        //discard
      }
    }
    
    if(i < _searchResults.getSuggestions().size()) {
      return header + body + moreFooter;
    } else {
      return header + body + footer;
    }
  }
    
  private String multipleStopResponse() throws Exception {
    
    String b = "";
    
    AgencyAndId id = null;
    if (_searchResults.getRouteIdFilter() != null && _searchResults.getRouteIdFilter().size() > 0) {
      id = AgencyAndIdLibrary.convertFromString((String)_searchResults.getRouteIdFilter().toArray()[0]);
    }
    
    if (id != null && !getRoutesInSearchResults().contains(id.getId())) {
      b += "No " + id.getId() + " stops nearby\n\n";
      b += "Routes nearby:\n\n";
    } else {
      b += _searchResults.getMatches().size() + " stops nearby\n\n";
      b += "Pick a route:\n\n";
    }
    
    for (String route : getRoutesInSearchResults()) {
      b += route + "\n";
    }
    
    b += "\nSend:\n";
    b += "ROUTE";
    
    if(_googleAnalytics != null) {
      try {
        _googleAnalytics.trackEvent("SMS", "Stop Realtime Response For Multiple Stops", _query);
      } catch(Exception e) {
        //discard
      }
    }
    
    return b;
  }
  
  private String directionDisambiguationResponse() throws Exception {
    
    filterStopSearchResultsToRouteFilterAndDirection();
    
    AgencyAndId id = AgencyAndIdLibrary.convertFromString((String)_searchResults.getRouteIdFilter().toArray()[0]);
    
    String a = null;
    if (id.getId().toUpperCase().matches("^(A|E|I|O|U).*$")) {
      a = "an";
    } else {
      a = "a";
    }
    
    String header = "Pick " + a + " " + id.getId() + " direction:\n\n";
    
    List<String> choices = new ArrayList<String>();
    List<String> choiceNumbers = new ArrayList<String>();
    
    for (SearchResult searchResult : _searchResults.getMatches()) {
      StopResult stopResult = (StopResult)searchResult;
      
      RouteAtStop routeAtStopInFilter = (RouteAtStop)CollectionUtils.find(stopResult.getRoutesAvailable(), new Predicate() {
        
        @Override
        public boolean evaluate(Object object) {
          RouteAtStop routeAtStop = (RouteAtStop)object;
          if (routeAtStop.getRoute().getId().equals(_searchResults.getRouteIdFilter().toArray()[0])) {
            return true;
          }
          return false;
        }
      });
      
      String destination = routeAtStopInFilter.getDirections().get(0).getDestination();
      
      int choiceNumber = _searchResults.getMatches().indexOf(searchResult) + 1;
      choiceNumbers.add(String.valueOf(choiceNumber));
      choices.add(choiceNumber + ") " + destination);
    }
    
    String footer = "Send:\n";
    footer += StringUtils.join(choiceNumbers, " or ");
    
    // find biggest choice
    int choiceTruncationLength = -1;
    for(String choice : choices) {
      choiceTruncationLength = Math.max(choiceTruncationLength, choice.length());
    }

    // try to fit the entire choice, but if we can't, start chopping from the longest one down
    String body = null;    
    while(body == null || header.length() + body.length() + footer.length() >= MAX_SMS_CHARACTER_COUNT) {
      body = "";
      for(String choice : choices) {

        if(choice.length() > choiceTruncationLength) {
          body += choice.substring(0, Math.min(choiceTruncationLength, choice.length())) + "...";
        } else {
          body += choice;
        }        
        body += "\n\n";
      }
      
      choiceTruncationLength--;
    }
    
    if(choiceTruncationLength <= 0) {
      throw new Exception("Couldn't fit any direction choice names!?");
    }
    
    if(_googleAnalytics != null) {
      try {
        _googleAnalytics.trackEvent("SMS", "Direction Disambiguation Response", _searchResults.getRouteIdFilter().toArray()[0].toString());
      } catch(Exception e) {
        //discard
      }
    }
    
    return header + body + footer;
  }
      
  private String singleStopResponse(String message) throws Exception {
    
    if (message == null) {
      message = "";
    }
    message = message.trim();
    if(!message.isEmpty()) {
      message = "\n" + message + "\n";
    }
    
    StopResult stopResult = (StopResult)_searchResults.getMatches().get(0);

    String header = "Stop " + stopResult.getIdWithoutAgency() + "\n\n";

    String footer = "\nSend:\n";
    footer += "R for refresh\n";
    if (_searchResults.getRouteIdFilter().isEmpty() && stopResult.getStop().getRoutes().size() > 1) {
      footer += stopResult.getIdWithoutAgency() + "+ROUTE for bus info\n";
    }

    // worst case for footer length
    String alertsFooter = footer + "C+ROUTE for *svc alert\n";

    // body content for stops
    String body = "";
    if(stopResult.getRoutesAvailable().size() == 0) {
      // if we found a stop with no routes because of a stop+route filter, 
      // indicate that specifically
      if(_searchResults.getRouteIdFilter().size() > 0) {
        body += "No filter matches\n";
      } else {
        body += "No routes\n";
      }
    } else {
      // bulid map of sorted vehicle observation strings for this stop, sorted by closest->farthest
      TreeMap<Double, String> observationsByDistanceFromStopAcrossAllRoutes = 
          new TreeMap<Double, String>();
      // Keep track of not scheduled and not en route so we can display that later
      Set<String> notScheduledRoutes = new HashSet<String>();
      Set<String> notEnRouteRoutes = new HashSet<String>();
      
      for(RouteAtStop routeHere : stopResult.getRoutesAvailable()) {
        
        if (_searchResults.getRouteIdFilter() != null && !_searchResults.getRouteIdFilter().isEmpty() && !_searchResults.getRouteIdFilter().contains(routeHere.getRoute().getId())) {
          continue;
        }
        
        for(RouteDirection direction : routeHere.getDirections()) {
          String prefix = "";
          if(!direction.getSerivceAlerts().isEmpty()) {
            footer = alertsFooter;
            prefix += "*";
          }            
          prefix += routeHere.getShortName();
          
          if(!direction.hasUpcomingScheduledService() && direction.getDistanceAways().isEmpty()) {
            notScheduledRoutes.add(prefix);
          } else {
            if(!direction.getDistanceAways().isEmpty()) {
              HashMap<Double, String> sortableDistanceAways = direction.getDistanceAwaysWithSortKey();
              for(Double distanceAway : sortableDistanceAways.keySet()) {
                String distanceAwayString = sortableDistanceAways.get(distanceAway);
                
                observationsByDistanceFromStopAcrossAllRoutes.put(distanceAway, prefix + ": " + distanceAwayString);
              }
            } else {
              notEnRouteRoutes.add(prefix);
            }
          }
        }
      }

      // if there are no upcoming buses, provide info about the routes that are not en route or not scheduled
      if(observationsByDistanceFromStopAcrossAllRoutes.isEmpty()) {
        if(notEnRouteRoutes.size() > 0) {
          body += StringUtils.join(notEnRouteRoutes, ",") + ": no buses en-route\n";
        }
        if(notScheduledRoutes.size() > 0) {
          body += StringUtils.join(notScheduledRoutes, ",") + ": not scheduled\n";
        }       
      // as many observations as will fit, sorted by soonest to arrive out
      } else {      
        for(String observationString : observationsByDistanceFromStopAcrossAllRoutes.values()) {
          String textToAdd = observationString + "\n";      
  
          if(message.length() + body.length() + header.length() + alertsFooter.length() + textToAdd.length() < MAX_SMS_CHARACTER_COUNT) {
            body += textToAdd;
          } else {
            break;
          }
        }
      }
    }

    if(_googleAnalytics != null) {
      try {
        _googleAnalytics.trackEvent("SMS", "Stop Realtime Response for Single Stop", _query);
      } catch(Exception e) {
        //discard
      }
    }
    
    return header + body + message + footer;
  }
  
  private String singleStopWrongFilterNearbyResponse() {
    
    StopResult searchedForStop = (StopResult)_searchResults.getMatches().get(0);
    
    String body = "Stop " + searchedForStop.getIdWithoutAgency() + ":\n";
    
    List<String> searchedForStopRoutes = new ArrayList<String>();
    for (RouteBean route : searchedForStop.getStop().getRoutes()) {
      searchedForStopRoutes.add(route.getShortName());
    }
    
    body += StringUtils.join(searchedForStopRoutes, " ") + "\n\n";
    
    AgencyAndId id = AgencyAndIdLibrary.convertFromString((String)_searchResults.getRouteIdFilter().toArray()[0]);
    body += "No " + id.getId() + " at this stop\n\n";
    
    body += "Send:\n";
    body += "N for nearby " + id.getId() + " stops\n";
    body += searchedForStop.getIdWithoutAgency() + "+ROUTE for bus info";
    
    if(_googleAnalytics != null) {
      try {
        _googleAnalytics.trackEvent("SMS", "Stop Realtime Response for Single Stop with Bad Filter", _query);
      } catch(Exception e) {
        //discard
      }
    }
    
    return body;
  }
  
  private String didYouMeanResponse() {
    String header = "Did you mean?\n\n";

    String footer = "\nSend:\n";
    footer += "ROUTE for schedule info\n";      

    String body = "";
    for(SearchResult _route : _searchResults.getSuggestions()) {
      RouteResult route = (RouteResult)_route;
      body += route.getShortName() + " ";
    }
    body = body.trim();
    body += "\n";
    
    if(_googleAnalytics != null) {
      try {
        _googleAnalytics.trackEvent("SMS", "Did You Mean Response", _query);
      } catch(Exception e) {
        //discard
      }
    }
    
    return header + body + footer;
  }
  
  private String errorResponse(String message) throws Exception {
    String staticStuff = "Send:\n\n";
    
    staticStuff += "STOPCODE or\n";
    staticStuff += "INTERSECTION\n";
    staticStuff += "Add ROUTE for best results\n\n";
    
    staticStuff += "Examples:\n";
    staticStuff += "'PARK AV AND 21 ST X1'\n";
    staticStuff += "'400145 X1'\n\n";
    
    staticStuff += "Find 6-digit stopcode on bus stop pole";

    if(_googleAnalytics != null) {
      try {
        _googleAnalytics.trackEvent("SMS", "Error Response", _query);
      } catch(Exception e) {
        //discard
      }
    }
    
    if(message != null) {
      if(staticStuff.length() + 1 + message.length() > MAX_SMS_CHARACTER_COUNT) {
        throw new Exception("Error message text is too long.");
      }

      return message + " " + staticStuff;
    } else {
      return staticStuff;
    }
  }

  /**
   * PRIVATE HELPER METHODS
   */
  private String getQuery(String query) {
    if(query == null) {
      return null;
    }
    
    query = query.trim();

    // if this is a command prefix, one with a parameter, the command is "C", the query is the
    // "parameter".
    if(query.toUpperCase().startsWith("C ") || query.toUpperCase().startsWith("C+")) {        
      return query.substring(2);
    }

    // if this is a command, no query can be pressent
    if(getCommand(query) != null) {
      return null;
    }
    
    return query;
  }
  
  private String getCommand(String query) {
    if(query == null) {
      return null;
    }
    
    query = query.trim();
    
    if (query.toUpperCase().equals("R")) {
      return "R";
    }

    if (query.toUpperCase().equals("M")) {
      return "M";
    }

    if (query.toUpperCase().startsWith("C ") || query.toUpperCase().startsWith("C+")) {        
      return "C";
    }
    
    // if we have nearby stops and the user wants to see them
    if (query.toUpperCase().equals("N") && (_searchResults.getResultType().equals("StopResult") || _searchResults.getResultType().equals("ServiceAlertResult"))) {
      return "N";
    }
    
    // Check if it's a route currently in our result set
    if (query != null && _searchResults != null && "StopResult".equals(_searchResults.getResultType()) && getRoutesInSearchResults().contains(query.toUpperCase())) {
      return query.toUpperCase();
    }

    // try as ambiguous location result choice:
    try {
      // no results means this can't be a pick from ambiguous location results
      if (_searchResults == null || _searchResults.isEmpty()) {
        return null;
      }
      
      // a list of things other than location results can't be a pick from ambiguous locations.
      if (!(_searchResults.getResultType().equals("GeocodeResult")) && !(_searchResults.getResultType().equals("StopResult"))) {
        return null;
      }
      
      // is the choice within the bounds of the number of things we have? Either geocode suggestions or search matches
      Integer choice = Integer.parseInt(query);
      if (choice >= 1 && (choice <= _searchResults.getSuggestions().size() || choice <= _searchResults.getMatches().size())) {
        return choice.toString();
      }
    } catch (NumberFormatException e) {
      return null;
    }

    return null;
  }
  
  private SortedSet<String> getRoutesInSearchResults() {
    SortedSet<String> routes = new TreeSet<String>();
    if (_searchResults != null) {
      for (SearchResult searchResult : _searchResults.getMatches()) {
        StopResult stopResult = (StopResult)searchResult;
        for (RouteBean routeBean : stopResult.getStop().getRoutes()) {
          routes.add(routeBean.getShortName().toUpperCase());
        }
      }
    }
    return routes;
  }
  
  private void filterStopSearchResultsToRouteFilterAndDirection() {
    
    CollectionUtils.filter(_searchResults.getMatches(), new Predicate() {
      
      private Set<String> directionsInResults = new HashSet<String>();
      
      @Override
      public boolean evaluate(Object arg0) {
        StopResult stopResult = (StopResult)arg0;
        for (RouteAtStop routeAtStop : stopResult.getRoutesAvailable()) {
          if (_searchResults.getRouteIdFilter().contains(routeAtStop.getRoute().getId()) && !directionsInResults.contains(routeAtStop.getDirections().get(0).getDestination())) {
            directionsInResults.add(routeAtStop.getDirections().get(0).getDestination());
            return true;
          }
        }
        return false;
      }
    });
  }
  
  /**
   * METHODS FOR VIEWS
   */
  public String getResponse() {
    
    String response = _response.trim();
    
    if (_needsGlobalAlert != null && _needsGlobalAlert) {
      List<ServiceAlertBean> globalAlerts = _realtimeService.getServiceAlertsGlobal();
      if (globalAlerts != null && globalAlerts.size() > 0) {
        String alertsThatFit = "\n\nService Notice: ";
        String end = "... More at mta.info";
        for (ServiceAlertBean alert : globalAlerts) {
          
          @SuppressWarnings("unchecked")
          Collection<String> descriptions = CollectionUtils.collect(alert.getDescriptions(), new Transformer() {
            @Override
            public Object transform(Object input) {
              return ((NaturalLanguageStringBean)input).getValue();
            }
          });
          
          String descriptionString = StringUtils.join(descriptions, "\n\n");
          
          if (response.length() + descriptionString.length() + alertsThatFit.length() > MAX_SMS_CHARACTER_COUNT*2) {
            int endIndex = MAX_SMS_CHARACTER_COUNT*2 - response.length() - alertsThatFit.length() - end.length();
            descriptionString = descriptionString.substring(0, endIndex) + end;
            alertsThatFit += descriptionString;
            break;
          }
          alertsThatFit += descriptionString + "\n\n";
        }
        response += alertsThatFit;
      }
      _needsGlobalAlert = false;
    }
    
    syncSession();
    
    return response;
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    // Initialize Google Analytics
    String googleAnalyticsSiteId = 
        _configurationService.getConfigurationValueAsString("display.googleAnalyticsSiteId", null);    
    try {
      if(googleAnalyticsSiteId != null) {    
        AnalyticsConfigData config = new AnalyticsConfigData(googleAnalyticsSiteId, _visitorCookie);
        _googleAnalytics = new JGoogleAnalyticsTracker(config, GoogleAnalyticsVersion.V_4_7_2);
      }
    } catch(Exception e) {
      // discard
    }
    
  }
}
