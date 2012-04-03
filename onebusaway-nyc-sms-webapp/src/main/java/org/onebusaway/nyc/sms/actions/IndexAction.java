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

import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.nyc.presentation.model.SearchResultCollection;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.realtime.ScheduledServiceService;
import org.onebusaway.nyc.presentation.service.search.SearchResultFactory;
import org.onebusaway.nyc.presentation.service.search.SearchService;
import org.onebusaway.nyc.sms.actions.model.GeocodeResult;
import org.onebusaway.nyc.sms.actions.model.RouteAtStop;
import org.onebusaway.nyc.sms.actions.model.RouteDirection;
import org.onebusaway.nyc.sms.actions.model.RouteResult;
import org.onebusaway.nyc.sms.actions.model.ServiceAlertResult;
import org.onebusaway.nyc.sms.actions.model.StopResult;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.services.TransitDataService;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

public class IndexAction extends SessionedIndexAction {
  
  private static final long serialVersionUID = 1L;

  private static final int MAX_SMS_CHARACTER_COUNT = 160;
  
  @Autowired
  private RealtimeService _realtimeService;

  @Autowired
  private ConfigurationService _configurationService;

  @Autowired
  private SearchService _searchService;

  @Autowired
  private ScheduledServiceService _scheduledServiceService;

  @Autowired
  private TransitDataService _transitDataService;

  private JGoogleAnalyticsTracker _googleAnalytics = null;
  
  private String _response = null;
  
