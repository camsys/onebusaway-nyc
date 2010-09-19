package org.onebusaway.nyc.webapp.actions;

import java.util.ArrayList;
import java.util.List;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.nyc.webapp.model.NextStop;
import org.onebusaway.nyc.webapp.model.VehicleDetails;

/**
 * Handles requests for detailed info on a particular vehicle, suitable for a popup.
 */
@ParentPackage("json-default")
@Result(type="json", params={"callbackParameter", "callback"})
public class VehicleAction extends OneBusAwayNYCActionSupport {
  
  private static final long serialVersionUID = 1L;
  
  private VehicleDetails vehicleDetails;
  private String vehicleId;

  @Override
  public String execute() throws Exception {
    vehicleDetails = makeVehicleDetails(vehicleId);
    
    return SUCCESS;
  }

  // FIXME stubbed data
  private VehicleDetails makeVehicleDetails(String vehicleId) {
//    List<NextStop> nextStops = Arrays.asList(
//        new NextStop[] {
//            new NextStop(vehicleId, "S000001", "Mulberry and Canal", new DistanceAway(2, 100), "one minute ago"),
//            new NextStop(vehicleId, "S000002", "Allen and Delancey", new DistanceAway(3, 150), "one minute ago")
//        });
    List<NextStop> nextStops = new ArrayList<NextStop>();
    return new VehicleDetails(vehicleId, "LI CITY QUEENS PLAZA", "one minute ago", "B62", nextStops);
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }
  
  public VehicleDetails getVehicle() {
    return vehicleDetails;
  }
}
