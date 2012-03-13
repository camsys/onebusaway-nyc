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

import org.onebusaway.nyc.geocoder.service.NycGeocoderService;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion;

import org.springframework.beans.factory.annotation.Autowired;

public class IndexAction extends SessionedIndexAction {
  
  private static final long serialVersionUID = 1L;

  private static final int MAX_SMS_CHARACTER_COUNT = 160;
  
  @Autowired
  private RealtimeService _realtimeService;

  @Autowired
  private ConfigurationService _configurationService;

  @Autowired
  private NycGeocoderService _geocoderService;
  
  private JGoogleAnalyticsTracker _googleAnalytics = null;
  
  /* response to user */
  private String _response = null;

  public String execute() throws Exception {
    String googleAnalyticsSiteId = 
        _configurationService.getConfigurationValueAsString("display.googleAnalyticsSiteId", null);
        
    if(googleAnalyticsSiteId != null) {    
      AnalyticsConfigData config = new AnalyticsConfigData(googleAnalyticsSiteId, _visitorCookie);
      _googleAnalytics = new JGoogleAnalyticsTracker(config, GoogleAnalyticsVersion.V_4_7_2);
      _googleAnalytics.trackPageView("/sms", "New SMS Session", "");
    }
    
//    SmsPresentationModelFactory factory = new SmsPresentationModelFactory(_realtimeService, _configurationService);
//    _stopSearchService.setModelFactory(factory);
//    _routeSearchService.setModelFactory(factory);    
    
    /**
     * INPUT PARSING
     */  
//    String commandString = findAndNormalizeCommandInQuery(_query);
//    
//    if(commandString != null) {
//      // (service) change
//      if(commandString.equals("C")) {
//        serviceAlertResponse(_query);
//        return SUCCESS;
//
//      // help
//      } else if(commandString.equals("H")) {
//        errorResponse(null);
//        return SUCCESS;
//
//      // refresh: just re-run the last query!
//      } else if(commandString.equals("R")) {
//        _query = _lastQuery;
//        generateNewSearchResults(_query);
//        
//      // more: (handled as part of regular logic)
//      } else if(commandString.equals("M")) {
////        if(_searchResultsCursor >= _searchResults.size()) {
//          errorResponse("No more.");
//          return SUCCESS;
//        }
//
//        // (fall through)
//
//      // 1-X: ambiguous location choice
//      } else {
//        // (the command will always be a number if we're here)
//        Integer choice = Integer.parseInt(commandString);
////        LocationResult pickedResult = (LocationResult)_searchResults.get(choice - 1);
//
////        _query = pickedResult.getFormattedAddress();
//        generateNewSearchResults(_query);
//      }

    // if not a command, we must be a query
//    } else
//      generateNewSearchResults(_query);

    // filter results
//    _searchResults = filterResults(_searchResults, _query);
    
    /**
     * GIVEN RESULTS, DISPLAY THEM TO THE USER
     */    
    // display results to user
//    if(_searchResults.size() == 0) {
//      errorResponse("No matches.");
//      
//      if(_googleAnalytics != null)
//        _googleAnalytics.trackEvent("SMS", "No Results", _query);
//      
//    } else {
//      if(_searchResults.getTypeOfResults().equals("StopResult")) {
//
//        // try to fill the most space in the SMS with realtime data; if we can't even get
//        // one observation for each route available in there, then punt to stop disambiguation.
//        boolean truncated = true;
//        for(int i = 1; truncated == true; i++) {
//          truncated = stopRealtimeResponse(_searchResults, i);            
//          
//          if(_response.length() > MAX_SMS_CHARACTER_COUNT) {
//            if(i == 1) {
//              _response = null;
//            } else {
//              truncated = stopRealtimeResponse(_searchResults, i - 1);            
//            }
//
//            break;
//          }
//        }
//
//        // okay, so we have to disambiguate--now find the maximum page size we can fit into 
//        // one SMS!
//        if(_response == null) {
//          truncated = true;
//          int i = 0;
//          for(i = 1; truncated == true; i++) {
//            truncated = stopDisambiguationResponse(_searchResults, _searchResultsCursor, i);
//
//            if(_response.length() > MAX_SMS_CHARACTER_COUNT) {
//              if(i == 1) {
//                _response = null;
//              } else {
//                truncated = stopDisambiguationResponse(_searchResults, _searchResultsCursor, i - 1);
//              }
//              break;
//            }
//          }
//
//          _searchResultsCursor = i;
//        }
//                  
//        // shouldn't get here!
//        if(_response == null)
//          errorResponse("Too general.");
//
//      } else if(_searchResults.getTypeOfResults().equals("LocationResult")) {
//        locationDisambiguationResponse(_searchResults, false);
//        
//        // too long? try compact mode!
//        if(_response.length() > MAX_SMS_CHARACTER_COUNT) {
//          locationDisambiguationResponse(_searchResults, true); 
//        }
//
//        // still too long? sorry...
//        if(_response.length() > MAX_SMS_CHARACTER_COUNT) {
//          errorResponse("Too general.");
//        }
//      } else if(_searchResults.getTypeOfResults().equals("RouteResult")) {
//        routeResponse(_searchResults, false);
//        
//        if(_response.length() > MAX_SMS_CHARACTER_COUNT) {
//          routeResponse(_searchResults, true);
//        }
//
//        // still too long? sorry...
//        if(_response.length() > MAX_SMS_CHARACTER_COUNT) {
//          errorResponse("Error.");
//        }
//      }
//    }
//    
    return SUCCESS;
  }

