package org.onebusaway.nyc.presentation.service.realtime;

import org.onebusaway.nyc.presentation.model.realtime.DistanceAway;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import java.util.Date;
import java.util.List;

public interface RealtimeService {

  public void setTime(Date time);

  public List<DistanceAway> getDistanceAwaysForStopAndHeadsign(
      String stopId, String headsign);

  public List<NaturalLanguageStringBean> getServiceAlertsForRouteAndDirection(
      String routeId, String directionId);

  public List<NaturalLanguageStringBean> getServiceAlertsForStop(
      String stopId);

}