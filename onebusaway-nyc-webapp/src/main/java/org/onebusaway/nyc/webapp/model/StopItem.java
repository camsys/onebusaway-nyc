package org.onebusaway.nyc.webapp.model;

import org.onebusaway.transit_data.model.StopBean;

/**
 * data transfer object wrapping a stop bean
 * useful to contain logic of returning back just the id portion of the stop
 * which doesn't include the agency id
 */
public class StopItem {

  private final String id;
  private final String name;

  public StopItem(StopBean stopBean) {
    String stopBeanId = stopBean.getId();
    String[] idFields = stopBeanId.split("_");
    if (idFields.length != 2)
      throw new IllegalArgumentException("Invalid stop bean id that doesn't contain agency: " + stopBeanId);
    this.id = idFields[1];
    this.name = stopBean.getName();
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }
}
