package org.onebusaway.nyc.webapp.actions.admin;

import org.apache.struts2.convention.annotation.Result;
import org.apache.struts2.convention.annotation.Results;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.nyc.webapp.actions.OneBusAwayNYCActionSupport;
import org.springframework.beans.factory.annotation.Autowired;

@Results({@Result(type = "redirectAction", name = "redirect", params = {
    "namespace", "/admin", "actionName", "vehicles"})})
public class ResetVehicleAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;

  @Autowired
  private VehicleTrackingManagementService vehicleTrackingManagementService;

  private String _vehicleId;

  public void setVehicleId(String vehicleId) {
    _vehicleId = vehicleId;
  }

  @Override
  public String execute() throws Exception {

    vehicleTrackingManagementService.resetVehicleTrackingForVehicleId(_vehicleId);

    return "redirect";
  }
}
