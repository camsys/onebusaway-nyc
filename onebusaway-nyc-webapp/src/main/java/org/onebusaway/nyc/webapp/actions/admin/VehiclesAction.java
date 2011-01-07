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
import org.onebusaway.nyc.presentation.impl.WebappIdParser;
import org.onebusaway.nyc.presentation.service.ConfigurationBean;
import org.onebusaway.nyc.presentation.service.NycConfigurationService;
import org.onebusaway.nyc.transit_data.model.NycVehicleStatusBean;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

@Results({@Result(type = "redirectAction", name = "redirect", params = {
    "namespace", "/admin", "actionName", "vehicles"})})
public class VehiclesAction extends OneBusAwayNYCActionSupport implements
    ServletRequestAware {

  private static final long serialVersionUID = 1L;

  private static final WebappIdParser idParser = new WebappIdParser();

  @Autowired
  private TransitDataService transitService;

  @Autowired
  private VehicleTrackingManagementService vehicleTrackingManagementService;

  private List<VehicleBag> vehicles = new ArrayList<VehicleBag>();

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
    Map<String, VehicleStatusBean> vehicleMap = new HashMap<String, VehicleStatusBean>();
    Map<String, NycVehicleStatusBean> nycVehicleMap = new HashMap<String, NycVehicleStatusBean>();
    for (VehicleStatusBean vehicleStatusBean : vehicleStatusBeans) {
      String vehicleId = vehicleStatusBean.getVehicleId();
      vehicleMap.put(vehicleId, vehicleStatusBean);
    }
    for (NycVehicleStatusBean nycVehicleStatusBean : nycVehicleStatuses) {
      String vehicleId = nycVehicleStatusBean.getVehicleId();
      nycVehicleMap.put(idParser.parseIdWithoutAgency(vehicleId), nycVehicleStatusBean);
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
          String vehicleId = key.substring("disable_".length());
          NycVehicleStatusBean nycVehicleStatusBean = nycVehicleMap.get(vehicleId);
          if (nycVehicleStatusBean != null) {
            // no need to call disable on it if it's already disabled
            boolean isDisabled = !nycVehicleStatusBean.isEnabled();
            if (!isDisabled)
              vehicleTrackingManagementService.setVehicleStatus(nycVehicleStatusBean.getVehicleId(),
                  false);
          }
          disabledVehicles.add(vehicleId);
        }
      }

      // enable all the vehicles that haven't been explicitly disabled from the
      // interface
      for (NycVehicleStatusBean nycVehicleStatusBean : nycVehicleStatuses) {
        String vehicleId = idParser.parseIdWithoutAgency(nycVehicleStatusBean.getVehicleId());
        if (!disabledVehicles.contains(vehicleId)
            && !nycVehicleStatusBean.isEnabled())
          vehicleTrackingManagementService.setVehicleStatus(nycVehicleStatusBean.getVehicleId(), true);
      }

      return "redirect";
    }

    for (NycVehicleStatusBean nycVehicleStatusBean : nycVehicleStatuses) {
      String vehicleId = nycVehicleStatusBean.getVehicleId();
      VehicleStatusBean vehicleStatusBean = vehicleMap.get(vehicleId);
      VehicleBag vehicleBag = new VehicleBag(nycVehicleStatusBean,
          vehicleStatusBean, configurationService.getConfiguration(), vehicleTrackingManagementService);
      vehicles.add(vehicleBag);
    }
    return SUCCESS;
  }

  public List<VehicleBag> getVehicles() {
    return vehicles;
  }

  // vehicle data bag suitable for use in vehicles jsp
  private static class VehicleBag {
    private NycVehicleStatusBean nycVehicleStatusBean;
    private VehicleStatusBean vehicleStatusBean;
    private ConfigurationBean configuration;
    private VehicleTrackingManagementService vehicleTrackingManagementService;
    
    public VehicleBag(NycVehicleStatusBean nycVehicleStatusBean,
        VehicleStatusBean vehicleStatusBean, ConfigurationBean configuration,
        VehicleTrackingManagementService vehicleTrackingManagementService) {
      this.nycVehicleStatusBean = nycVehicleStatusBean;
      this.vehicleStatusBean = vehicleStatusBean;
      this.configuration = configuration;
      this.vehicleTrackingManagementService = vehicleTrackingManagementService;
    }

    @SuppressWarnings("unused")
    public String getVehicleIdWithoutAgency() {
      String vehicleIdWithAgency = nycVehicleStatusBean.getVehicleId();
      String idWithoutAgency = idParser.parseIdWithoutAgency(vehicleIdWithAgency);
      return idWithoutAgency;
    }

    @SuppressWarnings("unused")
    public String getVehicleId() {
      String vehicleIdWithAgency = nycVehicleStatusBean.getVehicleId();
      return vehicleIdWithAgency;
    }
    
    @SuppressWarnings("unused")
    public String getStatusClass() {
      if (vehicleStatusBean == null)
        return "status red";
      String phase = nycVehicleStatusBean.getPhase();
      TripBean tripBean = vehicleStatusBean.getTrip();
      if (tripBean == null)
        return "status red";
      String tripHeadsign = tripBean.getTripHeadsign();
      long lastUpdateTime = nycVehicleStatusBean.getLastUpdateTime();
      long lastGpsTime = nycVehicleStatusBean.getLastGpsTime();
      long now = System.currentTimeMillis();
      long updateTimeDiff = now - lastUpdateTime;
      long gpsTimeDiff = now - lastGpsTime;
      long redMillisThreshold = configuration.getNoProgressTimeout() * 1000;
      long orangeMillisThreshold = configuration.getHideTimeout() * 1000;
      if (updateTimeDiff > redMillisThreshold
          || gpsTimeDiff > redMillisThreshold)
        return "status red";
      if (updateTimeDiff > orangeMillisThreshold
          || gpsTimeDiff > orangeMillisThreshold)
        return "status orange";
      if (phase == null || !phase.equals(EVehiclePhase.IN_PROGRESS.toLabel()))
        return "status orange";
      return "status green";
    }

    public String formatTimeInterval(long seconds) {
    	if (seconds < 60) {
           return seconds == 1 ? "1 second" : seconds + " seconds";
    	} else {    	
            long minutes = seconds / 60;
            long days = minutes / (60 * 24);
            
            if(days < 1) {
            	return minutes == 1 ? "1 minute" : minutes + " minutes";
            } else {
            	return days == 1 ? "1 day" : String.format("%1.2f", days) + " days";
            }
    	}
    }
    
    @SuppressWarnings("unused")
    public String getLastUpdateTime() {
      long lastUpdateTime = nycVehicleStatusBean.getLastUpdateTime();

      if(lastUpdateTime <= 0)
    	  return "Not Available";

      long now = System.currentTimeMillis();
      long timeDiff = now - lastUpdateTime;
      long seconds = timeDiff / 1000;
      return formatTimeInterval(seconds);      
    }

    @SuppressWarnings("unused")
    public String getLastCommTime() {
      long lastUpdateTime = nycVehicleStatusBean.getLastGpsTime();
      
      if(lastUpdateTime <= 0)
    	  return "Not Available";
      
      long now = System.currentTimeMillis();
      long timeDiff = now - lastUpdateTime;
      long seconds = timeDiff / 1000;
      return formatTimeInterval(seconds);      
    }

    @SuppressWarnings("unused")
    public String getHeadsign() {
      if (vehicleStatusBean == null)
        return "Not Available";
      
      TripBean trip = vehicleStatusBean.getTrip();
      String mostRecentDestinationSignCode = nycVehicleStatusBean.getMostRecentDestinationSignCode();
      boolean mostRecentDSCIsOutOfService = vehicleTrackingManagementService.isOutOfServiceDestinationSignCode(mostRecentDestinationSignCode);

      if (trip == null) {
    	  if(mostRecentDSCIsOutOfService)
    		  return mostRecentDestinationSignCode + ": Not In Service";
    	  else 	   	  
    		  return "Unknown<br/><span class='error'>(bus sent " + mostRecentDestinationSignCode + ")</span>";
      }
      
      if(nycVehicleStatusBean.getPhase().equals(EVehiclePhase.DEADHEAD_AFTER.toLabel()) ||
    	  nycVehicleStatusBean.getPhase().equals(EVehiclePhase.DEADHEAD_BEFORE.toLabel()) ||
    	  nycVehicleStatusBean.getPhase().equals(EVehiclePhase.DEADHEAD_DURING.toLabel())) {
    	  return "Not Applicable<br/>(bus sent " + mostRecentDestinationSignCode + ")";    	  
      }
      
	  String tripHeadsign = trip.getTripHeadsign();
      String inferredDestinationSignCode = nycVehicleStatusBean.getInferredDestinationSignCode();
      if (inferredDestinationSignCode.equals(mostRecentDestinationSignCode)) {
        return inferredDestinationSignCode + ": " + tripHeadsign;
      } else {
        return inferredDestinationSignCode + ": " + tripHeadsign
            + "<br/><span class='error'>(bus sent " + mostRecentDestinationSignCode + ")</span>";
      }
    }

    private String upperCaseWords(String s) {
    	StringBuilder result = new StringBuilder(s.length());
    	String[] words = s.split("\\s|_");
    	for(int i=0,l=words.length;i<l;++i) {
    	  if(i>0) result.append(" ");      
    	  result.append(Character.toUpperCase(words[i].charAt(0)))
    	        .append(words[i].substring(1));

    	}
    	return result.toString();
    }
    
    @SuppressWarnings("unused")
    public String getInferredState() {
      if(vehicleStatusBean == null)
    	  return "Not Available";
    	
      if (!nycVehicleStatusBean.isEnabled())
        return "Disabled";

      TripBean trip = vehicleStatusBean.getTrip();
      if (trip == null)
        return "No Trip";
      
      String status = nycVehicleStatusBean.getStatus();
      String phase = nycVehicleStatusBean.getPhase();
      StringBuilder sb = new StringBuilder();
      
      if(phase != null)
    	  sb.append("Phase: " + this.upperCaseWords(phase));
      else
    	  sb.append("Phase: None");
    	  
      if(sb.length() > 0)
    	  sb.append("<br/>");
      
      if(status != null)
    	  sb.append("Status: " + this.upperCaseWords(status));
      else
    	  sb.append("Status: None");
      
      return sb.toString();
    }

    @SuppressWarnings("unused")
    public String getLocation() {
      double lat = nycVehicleStatusBean.getLastGpsLat();
      double lon = nycVehicleStatusBean.getLastGpsLon();
      
      if(Double.isNaN(lat) || Double.isNaN(lon))
    	  return "Not Available";
      
      return lat + "," + lon;
    }
    
    @SuppressWarnings("unused")
    public String getOrientation() {
      try {
    	  double orientation = vehicleStatusBean.getTripStatus().getOrientation();
      
    	  if(Double.isNaN(orientation))
    		  return "Not Available";
    	  else
    		  return new Long(Math.round(orientation)).toString();
      } catch(Exception e) {
    	  return "Not Available";
      }
    }    

    @SuppressWarnings("unused")
    public boolean isDisabled() {
      return !nycVehicleStatusBean.isEnabled();
    }
  }
}
