package org.onebusaway.nyc.presentation.model.search;

import java.util.List;

import org.onebusaway.nyc.presentation.impl.WebappIdParser;
import org.onebusaway.nyc.presentation.model.RouteItem;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

/**
 * Data transfer object for route search results
 */
public class StopSearchResult implements SearchResult {

  private final String stopId;
  private final String name;
  private final List<Double> latlng;
  private final String type;
  private final List<RouteItem> availableRoutes;
  private final List<NaturalLanguageStringBean> serviceAlerts;
  private final String stopDirection;

  public StopSearchResult(String stopId, String name, List<Double> latlng, String stopDirection, 
		  List<RouteItem> availableRoutes, List<NaturalLanguageStringBean> serviceAlerts) {
    this.stopId = stopId;
    this.name = name;
    this.latlng = latlng;
    this.availableRoutes = availableRoutes;
    this.serviceAlerts = serviceAlerts;
    this.stopDirection = stopDirection;
    this.type = "stop";
  }

  public String getStopId() {
    return stopId;
  }
  
  public String getStopIdNoAgency() {
    WebappIdParser webappIdParser = new WebappIdParser();
    return webappIdParser.parseIdWithoutAgency(stopId);
  }

  public String getName() {
    return name;
  }

  public List<Double> getLatlng() {
    return latlng;
  }

  public String getType() {
    return type;
  }

  public String getStopDirection() {
    return stopDirection;
  }

  public List<RouteItem> getRoutesAvailable() {
    return availableRoutes;
  }
  
  public List<NaturalLanguageStringBean> getServiceAlerts() {
	return serviceAlerts;
  }
}
