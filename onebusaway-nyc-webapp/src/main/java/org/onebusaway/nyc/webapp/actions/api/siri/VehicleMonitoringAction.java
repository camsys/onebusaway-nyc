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
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.utility.DateLibrary;

import com.dmurph.tracking.AnalyticsConfigData;
import com.dmurph.tracking.JGoogleAnalyticsTracker;
import com.dmurph.tracking.JGoogleAnalyticsTracker.GoogleAnalyticsVersion;

import org.apache.struts2.interceptor.ServletRequestAware;
import org.apache.struts2.interceptor.ServletResponseAware;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri.ErrorDescriptionStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.OtherErrorStructure;
import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.ServiceDeliveryErrorConditionStructure;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleMonitoringDeliveryStructure;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class VehicleMonitoringAction extends OneBusAwayNYCActionSupport
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

  private Date _now = null;
  
  private JGoogleAnalyticsTracker _googleAnalytics = null;

  public void setTime(String time) throws Exception {
    Date timeAsDate = DateLibrary.getIso8601StringAsTime(time);

    _now = timeAsDate;
  }

  public Date getTime() {
    if (_now != null)
      return _now;
    else
      return new Date();
  }

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
    String agencyId = _request.getParameter("OperatorRef");

    AgencyAndId vehicleId = null;
    try {
      vehicleId = AgencyAndIdLibrary.convertFromString(_request.getParameter("VehicleRef"));
    } catch (Exception e) {
      vehicleId = new AgencyAndId(agencyId, _request.getParameter("VehicleRef"));
    }

    AgencyAndId routeId = null;
    try {
      routeId = AgencyAndIdLibrary.convertFromString(_request.getParameter("LineRef"));
    } catch (Exception e) {
      routeId = new AgencyAndId(agencyId, _request.getParameter("LineRef"));
    }
    
    Exception error = null;
    if(routeId != null && routeId.hasValues() && this._nycTransitDataService.getRouteForId(routeId.toString()) == null) {
      error = new Exception("No such route: " + routeId.toString());
    }

    String detailLevel = _request.getParameter("VehicleMonitoringDetailLevel");

    int maximumOnwardCalls = 0;
    if (detailLevel != null && detailLevel.equals("calls")) {
      maximumOnwardCalls = Integer.MAX_VALUE;

      try {
        maximumOnwardCalls = Integer.parseInt(_request.getParameter("MaximumNumberOfCallsOnwards"));
      } catch (NumberFormatException e) {
        maximumOnwardCalls = Integer.MAX_VALUE;
      }
    }
    
    String gaLabel = null;

    // *** CASE 1: by route
    if (routeId != null && routeId.hasValues()) {
      
      gaLabel = routeId.toString();
      
      List<VehicleActivityStructure> activities = _realtimeService.getVehicleActivityForRoute(
          routeId.toString(), directionId, maximumOnwardCalls);

      if (vehicleId != null && vehicleId.hasValues()) {
        List<VehicleActivityStructure> filteredActivities = new ArrayList<VehicleActivityStructure>();

        for (VehicleActivityStructure activity : activities) {
          MonitoredVehicleJourneyStructure journey = activity.getMonitoredVehicleJourney();
          AgencyAndId thisVehicleId = AgencyAndIdLibrary.convertFromString(journey.getVehicleRef().getValue());

          // user filtering
          if (!thisVehicleId.equals(vehicleId))
            continue;

          filteredActivities.add(activity);
        }

        activities = filteredActivities;
      }

      _response = generateSiriResponse(activities, routeId, error);
      
      // *** CASE 2: single vehicle--no route specified (that's the case above)
    } else if ((vehicleId != null && vehicleId.hasValues())
        && (routeId == null || !routeId.hasValues())) {
      
      gaLabel = vehicleId.toString();
      
      List<VehicleActivityStructure> activities = new ArrayList<VehicleActivityStructure>();
      
      VehicleActivityStructure activity = _realtimeService.getVehicleActivityForVehicle(
          vehicleId.toString(), maximumOnwardCalls);

      if (activity != null) {
        activities.add(activity);
      }
      
      _response = generateSiriResponse(activities, null, error);

      // *** CASE 3: all vehicles
    } else {
      
      gaLabel = "All Vehicles";
      
      List<VehicleActivityStructure> activities = new ArrayList<VehicleActivityStructure>();
      
      ListBean<VehicleStatusBean> vehicles = _nycTransitDataService.getAllVehiclesForAgency(
          agencyId, getTime().getTime());

      for (VehicleStatusBean v : vehicles.getList()) {
        VehicleActivityStructure activity = _realtimeService.getVehicleActivityForVehicle(
            v.getVehicleId(), maximumOnwardCalls);

        if (activity != null) {
          activities.add(activity);
        }
      }
      
      _response = generateSiriResponse(activities, null, error);
    }
    
    if(_googleAnalytics != null && _request.getParameter("key") != null && !_request.getParameter("key").isEmpty()) {
      try {
      _googleAnalytics.trackEvent("API", "Vehicle Monitoring", gaLabel);
      } catch(Exception e) {
        //discard
      }
    }
    
    try {
      this._servletResponse.getWriter().write(getVehicleMonitoring());
    } catch (IOException e) {
      e.printStackTrace();
    }

    return null;
  }

  /**
   * Generate a siri response for a set of VehicleActivities
   * 
   * @param routeId
   */
  private Siri generateSiriResponse(List<VehicleActivityStructure> activities,
      AgencyAndId routeId, Exception error) {
    
    VehicleMonitoringDeliveryStructure vehicleMonitoringDelivery = new VehicleMonitoringDeliveryStructure();
    vehicleMonitoringDelivery.setResponseTimestamp(getTime());
    
    ServiceDelivery serviceDelivery = new ServiceDelivery();
    serviceDelivery.setResponseTimestamp(getTime());
    serviceDelivery.getVehicleMonitoringDelivery().add(vehicleMonitoringDelivery);
    
    if (error != null) {
      ServiceDeliveryErrorConditionStructure errorConditionStructure = new ServiceDeliveryErrorConditionStructure();
      
      ErrorDescriptionStructure errorDescriptionStructure = new ErrorDescriptionStructure();
      errorDescriptionStructure.setValue(error.getMessage());
      
      OtherErrorStructure otherErrorStructure = new OtherErrorStructure();
      otherErrorStructure.setErrorText(error.getMessage());
      
      errorConditionStructure.setDescription(errorDescriptionStructure);
      errorConditionStructure.setOtherError(otherErrorStructure);
      
      vehicleMonitoringDelivery.setErrorCondition(errorConditionStructure);
    } else {
      Calendar gregorianCalendar = new GregorianCalendar();
      gregorianCalendar.setTime(getTime());
      gregorianCalendar.add(Calendar.MINUTE, 1);
      vehicleMonitoringDelivery.setValidUntil(gregorianCalendar.getTime());

      vehicleMonitoringDelivery.getVehicleActivity().addAll(activities);

      _serviceAlertsHelper.addSituationExchangeToServiceDelivery(serviceDelivery,
          activities, _nycTransitDataService, routeId);
    }

    Siri siri = new Siri();
    siri.setServiceDelivery(serviceDelivery);

    return siri;
  }

  public String getVehicleMonitoring() {
    try {
      if (_type.equals("xml")) {
        this._servletResponse.setContentType("application/xml");
        return _realtimeService.getSiriXmlSerializer().getXml(_response);
      } else {
        this._servletResponse.setContentType("application/json");
        return _realtimeService.getSiriJsonSerializer().getJson(_response,
            _request.getParameter("callback"));
      }
    } catch (Exception e) {
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
