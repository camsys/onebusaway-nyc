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

import org.onebusaway.nyc.geocoder.model.NycGeocoderResult;
import org.onebusaway.nyc.geocoder.service.NycGeocoderService;
import org.onebusaway.nyc.presentation.impl.realtime.SiriDistanceExtension;
import org.onebusaway.nyc.presentation.impl.realtime.SiriExtensionWrapper;
import org.onebusaway.nyc.presentation.impl.sort.SearchResultComparator;
import org.onebusaway.nyc.presentation.model.search.LocationResult;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.model.search.RouteResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.presentation.service.search.SearchResult;
import org.onebusaway.nyc.presentation.service.search.StopSearchService;
import org.onebusaway.nyc.sms.actions.model.SmsRouteDestinationItem;
import org.onebusaway.nyc.sms.actions.model.SmsSearchModelFactory;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import org.apache.commons.lang.xwork.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri.MonitoredCallStructure;
import uk.org.siri.siri.MonitoredStopVisitStructure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IndexAction extends SessionedIndexAction {
  
  private static final long serialVersionUID = 1L;

  private static final int MAX_SMS_CHARACTER_COUNT = 160;
  
  private String _response = null;

  @Autowired
  private RealtimeService _realtimeService;

  @Autowired
  private StopSearchService _stopSearchService;

  @Autowired
  private NycGeocoderService _geocoderService;

  /**
   * Returns the canonical string for a command, or null if not a command at all.
   * @param query: user query.
   * @return null if not a command, canonical command string if a command.
   */
  public String normalizeCommand(String query) {
    if(query.toUpperCase().startsWith("A ") || query.toUpperCase().startsWith("ALERT "))
      return "A";

    if(query.toUpperCase().equals("R") || query.toUpperCase().equals("REFRESH"))
      return "R";

    if(query.toUpperCase().equals("M") || query.toUpperCase().equals("MORE"))
      return "M";
    
    try {
      Integer choice = Integer.parseInt(query);

      if(choice >= 1 && choice <= 10) 
        return choice.toString();
    } catch (Exception e) {
      // (fall through)
    }

    return null;
  }
  
  public String execute() throws Exception {
    SmsSearchModelFactory factory = new SmsSearchModelFactory();
    _stopSearchService.setModelFactory(factory);

    // if query is a command, process it using the last result context. 
    // otherwise, try the input as a query.
    String commandString = normalizeCommand(_query);
    if(commandString != null) {
      // ALERT
      if(commandString.equals("A")) {
        String[] tokens = _query.split(" ");
        if(tokens.length == 2) {
          serviceAlertResponse(tokens[1]);
          return SUCCESS;
        }
        
      // REFRESH
      } else if(commandString.equals("R")) {
        _query = _lastQuery;
        generateNewSearchResults(_query);
        
      // MORE
      } else if(commandString.equals("M")) {
        // (fall through)

      // AMBIGUOUS LOCATION CHOICE
      } else {
        try {
          Integer choice = Integer.parseInt(commandString);
          LocationResult theResult = (LocationResult)_searchResults.get(choice - 1);

          _query = theResult.getFormattedAddress();
          generateNewSearchResults(_query);
        } catch(Exception e) {
          // (fall through)
        }
      }
    } else
      generateNewSearchResults(_query);

    // display results to user
    if(_searchResults.size() == 0) {
      errorResponse("No matches.");
      
    } else {
      String resultType = _searchResults.get(0).getType();
      
      // STOP RESULT
      if(resultType.equals("stopResult")) {

        // try to fill the most space in the SMS with realtime data; if we can't even get
        // one observation for each route available in there, then punt to disambiguation.
        boolean truncated = true;
        int i = 1;
        while(truncated) {
          truncated = realtimeStopResponse(_searchResults, i);

          if(_response.length() > MAX_SMS_CHARACTER_COUNT) {
            if(i == 1) {
              _response = null;
            } else {
              truncated = realtimeStopResponse(_searchResults, i - 1);
            }
            break;
          }
          
          i++;
        }

        // okay, so we have to disambiguate--now find the maximum page size we can fit into 
        // one SMS!
        if(_response == null) {
          truncated = true;
          i = 0;
          while(truncated) {
            truncated = stopDisambiguationResponse(_searchResults, _searchResultsCursor, i);

            if(_response.length() > MAX_SMS_CHARACTER_COUNT) {
              if(i == 1) {
                _response = null;
              } else {
                truncated = stopDisambiguationResponse(_searchResults, _searchResultsCursor, i - 1);
                _searchResultsCursor += (i - 1);
              }
              break;
            }
            
            i++;
          }
        }
        
        // shouldn't happen!
        if(_response == null)
          throw new Exception("No combination of values produced a message that could fit into 1 SMS!");

      // LOCATION RESULT
      } else if(resultType.equals("locationResult")) {
        locationDisambiguationResponse(_searchResults);
        
        if(_response.length() > MAX_SMS_CHARACTER_COUNT) {
          errorResponse("Not specific enough.");
        }
      }
    }
    
    return SUCCESS;
  }
  
  private void generateNewSearchResults(String q) {
    _searchResults.clear();
    _searchResultsCursor = 0;

    if(q == null || q.isEmpty())
      return;

    // try as stop ID
    _searchResults.addAll(_stopSearchService.resultsForQuery(q));

    // nothing? geocode it!
    if(_searchResults.size() == 0) {
      _searchResults.addAll(generateResultsFromGeocode(q));        
    }
    
    Collections.sort(_searchResults, new SearchResultComparator());

    injectRealtimeDataIntoResults(_searchResults);
  }
    
  
  private List<SearchResult> generateResultsFromGeocode(String q) {
    List<SearchResult> results = new ArrayList<SearchResult>();

    List<NycGeocoderResult> geocoderResults = _geocoderService.nycGeocode(q);
    
    if(geocoderResults.size() == 1) {
      NycGeocoderResult result = geocoderResults.get(0);

      if(!result.isRegion()) {
        results.addAll(_stopSearchService.resultsForLocation(result.getLatitude(), result.getLongitude()));
      }
    } else {
      for(NycGeocoderResult result : geocoderResults) {
        if(!result.isRegion()) {
          LocationResult locationSearchResult = new LocationResult();
          locationSearchResult.setGeocoderResult(result);

          results.add(locationSearchResult);
        }
      }
    }

    return results;
  } 

  private void serviceAlertResponse(String route) throws Exception {
    List<NaturalLanguageStringBean> alerts = new ArrayList<NaturalLanguageStringBean>();
    
    for(SearchResult _result : _searchResults) {
      if(! (_result instanceof StopResult))
        continue;

      StopResult result = (StopResult)_result;
      
      for(RouteResult routeAvailable : result.getRoutesAvailable()) {
        if(!routeAvailable.getRouteIdWithoutAgency().equals(route))
          continue;
        
        for(RouteDestinationItem _destination : routeAvailable.getDestinations()) {
          SmsRouteDestinationItem destination = (SmsRouteDestinationItem)_destination;
          alerts.addAll(destination.getServiceAlerts());
        }
      }
    }
    
    if(alerts.size() == 0) 
      errorResponse("No alerts found.");
    else
      _response = StringUtils.join(alerts, "\n\n");
  }
  
  private void locationDisambiguationResponse(List<SearchResult> results) {
    _response = "Did you mean:\n\n";
    
    int i = 1;
    for(SearchResult _result : results) {
      LocationResult result = (LocationResult)_result;
      
      _response += i + ") " + result.getFormattedAddress() + "\n";
      
      if(results.size() <= 2) {
        _response += "(" + result.getNeighborhood() + ")";
        _response += "\n";
      }
      
      i++;
    }

    _response += "\nReply: 1-" + (i - 1);
  }
  
  private boolean realtimeStopResponse(List<SearchResult> results, int realtimeObservationCount) {
    boolean truncated = false;

    _response = "";

    if(results.size() > 1)
      _response += results.size() + " stop" + ((results.size() != 1) ? "s" : "") + " here.\n\n";
    
    List<String> routesWithAlerts = new ArrayList<String>();
    for(SearchResult _result : results) {
      StopResult result = (StopResult)_result;

      _response += result.getStopIdWithoutAgency() + " (" + result.getStopDirection() + "-bound)\n";

      for(RouteResult routeHere : result.getRoutesAvailable()) {
        for(RouteDestinationItem _destination : routeHere.getDestinations()) {
          SmsRouteDestinationItem destination = (SmsRouteDestinationItem)_destination;
          
          String prefix = "";
          if(destination.getServiceAlerts().size() > 0) {
            prefix += "*";
            routesWithAlerts.add(routeHere.getRouteIdWithoutAgency());
          }            

          prefix += routeHere.getRouteIdWithoutAgency() + ":";
          
          if(destination.getDistanceAwayStrings().size() > 0) {
            int c = 0;
            for(String distanceAway : destination.getDistanceAwayStrings()) {
              if(c >= realtimeObservationCount) {
                truncated = true;
                break;
              }

              _response += prefix + distanceAway + "\n";

              c++;
            }
          } else {
            _response += "No bus en-route.\n";
          }
          
          _response += "\n";
        }
      }
    }

    if(routesWithAlerts.size() > 0) {
      _response += "* Alert for " + StringUtils.join(routesWithAlerts, ",") + "\n";
      _response += "Reply: A)lert +route R)efresh";
    } else {
      _response += "Reply: R)efresh";
    }
    
    return truncated;
  }
    
  private boolean stopDisambiguationResponse(List<SearchResult> results, int offset, int count) {
    boolean truncated = false;
    
    _response = results.size() + " stop" + ((results.size() != 1) ? "s" : "") + " here.\n\n";

    int c = 0;
    for(SearchResult _result : results) {
      if(c >= count) {
        truncated = true;
        break;
      }
      
      StopResult result = (StopResult)_result;

      // header of item
      _response += result.getStopIdWithoutAgency() + " (" + result.getStopDirection() + "-bound)\n";

      // routes available at stop
      List<String> routesHere = new ArrayList<String>();
      for(RouteResult routeAvailable : result.getRoutesAvailable()) {
        routesHere.add(routeAvailable.getRouteIdWithoutAgency());
      }
      
      _response += StringUtils.join(routesHere, ",") + "\n";

      c++;
    }
    
    _response += "\nReply: stop ID +route";
    
    if(offset + count < results.size()) {
      _response += "\nOR M)ore";
    }
    
    return truncated;
  }
  
  private void errorResponse(String message) throws Exception {
    String staticStuff = "Reply with:\n\n";
    
    staticStuff += "stop ID (+route)\n";
    staticStuff += "or\n";
    staticStuff += "route+intersection:\n";
    staticStuff += "'B63 Atlantic & 4 Av'\n\n";
    
    staticStuff += "For alerts reply with A +route";

    if(message != null) {
      if(staticStuff.length() + 1 + message.length() > MAX_SMS_CHARACTER_COUNT) {
        throw new Exception("Error message text is too long.");
      }

      _response = message + " " + staticStuff;
    } else
      _response = staticStuff;
  }

  private void injectRealtimeDataIntoResults(List<SearchResult> searchResults) {
    for(SearchResult searchResult : searchResults) {
      if(searchResult instanceof StopResult)
        injectRealtimeDataIntoModel((StopResult)searchResult);
    }
  }
  
  private StopResult injectRealtimeDataIntoModel(StopResult stopSearchResult) {
    for(RouteResult route : stopSearchResult.getRoutesAvailable()) {
      for(RouteDestinationItem _destination : route.getDestinations()) {
        SmsRouteDestinationItem destination = (SmsRouteDestinationItem)_destination;

        // service alerts
        List<NaturalLanguageStringBean> serviceAlerts = _realtimeService.getServiceAlertsForStop(stopSearchResult.getStopId());
        destination.setServiceAlerts(serviceAlerts);

        // stop visits
        List<MonitoredStopVisitStructure> visits = _realtimeService.getMonitoredStopVisitsForStop(stopSearchResult.getStopId(), false);

        List<String> distanceAwayStrings = new ArrayList<String>();
        for(MonitoredStopVisitStructure visit : visits) {
          String routeId = visit.getMonitoredVehicleJourney().getLineRef().getValue();
          String directionId = visit.getMonitoredVehicleJourney().getDirectionRef().getValue();
          if(!route.getRouteId().equals(routeId) || !destination.getDirectionId().equals(directionId))
            continue;

          MonitoredCallStructure monitoredCall = visit.getMonitoredVehicleJourney().getMonitoredCall();
          if(monitoredCall == null) 
            continue;

          SiriExtensionWrapper wrapper = (SiriExtensionWrapper)monitoredCall.getExtensions().getAny();
          SiriDistanceExtension distanceExtension = wrapper.getDistances();
          distanceAwayStrings.add(distanceExtension.getPresentableDistance());
        }

        destination.setDistanceAwayStrings(distanceAwayStrings);
      }
    }    

    return stopSearchResult;
  }
  
  public String getResponse() {
    syncSession();
    
    return _response;
  }
}
