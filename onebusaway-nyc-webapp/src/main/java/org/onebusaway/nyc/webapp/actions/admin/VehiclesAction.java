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
package org.onebusaway.nyc.webapp.actions.admin;

import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.admin.model.VehicleModel;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

@Results({@Result(type = "redirectAction", name = "redirect", params = {
    "namespace", "/admin", "actionName", "vehicles"})})
public class VehiclesAction extends OneBusAwayNYCActionSupport implements
    ServletRequestAware {

  private static final long serialVersionUID = 1L;

  @Autowired
  private ConfigurationService _configurationService;

  @Autowired
  private VehicleTrackingManagementService _vehicleTrackingManagementService;

  @Autowired
  private TransitDataService _transitService;

  private List<VehicleModel> _vehicles = new ArrayList<VehicleModel>();

  private HttpServletRequest _request;

  @Override
  public void setServletRequest(HttpServletRequest request) {
    this._request = request;
  }

  public String getCurrentTimestamp() {
    Date now = new Date();
    return DateFormat.getDateInstance().format(now) + " "
        + DateFormat.getTimeInstance().format(now);
  }

  @Override
  public String execute() throws Exception {

    ListBean<VehicleStatusBean> vehiclesForAgencyListBean = _transitService.getAllVehiclesForAgency(
        "MTA NYCT", System.currentTimeMillis()); // FIXME

    Map<String, VehicleStatusBean> vehicleMap = new HashMap<String, VehicleStatusBean>();
    for (VehicleStatusBean vehicleStatusBean : vehiclesForAgencyListBean.getList()) {
      String vehicleId = vehicleStatusBean.getVehicleId();
      vehicleMap.put(vehicleId, vehicleStatusBean);
    }

    String method = _request.getMethod().toUpperCase();

    // disable vehicle form submitted
    if (method.equals("POST")) {
      Set<String> disabledVehicles = new HashSet<String>();

      Enumeration<?> parameterNames = _request.getParameterNames();
      while (parameterNames.hasMoreElements()) {
        String key = parameterNames.nextElement().toString();
        if (key.startsWith("disable_")) {
          String vehicleId = key.substring("disable_".length()).replace("^",
              " ");
          disabledVehicles.add(vehicleId);
        }
      }

      for (VehicleStatusBean vehicle : vehicleMap.values()) {
        String vehicleId = vehicle.getVehicleId();
        _vehicleTrackingManagementService.setVehicleStatus(vehicleId,
            !disabledVehicles.contains(vehicleId));
      }

      return "redirect";
    }

    for (VehicleStatusBean vehicleBean : vehicleMap.values()) {
      VehicleModel v = new VehicleModel(
          _vehicleTrackingManagementService.getVehicleManagementStatusBeanForVehicleId(vehicleBean.getVehicleId()),
          vehicleBean,
          _vehicleTrackingManagementService,
          _configurationService);

      _vehicles.add(v);
    }

    return SUCCESS;
  }

  public List<VehicleModel> getVehicles() {
    return _vehicles;
  }
}