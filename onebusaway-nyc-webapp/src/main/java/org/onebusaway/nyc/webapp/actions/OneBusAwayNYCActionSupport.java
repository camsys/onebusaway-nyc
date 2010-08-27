package org.onebusaway.nyc.webapp.actions;

import java.util.Arrays;
import java.util.List;

import org.onebusaway.nyc.webapp.model.AvailableRoute;
import org.onebusaway.nyc.webapp.model.DistanceAway;

import com.opensymphony.xwork2.ActionSupport;

/**
 * Abstract class that is currently being used to hang stub data methods onto
 */
public abstract class OneBusAwayNYCActionSupport extends ActionSupport {

  private static final long serialVersionUID = 1L;

  protected List<Double> makeLatLng(double lat, double lng) {
    return Arrays.asList(new Double[] { lat, lng} );
  }

  // FIXME stubbed data
  protected List<AvailableRoute> makeAvailableRoutes() {
    List<DistanceAway> distanceAways = Arrays.asList(
        new DistanceAway[] { new DistanceAway(2, 100), new DistanceAway(3, 2500) });       
    return Arrays.asList(new AvailableRoute[] {
        new AvailableRoute("M14A",
            "14th Street Crosstown to LES/Delancey via Avenue A",
            distanceAways),
        new AvailableRoute("M14D",
            "14th Street Crosstown to LES/Delancey via Avenue D",
            distanceAways)});
  }

  protected String parseIdWithoutAgency(String id) {
    if (id == null) throw new NullPointerException("id is null");
    id = id.trim();
    String[] fields = id.split("_", 2);
    if (fields.length != 2) throw new IllegalArgumentException("'" + id + "' does not look like an id with an agency");
    return fields[1];
  }

}