  /**
   * RESPONSE GENERATION METHODS
   */
//  private void serviceAlertResponse(String _q) throws Exception {
//    List<String> routeTokens = findRouteTokensInQuery(_q);
//    
//    if(routeTokens.size() != 1) {
//      errorResponse("Not found.");
//      return;      
//    }

//    List<RouteResult> routeResult = _routeSearchService.resultsForQuery(routeTokens.get(0));
//    
//    if(routeResult.size() == 0) {
//      errorResponse("Not found.");
//      return;
//    }
//
//    List<NaturalLanguageStringBean> alerts = new ArrayList<NaturalLanguageStringBean>();
//      
//    for(SearchResult _result : routeResult) {
//      RouteResult result = (RouteResult)_result;
//      for(RouteDestinationItem _destination : result.getDestinations()) {
//        SmsRouteDestinationItem destination = (SmsRouteDestinationItem)_destination;
//        alerts.addAll(destination.getServiceAlerts());
//      }
//    }
//    
//    if(alerts.size() == 0) {
//      errorResponse("No alerts.");
//      return;
//    }
//
//    // Note we use a set here so that the list gets deduped.
//    Set<String> alertValues = new HashSet<String>(alerts.size());
//    for (NaturalLanguageStringBean alert: alerts) {
//      alertValues.add(alert.getValue());
//    }
//    
//    _response = StringUtils.join(alertValues, "\n\n");
//    
//    if(_googleAnalytics != null)
//      _googleAnalytics.trackEvent("SMS", "Service Alert", _query + " [" + routeResult.size() + "]");
//  }
  
//  private void routeResponse(List<SearchResult> results, boolean truncate) {
//    _response = "";
//    
//    RouteResult result = (RouteResult)results.get(0);
//
//    _response += result.getRouteIdWithoutAgency() + "\n\n";
//    
//    for(RouteDestinationItem destination : result.getDestinations()) {
//      if(truncate) {
//        _response += destination.getHeadsign().substring(0, 15) + "...\n";
//      } else {
//        _response += destination.getHeadsign() + "\n";
//      }
//      
//      if(destination.getHasUpcomingScheduledService() == false) {
//        _response += "not scheduled\n";
//      } else {
//        _response += "is scheduled\n";        
//      }
//    }
//
//    _response += "\n";    
//    
//    _response += "Send:\n";
//    _response += "STOP CODE or INTERSECTION\n";
//      
//    _response += "Add '" + result.getRouteIdWithoutAgency() + "' for best results\n";
//    
//    if(_googleAnalytics != null)
//      _googleAnalytics.trackEvent("SMS", "Route Response", _query + " [" + _searchResults.size() + "]");
//  }
//  
//  private void locationDisambiguationResponse(List<SearchResult> results, boolean compactMode) {
//    _response = "Did you mean\n\n";
//    
//    int i = 1;
//    for(SearchResult _result : results) {
//      LocationResult result = (LocationResult)_result;
//      
//      // compact mode omits neighborhood, city/borough and whitespace to save space!
//      if(compactMode) {
//        _response += i + ") " + result.getAddress() + " " + result.getPostalCode() + "\n";
//      } else {
//        _response += i + ") " + result.getAddress() + "\n";        
//        _response += result.getCity() + " " + result.getPostalCode() + "\n";
//
//        if(results.size() <= 2) {
//          if(result.getNeighborhood() != null) {
//            _response += "(" + result.getNeighborhood() + ")\n";
//          }
//
//          // if we have few results, throw some whitespace in for nicer formatting
//          if(results.iterator().hasNext()) {
//            _response += "\n";
//          }
//        }
//      }
//      
//      i++;
//    }
//
//    _response += "\n";    
//    
//    _response += "Send:\n";
//    _response += "1-" + (i - 1) + "\n";
//      
//    if(_googleAnalytics != null)
//      _googleAnalytics.trackEvent("SMS", "Location Disambiguation", _query + " [" + _searchResults.size() + "]");
//  }
//  
//  private boolean stopRealtimeResponse(List<SearchResult> results, int realtimeObservationCount) {
//    boolean truncated = false;
//
//    _response = "";
//    
//    if(results.size() > 1) {
//      _response += results.size() + " stop" + ((results.size() != 1) ? "s" : "") + " here\n\n";
//    }
//    
//    List<String> routesWithAlerts = new ArrayList<String>();
//    for(SearchResult _result : results) {
//      StopResult result = (StopResult)_result;
//
//      if(results.size() > 1) {
//        _response += result.getStopDirection() + "-bound: " + result.getStopIdWithoutAgency() + "\n";
//      } else {
//        _response += result.getStopIdWithoutAgency() + "\n";
//      }
//      
//      if(result.getRoutesAvailable().size() > 0) {
//        for(RouteResult routeHere : result.getRoutesAvailable()) {
//          for(RouteDestinationItem _destination : routeHere.getDestinations()) {
//            SmsRouteDestinationItem destination = (SmsRouteDestinationItem)_destination;
//
//            String prefix = "";
//
//            if(destination.getServiceAlerts().size() > 0) {
//              prefix += "*";
//              routesWithAlerts.add(routeHere.getRouteIdWithoutAgency());
//            }            
//
//            prefix += routeHere.getRouteIdWithoutAgency() + " ";
//
//            if(destination.getDistanceAwayStrings().size() > 0) {
//              int c = 0;
//              for(String distanceAway : destination.getDistanceAwayStrings()) {
//                if(c > realtimeObservationCount) {
//                  truncated = true;
//                  break;
//                }
//
//                if(c > 0) {
//                  _response += "\n";
//                } 
//
//                _response += prefix + distanceAway;
//
//                c++;
//              }
//            } else {
//              if(_destination.getHasUpcomingScheduledService() == false) {
//                _response += prefix + "not scheduled";                
//              } else {
//                _response += prefix + "no bus en-route";
//              }
//            }
//
//            _response += "\n";
//          } // for destinations
//        } // for routes
//      } else {
//        _response += "No matches.\n";
//      }
//    }
//    
//    _response += "\n";
//    _response += "Send:\n";
//    _response += "STOP CODE + ROUTE realtime info\n";
//
//    if(routesWithAlerts.size() > 0) {
//      _response += "'C' + ROUTE service change (*)\n";
//    }
//    
//    if(_googleAnalytics != null)
//      _googleAnalytics.trackEvent("SMS", "Stop Realtime Response", _query + " [" + results.size() + "]");
//
//    return truncated;
//  }
//  
//  private boolean stopDisambiguationResponse(List<SearchResult> results, int offset, int count) {
//    boolean truncated = false;
//
//    _response = "";
//    
//    String responseBody = "";
//    List<String> routesWithAlerts = new ArrayList<String>();
//    boolean haveNSSMRoute = false;
//    int c = 0;
//    for(SearchResult _result : results) {
//      // pagination
//      if(c < offset) {
//        c++;
//        continue;
//      }
//      
//      if(c > count) {
//        truncated = true;
//        break;
//      }
//      
//      StopResult result = (StopResult)_result;
//      
//      // header of item
//      if(results.size() > 1) {
//        if(result.getStopDirection() != null) {
//          responseBody += result.getStopDirection() + "-bound: " + result.getStopIdWithoutAgency() + "\n";
//        } else {
//          responseBody += result.getStopIdWithoutAgency() + "\n";
//        }
//      } else {
//        responseBody += result.getStopIdWithoutAgency() + "\n"; 
//        responseBody += "Routes here:\n";
//      }
//
//      // routes available at stop
//      for(RouteResult routeHere : result.getRoutesAvailable()) {
//        for(RouteDestinationItem _destination : routeHere.getDestinations()) {
//          SmsRouteDestinationItem destination = (SmsRouteDestinationItem)_destination;
//            
//          if(_destination.getHasUpcomingScheduledService() == false) {
//            responseBody += "#";
//            haveNSSMRoute = true;
//          }
//          
//          if(destination.getServiceAlerts().size() > 0) {
//            responseBody += "*";
//            routesWithAlerts.add(routeHere.getRouteIdWithoutAgency());
//          }            
//
//          responseBody += routeHere.getRouteIdWithoutAgency() + "  ";
//        }
//      }
//      
//      responseBody += "\n";
//
//      c++;
//    }
//
//    if(haveNSSMRoute) {
//      responseBody += "#=not scheduled\n";
//    }
//    
//    // if we have more than one page, include ordinal numbers 
//    if(offset + c < results.size() || offset > 0) {
//      _response += "Stops " + (offset + 1) + "-" + c + " of " + results.size() + "\n\n"; 
//    } else {
//      if(results.size() > 1) {
//        _response += results.size() + " stop" + ((results.size() != 1) ? "s" : "") + " here\n\n";
//      }
//    }
//
//    _response += responseBody;    
//    _response += "\n";
//    _response += "Send:\n";
//
//    if(offset + c < results.size()) {
//      _response += "MORE more stops\n";
//    }
//    
//    _response += "STOP CODE + ROUTE realtime info\n";
//
//    if(routesWithAlerts.size() > 0) {
//      _response += "'C' + ROUTE service change (*)\n";
//    }
//        
//    if(_googleAnalytics != null)
//      _googleAnalytics.trackEvent("SMS", "Stop Disambiguation", _query + " [" + results.size() + "]");
//
//    return truncated;
//  }
//      
//  private void errorResponse(String message) throws Exception {
//    String staticStuff = "Text your:\n\n";
//    
//    staticStuff += "STOP CODE or\n";
//    staticStuff += "INTERSECTION\n\n";
//    
//    staticStuff += "Add ROUTE for best results:\n";
//    staticStuff += "'S74 MAIN AND CRAIG'\n";
//    staticStuff += "'200884 S44'\n\n";
//    
//    staticStuff += "Find 6-digit stop code on bus stop pole.";
//
//    if(message != null) {
//      if(staticStuff.length() + 1 + message.length() > MAX_SMS_CHARACTER_COUNT) {
//        throw new Exception("Error message text is too long.");
//      }
//
//      _response = message + " " + staticStuff;
//    } else
//      _response = staticStuff;
//  }
//
//  /**
//   * PRIVATE HELPER METHODS
//   */
//  private SearchResultCollection filterResults(SearchResultCollection results, String q) {    
//    List<String> routesToFilterBy = findRouteTokensInQuery(q);
//
//    if(routesToFilterBy.size() == 0) {
//      return results;
//    }
//
//    SearchResultCollection newResults = new SearchResultCollection();
//    for(SearchResult _result : results) {
//      if (_result instanceof StopResult) {
//        StopResult result = (StopResult)_result;
//        
//        List<RouteResult> newRoutesAvailable = new ArrayList<RouteResult>();
//        for(RouteResult routeAvailable : result.getRoutesAvailable()) {
//          if(routesToFilterBy.contains(routeAvailable.getRouteIdWithoutAgency())) {
//            newRoutesAvailable.add(routeAvailable);
//          }
//        }
//        
//        result.getRoutesAvailable().clear();
//        result.getRoutesAvailable().addAll(newRoutesAvailable);
//        
//        newResults.add(result);
//
//      // pass through
//      } else {
//        newResults.add(_result);
//      }
//    }
//       
//    return newResults;
//  }
//  
//  public String findAndNormalizeCommandInQuery(String query) {
//    if(query == null) {
//      return null;
//    }
//    
//    query = query.trim();
//    
//    if(query.toUpperCase().startsWith("C") || query.toUpperCase().startsWith("CHANGE")) {
//      return "C";
//    }
//
//    if(query.toUpperCase().equals("R") || query.toUpperCase().equals("REFRESH")) {
//      return "R";
//    }
//
//    if(query.toUpperCase().equals("M") || query.toUpperCase().equals("MORE")) {
//      return "M";
//    }
//
//    if(query.toUpperCase().equals("H") || query.toUpperCase().equals("HELP")) {
//      return "H";
//    }
//
//    // try as ambiguous location result choice:
//    try {
//      // no results means this can't be a pick from ambiguous location results
//      if(_searchResults == null || _searchResults.size() == 0) {
//        return null;
//      }
//      
//      // a list of things other than location results can't be a pick from ambiguous locations.
//      if(!(_searchResults.getTypeOfResults().equals("LocationResult"))) {
//        return null;
//      }
//      
//      // is the choice within the bounds of the number of things we have?
//      Integer choice = Integer.parseInt(query);
//      if(choice >= 1 && choice <= _searchResults.size()) {
//        return choice.toString();
//      }
//    } catch (NumberFormatException e) {
//      return null;
//    }
//
//    return null;
//  }
//  
//  private List<String> findRouteTokensInQuery(String q) {
//    List<String> output = new ArrayList<String>();
//    
//    if(q == null) {
//      return output;
//    }
//    
//    for(String token : q.split(" ")) {
//      if(_routeSearchService.isRoute(token)) {
//        output.add(token);
//      }
//    }
//
//    return output;
//  }
//  
//  private void generateNewSearchResults(String q) {
//    _searchResults.clear();
//    _searchResultsCursor = 0;
//
//    if(q == null || q.isEmpty()) {
//      return;
//    }
//
//    _lastQuery = q;
//
//    // try as stop ID
//    _searchResults.addAll(_stopSearchService.resultsForQuery(q));
//
//    // try as route ID
//    if(_searchResults.size() == 0)
//      _searchResults.addAll(_routeSearchService.resultsForQuery(q));
//
//    // nothing? geocode it!
//    if(_searchResults.size() == 0)
//      _searchResults.addAll(generateResultsFromGeocode(q));        
//    
//    Collections.sort(_searchResults, new SearchResultComparator());
//  } 
//  
//  private List<SearchResult> generateResultsFromGeocode(String q) {
//    List<SearchResult> results = new ArrayList<SearchResult>();
//
//    List<NycGeocoderResult> geocoderResults = _geocoderService.nycGeocode(q);
//    
//    // if we got a location that is a point, return stops nearby
//    if(geocoderResults.size() == 1) {
//      NycGeocoderResult result = geocoderResults.get(0);
//
//      if(!result.isRegion()) {
//        results.addAll(_stopSearchService.resultsForLocation(result.getLatitude(), result.getLongitude()));
//      }
//
//    // prompt the user to choose from non-regional locations.
//    } else {
//      for(NycGeocoderResult result : geocoderResults) {
//        if(!result.isRegion()) {
//          results.add(new LocationResult(result));
//        }
//      }
//    }
//
//    return results;
//  }
    
  /**
   * METHODS FOR VIEWS
   */
  public String getResponse() {
    syncSession();
    
    return _response;
  }
}
