package org.onebusaway.nyc.presentation.service.realtime;

import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopGroupBean;

import java.util.Date;

public interface ScheduledServiceService {

  public void setTime(Date time);

  public Boolean hasUpcomingScheduledService(RouteBean routeBean, StopGroupBean stopGroup);

}