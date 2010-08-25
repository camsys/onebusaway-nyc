package org.onebusaway.nyc.webapp.actions;

import java.util.ArrayList;
import java.util.List;

import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.webapp.model.StopLatLng;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Handles requests to retrieve stop ids/latlngs
 */
@ParentPackage("json-default")
@Result(type="json")
public class StopsAction extends OneBusAwayNYCActionSupport {

  private static final long serialVersionUID = 1L;
  
  @Autowired
  private TransitDataService service;
  
  private List<StopLatLng> stops = new ArrayList<StopLatLng>();

  // set from request
  private double minLat;
  private double minLng;
  private double maxLat;
  private double maxLng;

  public void setMinLat(double minLat) {
    this.minLat = minLat;
  }

  public void setMinLng(double minLng) {
    this.minLng = minLng;
  }

  public void setMaxLat(double maxLat) {
    this.maxLat = maxLat;
  }

  public void setMaxLng(double maxLng) {
    this.maxLng = maxLng;
  }
  
  @Override
  public String execute() throws Exception {
    CoordinateBounds bounds = new CoordinateBounds(minLat, minLng, maxLat, maxLng);
    SearchQueryBean searchQueryBean = new SearchQueryBean();
    searchQueryBean.setType(SearchQueryBean.EQueryType.BOUNDS);
    searchQueryBean.setBounds(bounds);
    searchQueryBean.setMaxCount(700);
    
    StopsBean stopsBean = service.getStops(searchQueryBean);
    List<StopBean> stopsList = stopsBean.getStops();
    for (StopBean stopBean : stopsList) {
      String stopId = stopBean.getId();
      double lat = stopBean.getLat();
      double lng = stopBean.getLon();
      stops.add(new StopLatLng(stopId, makeLatLng(lat, lng)));
    }
    
    return SUCCESS;
  }

  public List<StopLatLng> getStops() {
    return stops;
  }
}
