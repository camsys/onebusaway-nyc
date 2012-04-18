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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.impl.service_alerts.ServiceAlertsHelper;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.utility.DateLibrary;

import org.apache.struts2.interceptor.ServletRequestAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.StopMonitoringDeliveryStructure;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class StopMonitoringAction extends OneBusAwayNYCActionSupport 
  implements ServletRequestAware {

  private static final long serialVersionUID = 1L;

  private static Logger _log = LoggerFactory.getLogger(StopMonitoringAction.class);

  @Autowired
  public NycTransitDataService _nycTransitDataService;

  @Autowired  
  private RealtimeService _realtimeService;

  private Siri _response;
  
  private ServiceAlertsHelper _serviceAlertsHelper = new ServiceAlertsHelper();

  private HttpServletRequest _request;
  
  private String _type = "xml";

  private Date _now = null;
  
  public void setTime(String time) throws Exception {
    Date timeAsDate = DateLibrary.getIso8601StringAsTime(time);    
    
    _now = timeAsDate;
  }

  public Date getTime() {
    if(_now != null)
      return _now;
    else
      return new Date();
  }
  
  public void setType(String type) {
    _type = type;
  }
  
  @Override
  public String execute() { 
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

    List<MonitoredStopVisitStructure> visits = new ArrayList<MonitoredStopVisitStructure>();

    if(stopId != null && stopId.hasValues()) {
       visits =
          _realtimeService.getMonitoredStopVisitsForStop(stopId.toString(), maximumOnwardCalls);

      if((routeId != null && routeId.hasValues()) || directionId != null) {
        List<MonitoredStopVisitStructure> filteredVisits = new ArrayList<MonitoredStopVisitStructure>();

        for(MonitoredStopVisitStructure visit : visits) {
          MonitoredVehicleJourneyStructure journey = visit.getMonitoredVehicleJourney();

          AgencyAndId thisRouteId = AgencyAndIdLibrary.convertFromString(journey.getLineRef().getValue());
          String thisDirectionId = journey.getDirectionRef().getValue();
          
          // user filtering
          if((routeId != null && routeId.hasValues()) && !thisRouteId.equals(routeId))
            continue;
          
          if(directionId != null && !thisDirectionId.equals(directionId))
            continue;
          
          filteredVisits.add(visit);
        }

        visits = filteredVisits;
      }
    }

    _response = generateSiriResponse(visits, stopId);

    return SUCCESS;
  }
  

  private Siri generateSiriResponse(List<MonitoredStopVisitStructure> visits, AgencyAndId stopId) {
    ServiceDelivery serviceDelivery = new ServiceDelivery();
    try {
      StopMonitoringDeliveryStructure stopMonitoringDelivery = new StopMonitoringDeliveryStructure();
      stopMonitoringDelivery.setResponseTimestamp(getTime());
      
      Calendar gregorianCalendar = new GregorianCalendar();
      gregorianCalendar.setTime(getTime());
      gregorianCalendar.add(Calendar.MINUTE, 1);
      stopMonitoringDelivery.setValidUntil(gregorianCalendar.getTime());

      stopMonitoringDelivery.getMonitoredStopVisit().addAll(visits);

      serviceDelivery.setResponseTimestamp(getTime());
      serviceDelivery.getStopMonitoringDelivery().add(stopMonitoringDelivery);

      _serviceAlertsHelper.addSituationExchangeToSiriForStops(serviceDelivery, visits, _nycTransitDataService, stopId);
    } catch (RuntimeException e) {
      _log.error("Exception in generateSirirResponse", e);
      throw e;
    }

    Siri siri = new Siri();
    siri.setServiceDelivery(serviceDelivery);
    
    return siri;
  }

  public String getStopMonitoring() {
    try {
      if(_type.equals("xml"))
        return _realtimeService.getSiriXmlSerializer().getXml(_response);
      else
        return _realtimeService.getSiriJsonSerializer().getJson(_response, _request.getParameter("callback"));
    } catch(Exception e) {
      return e.getMessage();
    }
  }

  @Override
  public void setServletRequest(HttpServletRequest request) {
    this._request = request;
  }
  
}
