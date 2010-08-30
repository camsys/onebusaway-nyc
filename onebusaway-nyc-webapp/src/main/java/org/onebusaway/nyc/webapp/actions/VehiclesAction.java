package org.onebusaway.nyc.webapp.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.nyc.webapp.model.VehicleLatLng;
import org.onebusaway.nyc.webapp.model.VehicleRoute;

/**
 * Handles requests for vehicle locations given route ids.
 * This is suitable for updating the locations of vehicles for currently displayed routes.
 */
@ParentPackage("json-default")
@Result(type="json", params={"callbackParameter", "callback"})
public class VehiclesAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  
  private List<VehicleRoute> vehicles = new ArrayList<VehicleRoute>();
  private String[] routeIds;

  @Override
  public String execute() throws Exception {
    vehicles = makeVehicleLatLngs(routeIds);
    return SUCCESS;
  }
  
  // FIXME stubbed data
  private List<VehicleRoute> makeVehicleLatLngs(String[] routeIds) {
    List<VehicleRoute> vehicles = new ArrayList<VehicleRoute>(2);
    for (String routeId : routeIds) {
      String justRouteId = parseIdWithoutAgency(routeId);
      if (justRouteId.equals("Q54")) {
        List<VehicleLatLng> vehicleLatLngs = Arrays.asList(
            new VehicleLatLng[] {
                new VehicleLatLng("8213", makeLatLng(40.71818426273522, -74.00017260321046)),
                new VehicleLatLng("8210", makeLatLng(40.71408577078771, -73.9972972751465))
            });
        vehicles.add(new VehicleRoute(routeId, vehicleLatLngs));
      } else if (justRouteId.equals("B62")) {
        List<VehicleLatLng> vehicleLatLngs = Arrays.asList(
            new VehicleLatLng[] {
                new VehicleLatLng("8214", makeLatLng(40.71805415575759, -73.99085997351075)),
                new VehicleLatLng("8215", makeLatLng(40.71213401927467, -73.99219034918214))
            });
        vehicles.add(new VehicleRoute(routeId, vehicleLatLngs));
      }
    }
    return vehicles;
  }

  public void setRouteIds(String[] routeIds) {
    this.routeIds = routeIds;
  }
  
  public List<VehicleRoute> getVehicles() {
    return vehicles;
  }
}
