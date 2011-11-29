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

import org.onebusaway.nyc.presentation.impl.service_alerts.ServiceAlertsHelper;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriJsonSerializer;
import org.onebusaway.nyc.transit_data_federation.siri.SiriXmlSerializer;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.services.TransitDataService;

import org.apache.struts2.interceptor.ServletRequestAware;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.StopMonitoringDeliveryStructure;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

public class StopMonitoringAction extends OneBusAwayNYCActionSupport 
  implements ServletRequestAware {

  private static final long serialVersionUID = 1L;

  @Autowired
  public TransitDataService _transitDataService;

  @Autowired  
  private RealtimeService _realtimeService;

  private Siri _response;
  
  private ServiceAlertsHelper _serviceAlertsHelper = new ServiceAlertsHelper();

  private HttpServletRequest _request;
  
  private Date _now = new Date();

  private String _type = "json";

  public void setType(String type) {
    _type = type;
  }
  
  @Override
  public String execute() {  
    String stopId = _request.getParameter("MonitoringRef");
    String agencyId = _request.getParameter("OperatorRef");

    String routeId = _request.getParameter("LineRef");
    String directionId = _request.getParameter("DirectionRef");

    String detailLevel = _request.getParameter("StopMonitoringDetailLevel");
    boolean includeOnwardCalls = false;
    if (detailLevel != null) {
      includeOnwardCalls = detailLevel.equals("calls");
    }

    if(stopId != null && agencyId != null) {
      String stopIdWithAgency = agencyId + "_" + stopId;
      
      List<MonitoredStopVisitStructure> visits = 
          _realtimeService.getMonitoredStopVisitsForStop(stopIdWithAgency, includeOnwardCalls);

      if(routeId != null || directionId != null) {
        for(MonitoredStopVisitStructure visit : visits) {
          MonitoredVehicleJourneyStructure journey = visit.getMonitoredVehicleJourney();
          String thisRouteId = journey.getLineRef().getValue();
          String thisDirectionId = journey.getDirectionRef().getValue();
          
          // user filtering
          if(routeId != null && !thisRouteId.equals(routeId))
            visits.remove(visit);
          
          if(directionId != null && !thisDirectionId.equals(directionId))
            visits.remove(visit);
        }
      }
      
      _response = generateSiriResponse(visits);
    }
    
    return SUCCESS;
  }

  private Siri generateSiriResponse(List<MonitoredStopVisitStructure> visits) {
    
    StopMonitoringDeliveryStructure stopMonitoringDelivery = new StopMonitoringDeliveryStructure();
    stopMonitoringDelivery.setResponseTimestamp(_now);
    
    Calendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTime(_now);
    gregorianCalendar.add(Calendar.MINUTE, 1);
    stopMonitoringDelivery.setValidUntil(gregorianCalendar.getTime());
    
    stopMonitoringDelivery.getMonitoredStopVisit().addAll(visits);

    ServiceDelivery serviceDelivery = new ServiceDelivery();
    serviceDelivery.setResponseTimestamp(_now);
    serviceDelivery.getStopMonitoringDelivery().add(stopMonitoringDelivery);

    _serviceAlertsHelper.addSituationExchangeToSiriForStops(serviceDelivery, visits, _transitDataService);

    Siri siri = new Siri();
    siri.setServiceDelivery(serviceDelivery);
    
    return siri;
  }

  public String getStopMonitoring() {
    try {
      if(_type.equals("xml"))
        return SiriXmlSerializer.getXml(_response);
      else
        return SiriJsonSerializer.getJson(_response, _request.getParameter("callback"));
    } catch(Exception e) {
      return e.getMessage();
    }
  }

  @Override
  public void setServletRequest(HttpServletRequest request) {
    this._request = request;
  }
}
