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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts2.interceptor.ServletRequestAware;
import org.onebusaway.nyc.presentation.impl.service_alerts.ServiceAlertsHelper;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriJsonSerializer;
import org.onebusaway.nyc.transit_data_federation.siri.SiriXmlSerializer;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

import uk.org.siri.siri.ServiceDelivery;
import uk.org.siri.siri.Siri;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleMonitoringDeliveryStructure;

public class VehicleMonitoringAction extends OneBusAwayNYCActionSupport
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
    String agencyId = _request.getParameter("OperatorRef");
    String vehicleId = _request.getParameter("VehicleRef");

    String directionId = _request.getParameter("DirectionRef");
    String routeId = _request.getParameter("LineRef");

    int maximumOnwardCalls = 0;        

    String detailLevel = _request.getParameter("VehicleMonitoringDetailLevel");
    if (detailLevel != null && detailLevel.equals("calls")) {
      maximumOnwardCalls = Integer.MAX_VALUE;

      try {
        maximumOnwardCalls = Integer.parseInt(_request.getParameter("MaximumNumberOfCallsOnwards"));
      } catch (NumberFormatException e) {
        maximumOnwardCalls = Integer.MAX_VALUE;
      }
    }

    // *** CASE 1: by route
    if (agencyId != null && routeId != null) {
      String routeIdWithAgency = agencyId + "_" + routeId;

      _response = generateSiriResponse(_realtimeService.getVehicleActivityForRoute(
          routeIdWithAgency, directionId, maximumOnwardCalls));

      return SUCCESS;
    }

    List<VehicleActivityStructure> activities = new ArrayList<VehicleActivityStructure>();

    // *** CASE 2: single vehicle
    if (agencyId != null && vehicleId != null) {
      String vehicleIdWithAgency = agencyId + "_" + vehicleId;

      activities.add(_realtimeService.getVehicleActivityForVehicle(
          vehicleIdWithAgency, maximumOnwardCalls));

      // *** CASE 3: all vehicles
    } else {
      ListBean<VehicleStatusBean> vehicles = _transitDataService.getAllVehiclesForAgency(
          agencyId, _now.getTime());

      for (VehicleStatusBean v : vehicles.getList()) {
        activities.add(_realtimeService.getVehicleActivityForVehicle(
            v.getVehicleId(), maximumOnwardCalls));
      }
    }

    _response = generateSiriResponse(activities);

    return SUCCESS;
  }

  /** Generate a siri response for a set of VehicleActivities */
  private Siri generateSiriResponse(List<VehicleActivityStructure> activities) {

    VehicleMonitoringDeliveryStructure vehicleMonitoringDelivery = new VehicleMonitoringDeliveryStructure();
    vehicleMonitoringDelivery.setResponseTimestamp(_now);

    Calendar gregorianCalendar = new GregorianCalendar();
    gregorianCalendar.setTime(_now);
    gregorianCalendar.add(Calendar.MINUTE, 1);
    vehicleMonitoringDelivery.setValidUntil(gregorianCalendar.getTime());

    vehicleMonitoringDelivery.getVehicleActivity().addAll(activities);

    ServiceDelivery serviceDelivery = new ServiceDelivery();
    serviceDelivery.setResponseTimestamp(_now);
    serviceDelivery.getVehicleMonitoringDelivery().add(
        vehicleMonitoringDelivery);

    _serviceAlertsHelper.addSituationExchangeToSiri(serviceDelivery, activities, _transitDataService);

    Siri siri = new Siri();
    siri.setServiceDelivery(serviceDelivery);

    return siri;
  }

  public String getVehicleMonitoring() {
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
