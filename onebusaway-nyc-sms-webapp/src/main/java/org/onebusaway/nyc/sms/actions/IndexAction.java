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

import java.util.HashSet;
import java.util.Set;

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
        if(_searchResults.getMatches().size() == 1 && _searchResults.getResultType().equals("RouteResult")) {
          RouteResult route = (RouteResult)_searchResults.getMatches().get(0);

          // service alerts for the route
          if(commandString != null && commandString.equals("C")) {
            // find a unique set of service alerts
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
          if(commandString != null && commandString.equals("M")) {
            _response = stopResponse(_searchResultsCursor);
          } else {
            _response = stopResponse(0);
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
        // ambiguous addresses. if they send "M" for more, we start displaying where we left
        // off, otherwise start at the beginning. we also recognize a numeric input that
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
        _googleAnalytics.trackEvent("SMS", "No Results", _query);
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
    footer += "'M' for more\n";

    String body = ""; 
    int i = offset;
    while(i < _searchResults.getMatches().size()) {
      ServiceAlertResult serviceAlert = (ServiceAlertResult)_searchResults.getMatches().get(i);
      String textToAdd = serviceAlert.getAlert() + "\n\n";      

      // if the alert alone is too long, we have to chop it
      if(textToAdd.length() > MAX_SMS_CHARACTER_COUNT - footer.length() - 3) {
        textToAdd = textToAdd.substring(0, MAX_SMS_CHARACTER_COUNT - footer.length() - 3) + "...";
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
      _googleAnalytics.trackEvent("SMS", "Service Alert", _query + " [" + _searchResults.getMatches().size() + "]");
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
    footer += "STOP CODE or INTERSECTION\n";      
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
          body += "to " + headsign.substring(0, Math.min(routeDirectionTruncationLength, headsign.length())) + "... ";
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
      _googleAnalytics.trackEvent("SMS", "Route Response", _query);
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
    String moreFooter = footer + "'M' for more\n";
    
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
      _googleAnalytics.trackEvent("SMS", "Location Disambiguation", _query + " [" + _searchResults.getSuggestions().size() + "]");
    }
    
    if(i < _searchResults.getSuggestions().size()) {
      return header + body + moreFooter;
    } else {
      return header + body + footer;
    }
  }
    
  private String stopResponse(int offset) throws Exception {
    if(offset >= _searchResults.getMatches().size()) {
      return errorResponse("No more.");
    }

    String header = "";
    if(_searchResults.getMatches().size() > 1) {
      header += _searchResults.getMatches().size() + " stop" + ((_searchResults.getMatches().size() != 1) ? "s" : "") + " here\n\n";
    }
    
    String footer = "\nSend:\n";
    footer += "STOP CODE + ROUTE for realtime info\n";

    String alertsFooter = footer + "'C' + ROUTE for service change (*)\n";

    // the worst case in terms of length for footer
    String moreFooter = alertsFooter + "'M' for more\n";

    String body = ""; 
    int i = offset;
    while(i < _searchResults.getMatches().size()) {
      StopResult stopResult = (StopResult)_searchResults.getMatches().get(i);

      String fixedPartToAdd = "";
      if(_searchResults.getMatches().size() > 1) {
        fixedPartToAdd += stopResult.getStopDirection() + "-bound: " + stopResult.getIdWithoutAgency() + "\n";
      } else {
        fixedPartToAdd += stopResult.getIdWithoutAgency() + "\n";
      }

      // with realtime info
      String realtimePartToAdd = "";
      // without realtime info
      String routesOnlyPartToAdd = "";

      if(stopResult.getRoutesAvailable().size() == 0) {
        if(_searchResults.getRouteIdFilter().size() > 0) {
          fixedPartToAdd += "No filter matches.\n";
        } else {
          fixedPartToAdd += "No routes.\n";
        }
      } else {
        for(RouteAtStop routeHere : stopResult.getRoutesAvailable()) {
          for(RouteDirection direction : routeHere.getDirections()) {
            String prefix = "";
            if(!direction.getSerivceAlerts().isEmpty()) {
              footer = alertsFooter;
              prefix += "*";
            }            
            prefix += routeHere.getShortName();

            if(!direction.hasUpcomingScheduledService()) {
              realtimePartToAdd += prefix + " not sched.";                
            } else {
              if(direction.getDistanceAways().size() > 0) {
                realtimePartToAdd += prefix + " " + direction.getDistanceAways().get(0);
              } else {
                realtimePartToAdd += prefix + " not en-route";
              }
            }
            
            realtimePartToAdd += "\n";
          }

          routesOnlyPartToAdd += routeHere.getShortName() + " ";
        } // for routes here realtime
      } // if routes available

      int remainingSpace = MAX_SMS_CHARACTER_COUNT - header.length() - body.length() - moreFooter.length() - fixedPartToAdd.length();

      // we can fit realtime for this stop
      if(realtimePartToAdd.length() < remainingSpace) {
        body += fixedPartToAdd + realtimePartToAdd;

      // we can fit only routes for this stop
      } else if(routesOnlyPartToAdd.length() + 1 < remainingSpace) {
        body += fixedPartToAdd + routesOnlyPartToAdd + "\n";

      // out of space!      
      } else {
        break;
      }
      
      i++;
    }
    _searchResultsCursor = i; 

    if(_googleAnalytics != null) {
      _googleAnalytics.trackEvent("SMS", "Stop Realtime Response", _query);
    }
    
    if(i < _searchResults.getMatches().size()) {
      return header + body + moreFooter;
    } else {
      return header + body + footer;
    }
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
    String staticStuff = "Text your:\n\n";
    
    staticStuff += "STOP CODE or\n";
    staticStuff += "INTERSECTION\n\n";
    
    staticStuff += "Add ROUTE for best results:\n";
    staticStuff += "'S74 MAIN AND CRAIG'\n";
    staticStuff += "'200884 S44'\n\n";
    
    staticStuff += "Find 6-digit stop code on bus stop pole.";

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

    if(query.toUpperCase().startsWith("C ")) {        
      return query.substring(2);
    }

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

    if(query.toUpperCase().startsWith("C ")) {
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
    
    return _response;
  }
}