  public String execute() throws Exception {
    
    SearchResultFactory _resultFactory = new SearchResultFactoryImpl(_transitDataService,
        _scheduledServiceService, _realtimeService, _configurationService);

    // send an initial visit to google analytics to tie our events to
    String googleAnalyticsSiteId = 
        _configurationService.getConfigurationValueAsString("display.googleAnalyticsSiteId", null);
        
    try {
      if(googleAnalyticsSiteId != null) {    
        AnalyticsConfigData config = new AnalyticsConfigData(googleAnalyticsSiteId, _visitorCookie);
        _googleAnalytics = new JGoogleAnalyticsTracker(config, GoogleAnalyticsVersion.V_4_7_2);
        _googleAnalytics.trackPageView("/sms", "New SMS Session", "");
      }
    } catch(Exception e) {
      // discard
    }
    
    String commandString = getCommand(_query);    
    String queryString = getQuery(_query);

    if(queryString != null && !queryString.isEmpty()) {
      _searchResults = _searchService.getSearchResults(queryString, _resultFactory);
    }
          
    while(true) {
      if(_searchResults.getMatches().size() > 0) {
        // route identifier search
        if(_searchResults.getMatches().size() == 1 && _searchResults.getResultType().equals("RouteResult")) {
          RouteResult route = (RouteResult)_searchResults.getMatches().get(0);

          if(commandString != null && commandString.equals("C")) {
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

            _searchResults = newResults;
            continue;
          
          // route schedule status
          } else {
            _response = routeResponse(route);
            break;
          }
          
        // paginated service alerts
        } else if(_searchResults.getResultType().equals("ServiceAlertResult")) {
          if(commandString != null && commandString.equals("M")) {
            _response = serviceAlertResponse(_searchResultsCursor);
          } else {
            _response = serviceAlertResponse(0);
          }
          break;
          
        // one or more paginated stops
        } else if(_searchResults.getResultType().equals("StopResult")) {
          if(_searchResults.getMatches().size() > 1) {
            if(commandString != null && commandString.equals("M")) {
              _response = multipleStopResponse(_searchResultsCursor);
            } else {
              _response = multipleStopResponse(0);
            }
          } else {
            _response = singleStopResponse();              
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
    footer += "M for more\n";

    String body = ""; 
    int i = offset;
    while(i < _searchResults.getMatches().size()) {
      ServiceAlertResult serviceAlert = (ServiceAlertResult)_searchResults.getMatches().get(i);
      String textToAdd = serviceAlert.getAlert() + "\n\n";      

      // if the alert alone is too long, we have to chop it
      if(textToAdd.length() > MAX_SMS_CHARACTER_COUNT - footer.length() - 5) {
        textToAdd = textToAdd.substring(0, MAX_SMS_CHARACTER_COUNT - footer.length() - 5) + "...\n\n";
      }
      
      if(body.length() + footer.length() + textToAdd.length() < MAX_SMS_CHARACTER_COUNT) {
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
    String header = result.getShortName() + "\n";

    String footer = "\nSend:\n";
    footer += "STOP-ID or INTERSECTION\n";      
    footer += "Add '" + result.getShortName() + "' for best results\n";
    
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
        
        if(headsign.length() > routeDirectionTruncationLength) {
          body += "to " + headsign.substring(0, Math.min(routeDirectionTruncationLength, headsign.length())) + "...";
        } else {
          body += "to " + headsign + " ";
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
    
  private String multipleStopResponse(int offset) throws Exception {
    if(offset >= _searchResults.getMatches().size()) {
      return errorResponse("No more.");
    }

    // a placeholder header used for worst-case length calculations--updated
    // with real values at the end once we know how many things actually fit 
    // with realtime data
    String header = "XX-XX of XX stops\n";
    
    String footer = "\nSend:\n";
    footer += "STOP-ID+ROUTE for bus info\n";

    String alertsFooter = footer + "C+ROUTE for alerts (*)\n";

    // the worst case in terms of length for footer
    String moreFooter = alertsFooter + "M for more\n";

    String body = ""; 
    int i = offset;
    int stopsInThisPage = 0;
    while(i < _searchResults.getMatches().size()) {
      StopResult stopResult = (StopResult)_searchResults.getMatches().get(i);

      // stop header
      String fixedPartToAdd = stopResult.getStopDirection() + "-bound: " + stopResult.getIdWithoutAgency() + "\n";

      // body content for stop
      String realtimePartToAdd = ""; // can fit each route in a category, or show an observation
      String routesOnlyPartToAdd = ""; // the above isn't true. 
      
      if(stopResult.getRoutesAvailable().size() == 0) {
        if(_searchResults.getRouteIdFilter().size() > 0) {
          fixedPartToAdd += "No filter matches\n";
        } else {
          fixedPartToAdd += "No routes\n";
        }
      } else {
        Set<String> notScheduledRoutes = new HashSet<String>();
        Set<String> notEnRouteRoutes = new HashSet<String>();

        Set<String> routeList = new HashSet<String>();

        for(RouteAtStop routeHere : stopResult.getRoutesAvailable()) {
          for(RouteDirection direction : routeHere.getDirections()) {
            String prefix = "";
            if(!direction.getSerivceAlerts().isEmpty()) {
              footer = alertsFooter;
              prefix += "*";
            }            
            prefix += routeHere.getShortName();

            routeList.add(prefix);
            
            if(!direction.hasUpcomingScheduledService()) {
              notScheduledRoutes.add(prefix);
            } else {
              if(direction.getDistanceAways().size() > 0) {
                realtimePartToAdd += prefix + " " + direction.getDistanceAways().get(0) + "\n";
              } else {
                notEnRouteRoutes.add(prefix);
              }
            }
          } // for direction
        } // for routes here 

        if(notEnRouteRoutes.size() > 0) {
          realtimePartToAdd += StringUtils.join(notEnRouteRoutes, ",") + " not en-route\n";
        }
        
        if(notScheduledRoutes.size() > 0) {
          realtimePartToAdd += StringUtils.join(notScheduledRoutes, ",") + " not sched.\n";
        }

        routesOnlyPartToAdd += StringUtils.join(routeList, ",") + "\n";
      } // if routes available
      
      int remainingSpace = MAX_SMS_CHARACTER_COUNT - header.length() - body.length() - moreFooter.length() - fixedPartToAdd.length();

      // we can fit realtime for this stop
      if(realtimePartToAdd.length() + 1 < remainingSpace) {
        body += fixedPartToAdd + realtimePartToAdd;
        
      // we can fit only routes for this stop
      } else if(routesOnlyPartToAdd.length() + 1 < remainingSpace) {
        body += fixedPartToAdd + routesOnlyPartToAdd;

      // can't fit the "fallback case"--a stop with so many routes, it doesn't fit in one page, either.
      } else if(stopsInThisPage == 0 && routesOnlyPartToAdd.length() + 1 > remainingSpace) {
        body += fixedPartToAdd + "(too many routes to show)\n";

      // out of space in this message, break to next page    
      } else {
        break;
      }
      
      stopsInThisPage++;
      i++;
    }
    _searchResultsCursor = i; 

    // construct real header now that we know how many items fit on our page:
    int totalItems = _searchResults.getMatches().size();
    int start = offset + 1;
    int end = i;
            
    if(start == end) {
      header = start + "";
    } else {
      header = start + "-" + end;
    }

    header += " of " + totalItems + " stops\n";

    if(_googleAnalytics != null) {
      try {
        _googleAnalytics.trackEvent("SMS", "Stop Realtime Response For Single Stop", _query);
      } catch(Exception e) {
        //discard
      }
    }
    
    if(i < _searchResults.getMatches().size()) {
      return header + body + moreFooter;
    } else {
      return header + body + footer;
    }
  }
      
  private String singleStopResponse() throws Exception {
    StopResult stopResult = (StopResult)_searchResults.getMatches().get(0);

    String header = stopResult.getIdWithoutAgency() + "\n";

    String footer = "\nSend:\n";
    footer += stopResult.getIdWithoutAgency() + "+ROUTE for bus info\n";

    // worst case for footer length
    String alertsFooter = footer + "C+ROUTE for alerts (*)\n";

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
      
      for(RouteAtStop routeHere : stopResult.getRoutesAvailable()) {
        for(RouteDirection direction : routeHere.getDirections()) {
          String prefix = "";
          if(!direction.getSerivceAlerts().isEmpty()) {
            footer = alertsFooter;
            prefix += "*";
          }            
          prefix += routeHere.getShortName();

          HashMap<Double, String> sortableDistanceAways = direction.getDistanceAwaysWithSortKey();
          for(Double distanceAway : sortableDistanceAways.keySet()) {
            String distanceAwayString = sortableDistanceAways.get(distanceAway);
            
            observationsByDistanceFromStopAcrossAllRoutes.put(distanceAway, prefix + ": " + distanceAwayString);
          }
        }
      }

      // if there are no upcoming buses, provide info about the routes that the user gave a filter for, if any
      if(observationsByDistanceFromStopAcrossAllRoutes.isEmpty()) {
        if(_searchResults.getRouteIdFilter().isEmpty()) {
          body += "No upcoming arrivals.\n";
        } else {
          // respond specifically to any route that the user added as a filter to this stop
          for(RouteAtStop routeHere : stopResult.getRoutesAvailable()) {
            for(RouteDirection direction : routeHere.getDirections()) {
              if(direction.hasUpcomingScheduledService()) {
                body += routeHere.getShortName() + ": not en-route\n"; 
              } else {
                body += routeHere.getShortName() + ": not scheduled\n"; 
              }
            }
          }
        }        
        
      // as many observations as will fit, sorted by soonest to arrive out
      } else {      
        for(String observationString : observationsByDistanceFromStopAcrossAllRoutes.values()) {
          String textToAdd = observationString + "\n";      
  
          if(body.length() + header.length() + alertsFooter.length() + textToAdd.length() < MAX_SMS_CHARACTER_COUNT) {
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
    
    return header + body + footer;
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
    
    return header + body + footer;
  }
  
  private String errorResponse(String message) throws Exception {
    String staticStuff = "Send:\n\n";
    
    staticStuff += "STOP-ID or\n";
    staticStuff += "INTERSECTION\n\n";
    
    staticStuff += "Add ROUTE for best results:\n";
    staticStuff += "S74 MAIN AND CRAIG\n";
    staticStuff += "200884 S44\n\n";
    
    staticStuff += "Find your 6-digit stop ID on bus stop pole";

    if(message != null) {
      if(staticStuff.length() + 1 + message.length() > MAX_SMS_CHARACTER_COUNT) {
        throw new Exception("Error message text is too long.");
      }

      return message + " " + staticStuff;
    } else
      return staticStuff;
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
    
    if(query.toUpperCase().equals("R")) {
      return "R";
    }

    if(query.toUpperCase().equals("M")) {
      return "M";
    }

    if(query.toUpperCase().startsWith("C ") || query.toUpperCase().startsWith("C+")) {        
      return "C";
    }

    // try as ambiguous location result choice:
    try {
      // no results means this can't be a pick from ambiguous location results
      if(_searchResults == null || _searchResults.isEmpty()) {
        return null;
      }
      
      // a list of things other than location results can't be a pick from ambiguous locations.
      if(!(_searchResults.getResultType().equals("GeocodeResult"))) {
        return null;
      }
      
      // is the choice within the bounds of the number of things we have?
      Integer choice = Integer.parseInt(query);
      if(choice >= 1 && choice <= _searchResults.getSuggestions().size()) {
        return choice.toString();
      }
    } catch (NumberFormatException e) {
      return null;
    }

    return null;
  }
  
  /**
   * METHODS FOR VIEWS
   */
  public String getResponse() {
    syncSession();
    
    return _response.trim();
  }
}
