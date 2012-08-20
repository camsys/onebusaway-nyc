/*
 * Copyright 2010, OpenPlans Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.webapp.actions.api.siri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.impl.service_alerts.ServiceAlertsHelper;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri.ErrorDescriptionStructure;
import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.OtherErrorStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.ServiceDeliveryErrorConditionStructure;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.StopMonitoringDeliveryStructure;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion;

public class StopMonitoringAction extends OneBusAwayNYCActionSupport 
  implements ServletRequestAware, ServletResponseAware {

  private static final long serialVersionUID = 1L;

  @Autowired
  public NycTransitDataService _nycTransitDataService;

  @Autowired  
  private RealtimeService _realtimeService;
  
  @Autowired
  private ConfigurationService _configurationService;

  private Siri _response;
  
  private ServiceAlertsHelper _serviceAlertsHelper = new ServiceAlertsHelper();

  private HttpServletRequest _request;
  
  private HttpServletResponse _servletResponse;
  
  private String _type = "xml";
  
  private JGoogleAnalyticsTracker _googleAnalytics = null;
  
  public void setType(String type) {
    _type = type;
  }
  
  @Override
  public String execute() {
    
    String googleAnalyticsSiteId = 
        _configurationService.getConfigurationValueAsString("display.googleAnalyticsSiteId", null);
    
    try {
      if(googleAnalyticsSiteId != null) {    
        AnalyticsConfigData config = new AnalyticsConfigData(googleAnalyticsSiteId, null);
        _googleAnalytics = new JGoogleAnalyticsTracker(config, GoogleAnalyticsVersion.V_4_7_2);
      }
    } catch(Exception e) {
      // discard
    }
    
    _realtimeService.setTime(getTime());

    String directionId = _request.getParameter("DirectionRef");
    
    AgencyAndId stopId = null;
    try {
      stopId = AgencyAndIdLibrary.convertFromString(_request.getParameter("MonitoringRef"));
    } catch (Exception e) {
      stopId = new AgencyAndId(_request.getParameter("OperatorRef"), _request.getParameter("MonitoringRef"));
    }
    
    AgencyAndId routeId = null;
    try {
      routeId = AgencyAndIdLibrary.convertFromString(_request.getParameter("LineRef"));
    } catch (Exception e) {
      routeId = new AgencyAndId(_request.getParameter("OperatorRef"), _request.getParameter("LineRef"));
    }
    
    String detailLevel = _request.getParameter("StopMonitoringDetailLevel");

    int maximumOnwardCalls = 0;        
    if (detailLevel != null && detailLevel.equals("calls")) {
      maximumOnwardCalls = Integer.MAX_VALUE;

      try {
        maximumOnwardCalls = Integer.parseInt(_request.getParameter("MaximumNumberOfCallsOnwards"));
      } catch (NumberFormatException e) {
        maximumOnwardCalls = Integer.MAX_VALUE;
      }
    }

    int maximumStopVisits = Integer.MAX_VALUE;        
    try {
      maximumStopVisits = Integer.parseInt(_request.getParameter("MaximumStopVisits"));
    } catch (NumberFormatException e) {
      maximumStopVisits = Integer.MAX_VALUE;
    }

    Integer minimumStopVisitsPerLine = null;        
    try {
      minimumStopVisitsPerLine = Integer.parseInt(_request.getParameter("MinimumStopVisitsPerLine"));
    } catch (NumberFormatException e) {
      minimumStopVisitsPerLine = null;
    }
    
    if(_googleAnalytics != null && _request.getParameter("key") != null && !_request.getParameter("key").isEmpty()) {
      try {
      _googleAnalytics.trackEvent("API", "Stop Monitoring", stopId.toString());
      } catch(Exception e) {
        //discard
      }
    }

    List<MonitoredStopVisitStructure> visits = new ArrayList<MonitoredStopVisitStructure>();
    Exception error = null;

    try {
      if(stopId != null && stopId.hasValues()) {
        
        // If the user supplied a routeId, see if it's valid and throw an exception if it's not
        if(routeId != null && routeId.hasValues() && this._nycTransitDataService.getRouteForId(routeId.toString()) == null) {
          throw new Exception("No such route: " + routeId.toString());
        }
        
      	visits = _realtimeService.getMonitoredStopVisitsForStop(stopId.toString(), maximumOnwardCalls);

      	List<MonitoredStopVisitStructure> filteredVisits = new ArrayList<MonitoredStopVisitStructure>();

      	Map<AgencyAndId, Integer> visitCountByLine = new HashMap<AgencyAndId, Integer>();
      	int visitCount = 0;
      	
      	for(MonitoredStopVisitStructure visit : visits) {
      		MonitoredVehicleJourneyStructure journey = visit.getMonitoredVehicleJourney();

      		AgencyAndId thisRouteId = AgencyAndIdLibrary.convertFromString(journey.getLineRef().getValue());
      		String thisDirectionId = journey.getDirectionRef().getValue();

      		// user filtering
      		if((routeId != null && routeId.hasValues()) && !thisRouteId.equals(routeId))
      			continue;

      		if(directionId != null && !thisDirectionId.equals(directionId))
      			continue;

      		// visit count filters
      		Integer visitCountForThisLine = visitCountByLine.get(thisRouteId);
      		if(visitCountForThisLine == null) {
      			visitCountForThisLine = 0;
      		}

      		if(visitCount >= maximumStopVisits) {
        		if(minimumStopVisitsPerLine == null) {
        			break;
        		} else {
        			if(visitCountForThisLine >= minimumStopVisitsPerLine) {
        				continue;    		
        			}
        		}
      		}
      		
      		filteredVisits.add(visit);

      		visitCount++;    		
      		visitCountForThisLine++;
      		visitCountByLine.put(thisRouteId, visitCountForThisLine);
      	}
      	visits = filteredVisits;
      }
    } catch (Exception e) {
      error = e;
    }

    _response = generateSiriResponse(visits, stopId, error);
    
    try {
      this._servletResponse.getWriter().write(getStopMonitoring());
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }
  
  private Siri generateSiriResponse(List<MonitoredStopVisitStructure> visits, AgencyAndId stopId, Exception error) {
    
    StopMonitoringDeliveryStructure stopMonitoringDelivery = new StopMonitoringDeliveryStructure();
    stopMonitoringDelivery.setResponseTimestamp(new Date(getTime()));
    
    ServiceDelivery serviceDelivery = new ServiceDelivery();
    serviceDelivery.setResponseTimestamp(new Date(getTime()));
    serviceDelivery.getStopMonitoringDelivery().add(stopMonitoringDelivery);
    
    if (error != null) {
      ServiceDeliveryErrorConditionStructure errorConditionStructure = new ServiceDeliveryErrorConditionStructure();
      
      ErrorDescriptionStructure errorDescriptionStructure = new ErrorDescriptionStructure();
      errorDescriptionStructure.setValue(error.getMessage());
      
      OtherErrorStructure otherErrorStructure = new OtherErrorStructure();
      otherErrorStructure.setErrorText(error.getMessage());
      
      errorConditionStructure.setDescription(errorDescriptionStructure);
      errorConditionStructure.setOtherError(otherErrorStructure);
      
      stopMonitoringDelivery.setErrorCondition(errorConditionStructure);
    } else {
      Calendar gregorianCalendar = new GregorianCalendar();
      gregorianCalendar.setTimeInMillis(getTime());
      gregorianCalendar.add(Calendar.MINUTE, 1);
      stopMonitoringDelivery.setValidUntil(gregorianCalendar.getTime());

      stopMonitoringDelivery.getMonitoredStopVisit().addAll(visits);

      serviceDelivery.setResponseTimestamp(new Date(getTime()));
      
      _serviceAlertsHelper.addSituationExchangeToSiriForStops(serviceDelivery, visits, _nycTransitDataService, stopId);
      _serviceAlertsHelper.addGlobalServiceAlertsToServiceDelivery(serviceDelivery, _realtimeService);
    }

    Siri siri = new Siri();
    siri.setServiceDelivery(serviceDelivery);
    
    return siri;
  }

  public String getStopMonitoring() {
    try {
      if(_type.equals("xml")) {
        this._servletResponse.setContentType("application/xml");
        return _realtimeService.getSiriXmlSerializer().getXml(_response);
      } else {
        this._servletResponse.setContentType("application/json");
        return _realtimeService.getSiriJsonSerializer().getJson(_response, _request.getParameter("callback"));
      }
    } catch(Exception e) {
      return e.getMessage();
    }
  }

  @Override
  public void setServletRequest(HttpServletRequest request) {
    this._request = request;
  }

  @Override
  public void setServletResponse(HttpServletResponse servletResponse) {
    this._servletResponse = servletResponse;
  }
  
  public HttpServletResponse getServletResponse(){
    return _servletResponse;
  }
  
}
