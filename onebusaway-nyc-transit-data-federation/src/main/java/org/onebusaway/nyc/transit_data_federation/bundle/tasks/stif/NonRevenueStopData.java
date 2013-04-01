package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

import java.io.Serializable;

import com.vividsolutions.jts.geom.Coordinate;

public class NonRevenueStopData implements Serializable {
  
  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  private NonRevenueStopOrder nonRevenueStopOrder;
  
  private Coordinate location;
  
  private int scheduleTime;

  public NonRevenueStopOrder getNonRevenueStopOrder() {
    return nonRevenueStopOrder;
  }

  public void setNonRevenueStopOrder(NonRevenueStopOrder nonRevenueStopOrder) {
    this.nonRevenueStopOrder = nonRevenueStopOrder;
  }

  public Coordinate getLocation() {
    return location;
  }

  public void setLocation(Coordinate location) {
    this.location = location;
  }

  public int getScheduleTime() {
    return scheduleTime;
  }

  public void setScheduleTime(int scheduleTime) {
    this.scheduleTime = scheduleTime;
  }

}
