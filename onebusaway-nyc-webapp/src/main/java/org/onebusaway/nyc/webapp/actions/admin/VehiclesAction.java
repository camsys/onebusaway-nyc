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

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.apache.struts2.interceptor.ServletRequestAware;
import org.onebusaway.nyc.presentation.service.NycConfigurationService;
import org.onebusaway.nyc.transit_data.model.NycVehicleStatusBean;
import org.onebusaway.nyc.transit_data.model.UtsRecordBean;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.nyc.webapp.actions.admin.model.MonitoredVehicleBean;
import org.onebusaway.nyc.webapp.actions.admin.model.MonitoredVehicle;
import org.onebusaway.nyc.webapp.actions.admin.model.UtsVehicleBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

@Results({@Result(type = "redirectAction", name = "redirect", params = {
    "namespace", "/admin", "actionName", "vehicles"})})
public class VehiclesAction extends OneBusAwayNYCActionSupport implements
    ServletRequestAware {

  private static final long serialVersionUID = 1L;

  @Autowired
  private TransitDataService transitService;

  @Autowired
  private VehicleTrackingManagementService vehicleTrackingManagementService;

  private List<MonitoredVehicle> vehicles = new ArrayList<MonitoredVehicle>();

  private HttpServletRequest request;

  @Autowired
  private NycConfigurationService configurationService;

  @Override
  public void setServletRequest(HttpServletRequest request) {
    this.request = request;
  }

  public String getCurrentTimestamp() {
	Date now = new Date();
	return DateFormat.getDateInstance().format(now) + " " + DateFormat.getTimeInstance().format(now);
  }
  
  @Override
  public String execute() throws Exception {

    String agencyId = configurationService.getDefaultAgencyId();

    ListBean<VehicleStatusBean> vehiclesForAgencyListBean = transitService.getAllVehiclesForAgency(
        agencyId, System.currentTimeMillis());
    List<VehicleStatusBean> vehicleStatusBeans = vehiclesForAgencyListBean.getList();
    List<NycVehicleStatusBean> nycVehicleStatuses = vehicleTrackingManagementService.getAllVehicleStatuses();
    List<UtsRecordBean> utsRecords = vehicleTrackingManagementService.getCurrentUTSRecordsForDepot("JGLT"); // FIXME

    Map<String, VehicleStatusBean> vehicleMap = new HashMap<String, VehicleStatusBean>();
    Map<String, NycVehicleStatusBean> nycVehicleMap = new HashMap<String, NycVehicleStatusBean>();
    Map<String, UtsRecordBean> utsRecordMap = new HashMap<String, UtsRecordBean>();
    for (VehicleStatusBean vehicleStatusBean : vehicleStatusBeans) {
      String vehicleId = vehicleStatusBean.getVehicleId();
      vehicleMap.put(vehicleId, vehicleStatusBean);
    }
    for (NycVehicleStatusBean nycVehicleStatusBean : nycVehicleStatuses) {
      String vehicleId = nycVehicleStatusBean.getVehicleId();
      nycVehicleMap.put(vehicleId, nycVehicleStatusBean);
    }
    for (UtsRecordBean utsRecordBean : utsRecords) {
      String vehicleId = vehicleTrackingManagementService.getDefaultAgencyId() + "_" + utsRecordBean.getBusNumber();
      utsRecordMap.put(vehicleId, utsRecordBean);
    }

    String method = request.getMethod().toUpperCase();
    if (method.equals("POST")) {
      // keep track of vehicles that have been disabled so we can enable the
      // others
      Set<String> disabledVehicles = new HashSet<String>();

      Enumeration<?> parameterNames = request.getParameterNames();
      while (parameterNames.hasMoreElements()) {
        String key = parameterNames.nextElement().toString();
        if (key.startsWith("disable_")) {
          String vehicleId = vehicleTrackingManagementService.getDefaultAgencyId() + "_" + key.substring("disable_".length());

          NycVehicleStatusBean nycVehicleStatusBean = nycVehicleMap.get(vehicleId);
          if (nycVehicleStatusBean == null) {
            vehicleTrackingManagementService.setVehicleStatus(vehicleId, false);
          } else {
            // no need to call disable on it if it's already disabled
            boolean isDisabled = !nycVehicleStatusBean.isEnabled();
            if (!isDisabled)
              vehicleTrackingManagementService.setVehicleStatus(vehicleId,
                  false);
          }
          disabledVehicles.add(vehicleId);
        }
      }

      // enable all the vehicles that haven't been explicitly disabled from the
      // interface
      for (NycVehicleStatusBean nycVehicleStatusBean : nycVehicleStatuses) {
        String vehicleId = nycVehicleStatusBean.getVehicleId();
        if (!disabledVehicles.contains(vehicleId)
            && !nycVehicleStatusBean.isEnabled())
          vehicleTrackingManagementService.setVehicleStatus(vehicleId, true);
      }

      return "redirect";
    }

    for (NycVehicleStatusBean nycVehicleStatusBean : nycVehicleStatuses) {
      String vehicleId = nycVehicleStatusBean.getVehicleId();
      VehicleStatusBean vehicleStatusBean = vehicleMap.get(vehicleId);
      UtsRecordBean utsRecordBean = utsRecordMap.get(vehicleId);
      MonitoredVehicle vehicleBag = new MonitoredVehicleBean(nycVehicleStatusBean,
          vehicleStatusBean, configurationService.getConfiguration(), 
          vehicleTrackingManagementService, utsRecordBean);
      vehicles.add(vehicleBag);
    }

    // add buses to the list that UTS says should be out, but are not present in
    // the list of managed/tracked buses (never sent data to the CIS or were reset).
    for(UtsRecordBean utsRecordBean : utsRecordMap.values()) {
      String vehicleId = vehicleTrackingManagementService.getDefaultAgencyId() 
      						+ "_" + utsRecordBean.getBusNumber();
      
      if(nycVehicleMap.containsKey(vehicleId))
    	continue;
      
      UtsVehicleBean vehicleBag = new UtsVehicleBean(utsRecordBean);
      vehicles.add(vehicleBag);
    }
    
    return SUCCESS;
  }

  public List<MonitoredVehicle> getVehicles() {
    return vehicles;
  }
}
