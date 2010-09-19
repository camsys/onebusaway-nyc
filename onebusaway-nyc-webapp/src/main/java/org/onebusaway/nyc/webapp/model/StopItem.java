package org.onebusaway.nyc.webapp.model;

import org.onebusaway.nyc.webapp.impl.WebappIdParser;
import org.onebusaway.transit_data.model.StopBean;

/**
 * data transfer object wrapping a stop bean
 * useful to contain logic of returning back just the id portion of the stop
 * which doesn't include the agency id
 */
public class StopItem {

  private final String id;
  private final String name;
  private final Double distance;
  
  private static final WebappIdParser idParser = new WebappIdParser();

  public StopItem(StopBean stopBean, Double distance) {
    this(idParser.parseIdWithoutAgency(stopBean.getId()), stopBean.getName(), distance);
  }
  
  public StopItem(String id, String name, Double distance) {
    this.id = id;
    this.name = name;
    this.distance = distance;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public Double getDistance() {
    return distance;
  }
  
  public String getPresentableDistance() {
    if (distance == null)
      return "";
    int intDistance = distance.intValue();
    return "" + intDistance;
  }
}
