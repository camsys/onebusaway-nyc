package org.onebusaway.nyc.webapp.model;

import java.util.List;

/**
 * Data transfer object for a stop containing its id and latlngs
 */
public class StopLatLng {

  private final String stopId;
  private final List<Double> latlng;

  public StopLatLng(String stopId, List<Double> latlng) {
    this.stopId = stopId;
    this.latlng = latlng;
  }

  public String getStopId() {
    return stopId;
  }

  public List<Double> getLatlng() {
    return latlng;
  }

}
