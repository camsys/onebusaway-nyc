package org.onebusaway.nyc.webapp.actions.admin;

import org.apache.struts2.interceptor.validation.SkipValidation;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleTrackingConfigurationService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.springframework.beans.factory.annotation.Autowired;

public class EditParametersAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  private Integer noProgressTimeout;
  private Double offRouteDistance;
  private Integer staleDataTimeout;
  private Integer staleDataGhostTimeout;

  @Autowired
  private VehicleTrackingConfigurationService _configService;

  @Override
  @SkipValidation
  public String execute() {
    noProgressTimeout = _configService.getVehicleStalledTimeThreshold();
    offRouteDistance = _configService.getVehicleOffRouteDistanceThreshold();
    staleDataTimeout = 300; // TODO
    staleDataGhostTimeout = 120; // TODO
    return SUCCESS;
  }


  // @Validations(requiredStrings = {@RequiredStringValidator(fieldName =
  // "noProgressTimeout", message = "Hey!")})
  public String submit() {

    boolean failed = false;
    if (noProgressTimeout == null) {
      noProgressTimeout = _configService.getVehicleStalledTimeThreshold();
      failed = true;
    } else {
      _configService.setVehicleStalledTimeThreshold(noProgressTimeout);
    }

    if (offRouteDistance == null) {
      offRouteDistance = _configService.getVehicleOffRouteDistanceThreshold();
      failed = true;
    } else {
      _configService.setVehicleOffRouteDistanceThreshold(offRouteDistance);
    }

    if (staleDataTimeout == null) {
      // staleDataTimeout = _configService.get
      staleDataTimeout = 300;
      failed = true;
    }
    if (staleDataGhostTimeout == null) {
      // staleDataTimeout = _configService.get
      staleDataTimeout = 120;
      failed = true;
    }
    if (failed) {
      return INPUT;
    }

    return SUCCESS;
  }


  public void setNoProgressTimeout(int noProgressTimeout) {
    this.noProgressTimeout = noProgressTimeout;
  }

  public String getNoProgressTimeout() {
    return "" + noProgressTimeout;
  }

  public void setOffRouteDistance(double offRouteDistance) {
    this.offRouteDistance = offRouteDistance;
  }

  public double getOffRouteDistance() {
    return offRouteDistance;
  }

  public void setStaleDataTimeout(int staleDataTimeout) {
    this.staleDataTimeout = staleDataTimeout;
  }

  public int getStaleDataTimeout() {
    return staleDataTimeout;
  }

  public void setStaleDataGhostTimeout(int staleDataGhostTimeout) {
    this.staleDataGhostTimeout = staleDataGhostTimeout;
  }

  public int getStaleDataGhostTimeout() {
    return staleDataGhostTimeout;
  }

}
