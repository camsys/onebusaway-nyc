package org.onebusaway.nyc.webapp.actions;

import java.util.List;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.nyc.webapp.model.AvailableRoute;
import org.onebusaway.nyc.webapp.model.StopDetails;

/**
 * Handles requests for detailed info for a particular stop, suitable for a popup
 */
@ParentPackage("json-default")
@Result(type="json")
public class StopAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  private StopDetails stopDetails;
  private String stopId;
  
  @Override
  public String execute() throws Exception {
    stopDetails = makeStopDetails(stopId);
    return SUCCESS;
  }
  
  // FIXME stubbed data
  private StopDetails makeStopDetails(String stopId) {
    String name;
    List<Double> latlng;
    List<AvailableRoute> availableRoutes = makeAvailableRoutes();
    
    if (stopId.equals("S000001")) {
      name = "Mulberry and Canal";
      latlng = makeLatLng(40.717078345319955, -73.9985418201294);
    } else {
      name = "Allen and Delancey";
      latlng = makeLatLng(40.71912753071832, -73.99034498937989);
    }
    return new StopDetails(stopId, name, latlng, "one minute ago", availableRoutes);
  }

  public void setStopId(String stopId) {
    this.stopId = stopId;
  }
  
  public StopDetails getStop() {
    return stopDetails;
  }

}
