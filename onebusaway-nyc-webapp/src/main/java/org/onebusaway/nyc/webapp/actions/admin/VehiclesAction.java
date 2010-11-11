package org.onebusaway.nyc.webapp.actions.admin;

import java.util.ArrayList;
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
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.presentation.impl.WebappIdParser;
import org.onebusaway.nyc.transit_data.model.NycVehicleStatusBean;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

@Results( {@Result(type = "redirectAction", name = "redirect", params = {
    "namespace", "/admin", "actionName", "vehicles"})})
public class VehiclesAction extends OneBusAwayNYCActionSupport implements ServletRequestAware {

  private static final long serialVersionUID = 1L;
  
  private static final WebappIdParser idParser = new WebappIdParser();
  
  private String agencyId = "MTA NYCT";

  @Autowired
  private TransitDataService transitService;
  
  @Autowired
  private VehicleTrackingManagementService vehicleTrackingManagementService;

  private List<VehicleBag> vehicles = new ArrayList<VehicleBag>();

  private HttpServletRequest request;

  @Override
  public void setServletRequest(HttpServletRequest request) {
    this.request = request;
  }
  
  public void setAgencyId(String agencyId) {
    this.agencyId = agencyId;
  }

  @Override
  public String execute() throws Exception {
    ListBean<VehicleStatusBean> vehiclesForAgencyListBean = transitService.getAllVehiclesForAgency(agencyId, System.currentTimeMillis());
    List<VehicleStatusBean> vehicleStatusBeans = vehiclesForAgencyListBean.getList();
    
    // first get a model of all the vehicle statuses key'ed off vehicle id for easy lookup
    List<NycVehicleStatusBean> allVehicleStatuses = vehicleTrackingManagementService.getAllVehicleStatuses();
    Map<String, NycVehicleStatusBean> vehicleStatusMap = new HashMap<String, NycVehicleStatusBean>();
    for (NycVehicleStatusBean nycVehicleStatusBean : allVehicleStatuses) {
      String vehicleId = nycVehicleStatusBean.getVehicleId();
      vehicleStatusMap.put(vehicleId, nycVehicleStatusBean);
    }

    String method = request.getMethod().toUpperCase();
    if (method.equals("POST")) {
      // keep track of vehicles that have been disabled so we can enable the others
      Set<String> disabledVehicles = new HashSet<String>();

      Enumeration<?> parameterNames = request.getParameterNames();
      while (parameterNames.hasMoreElements()) {
        String key = parameterNames.nextElement().toString();
        if (key.startsWith("disable_")) {
          String vehicleId = key.substring("disable_".length());
          NycVehicleStatusBean nycVehicleStatusBean = vehicleStatusMap.get(vehicleId);
          if (nycVehicleStatusBean == null) {
            vehicleTrackingManagementService.setVehicleStatus(vehicleId, false);            
          } else {
            // no need to call disable on it if it's already disabled
            boolean isDisabled = !nycVehicleStatusBean.isEnabled();
            if (!isDisabled)
              vehicleTrackingManagementService.setVehicleStatus(vehicleId, false);
          }
          disabledVehicles.add(vehicleId);
        }
      }

      // enable all the vehicles that haven't been explicitly disabled from the interface
      for (NycVehicleStatusBean nycVehicleStatusBean : allVehicleStatuses) {
        String vehicleId = nycVehicleStatusBean.getVehicleId();
        if (!disabledVehicles.contains(vehicleId))
            vehicleTrackingManagementService.setVehicleStatus(vehicleId, true);
      }
      
      return "redirect";
    }
    
    for (VehicleStatusBean vehicleStatusBean : vehicleStatusBeans) {
      String vehicleId = vehicleStatusBean.getVehicleId();
      NycVehicleStatusBean nycVehicleStatusBean = vehicleStatusMap.get(vehicleId);
      boolean isDisabled = false;
      if (nycVehicleStatusBean != null) {
        isDisabled = !nycVehicleStatusBean.isEnabled();
      }
      VehicleBag vehicleBag = new VehicleBag(vehicleStatusBean, isDisabled);
      vehicles.add(vehicleBag);
    }
    return SUCCESS;
  }

  public List<VehicleBag> getVehicles() {
    return vehicles;
  }

  // vehicle data bag suitable for use in vehicles jsp
  private static class VehicleBag {
    private VehicleStatusBean vehicleStatusBean;
    private boolean vehicleDisabled;

    public VehicleBag(VehicleStatusBean vehicleStatusBean, boolean isDisabled) {
      this.vehicleStatusBean = vehicleStatusBean;
      this.vehicleDisabled = isDisabled;
    }
    
    @SuppressWarnings("unused")
    public String getVehicleId() {
      String vehicleIdWithAgency = vehicleStatusBean.getVehicleId();
      String idWithoutAgency = idParser.parseIdWithoutAgency(vehicleIdWithAgency);
      return idWithoutAgency;
    }
    
    @SuppressWarnings("unused")
    public String getStatusClass() {
      String status = vehicleStatusBean.getStatus();
      TripBean tripBean = vehicleStatusBean.getTrip();
      if (tripBean == null)
        return "status red";
      String tripHeadsign = tripBean.getTripHeadsign();
      long lastUpdateTime = vehicleStatusBean.getLastUpdateTime();
      long now = System.currentTimeMillis();
      long timeDiff = now - lastUpdateTime;
      long redMillisThreshold = 1000 * 60 * 5;
      long orangeMillisThreshold = 1000 * 60 * 2;
      if (timeDiff > redMillisThreshold)
        return "status red";
      if (timeDiff > orangeMillisThreshold)
        return "status orange";
      if (status == null || !status.equals(EVehiclePhase.IN_PROGRESS.toString()))
        return "status orange";
      return "status normal";
    }
    
    @SuppressWarnings("unused")
    public String getLastUpdateTime() {
      long lastUpdateTime = vehicleStatusBean.getLastUpdateTime();
      long now = System.currentTimeMillis();
      long timeDiff = now - lastUpdateTime;
      long seconds = timeDiff / 1000;
      if (seconds < 60)
        return seconds == 1 ? "1 second" : seconds + " seconds";
      if (seconds >= 60 && seconds < 120)
        return "1 minute";
      long minutes = seconds / 60;
      return minutes + " minutes";
    }
    
    @SuppressWarnings("unused")
    public String getHeadsign() {
      TripBean trip = vehicleStatusBean.getTrip();
      if (trip == null)
        return "Not In Service";
      String tripHeadsign = trip.getTripHeadsign();
      return tripHeadsign;
    }
    
    public String getInferredState() {
      TripBean trip = vehicleStatusBean.getTrip();
      if (trip == null)
        return "No Trip";
      String status = vehicleStatusBean.getStatus();
      if (status != null && status.equals(EVehiclePhase.IN_PROGRESS.toString()))
        return "Normal";
      return "Unknown";
    }
    
    @SuppressWarnings("unused")
    public String getInferredStateClass() {
      String inferredState = getInferredState();
      return inferredState != "Normal" ? "inferred-state error" : "inferred-state";
    }
    
    @SuppressWarnings("unused")
    public String getLocation() {
      CoordinatePoint location = vehicleStatusBean.getLocation();
      double lat = location.getLat();
      double lon = location.getLon();
      return lat + "," + lon;
    }
    
    @SuppressWarnings("unused")
    public String getDisabledName() {
      String vehicleId = vehicleStatusBean.getVehicleId();
      return "disable_" + vehicleId;
    }

    @SuppressWarnings("unused")
    public String isDisabled() {
      return Boolean.valueOf(this.vehicleDisabled).toString();
    }
  }
}
