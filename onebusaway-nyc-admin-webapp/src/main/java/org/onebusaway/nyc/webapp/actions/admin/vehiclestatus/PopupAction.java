package org.onebusaway.nyc.webapp.actions.admin.vehiclestatus;

import org.onebusaway.nyc.admin.model.ui.VehicleDetail;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;
import org.onebusaway.nyc.admin.service.VehicleStatusService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCAdminActionSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class PopupAction extends OneBusAwayNYCAdminActionSupport {
  private static Logger _log = LoggerFactory.getLogger(PopupAction.class);	
  private static final long serialVersionUID = 1L;
  private VehicleStatusService vehicleStatusService;
	private ConfigurationService configurationService;
	private VehicleDetail vehicleDetailRecord;
	
  private String vehicleId = null;


  public String input() {
    _log.info("in input");
    vehicleDetailRecord = vehicleStatusService.getVehicleDetail(vehicleId);
    _log.info("retrieved vehicleDetail=" + vehicleDetailRecord);
    return INPUT;
  }
  
  
  /**
	 * Injects vehicle status service
	 * @param vehicleStatusService the vehicleStatusService to set
	 */
	@Autowired
	public void setVehicleStatusService(VehicleStatusService vehicleStatusService) {
		this.vehicleStatusService = vehicleStatusService;
	}

	@Autowired
	public void setConfigurationService(ConfigurationService configurationService) {
	  this.configurationService = configurationService;
	}

 public String getGoogleMapsClientId() {
    return configurationService.getConfigurationValueAsString("display.googleMapsClientId", "");    
  }

	public String getVehicleId() {
	  if (vehicleDetailRecord != null) {
	    return vehicleDetailRecord.getVehicleId();
	  }
	  return null;
	}
	
	public void setVehicleId(String vehicleId) {
	  this.vehicleId = vehicleId;
	}

	public String getLocation() {
	  if (vehicleDetailRecord != null) {
	    return vehicleDetailRecord.getLocation();
	  }
	  return null;
	}
	
	public Double getOrientation() {
	  if (vehicleDetailRecord != null) {
	    return vehicleDetailRecord.getDirection();
	  }
	  return null;
	}
	
	public String getDepot() {
	  if (vehicleDetailRecord != null) {
	    return vehicleDetailRecord.getDepot();
	  }
	  return null;
	}
	
	public String getHeadSign() {
	  if (vehicleDetailRecord != null) {
	    return vehicleDetailRecord.getHeadSign();
	  }
	  return null;
	}
	
	public String getInferredHeadSign() {
	  if (vehicleDetailRecord != null) {
	    return vehicleDetailRecord.getInferredHeadSign();
	  }
	  return null;
	}
}
