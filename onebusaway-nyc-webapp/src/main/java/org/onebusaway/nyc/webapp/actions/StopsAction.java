package org.onebusaway.nyc.webapp.actions;

import java.util.ArrayList;
import java.util.List;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.nyc.webapp.model.StopLatLng;

/**
 * Handles requests to retrieve stop ids/latlngs
 */
@ParentPackage("json-default")
@Result(type="json")
public class StopsAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  
  private List<StopLatLng> stops = new ArrayList<StopLatLng>();

  @Override
  public String execute() throws Exception {
    // FIXME stubbed data
    stops.add(new StopLatLng("S000001", makeLatLng(40.717078345319955, -73.9985418201294)));
    stops.add(new StopLatLng("S000002", makeLatLng(40.71912753071832, -73.99034498937989)));

    return SUCCESS;
  }

  public List<StopLatLng> getStops() {
    return stops;
  }
}
