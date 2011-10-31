package org.onebusaway.nyc.presentation.service.realtime;

import org.onebusaway.nyc.presentation.model.realtime.DistanceAway;
import org.onebusaway.nyc.presentation.model.realtime.VehicleResult;
import org.onebusaway.nyc.presentation.model.search.RouteDestinationItem;
import org.onebusaway.nyc.presentation.service.RealtimeModelFactory;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import java.util.Date;
import java.util.List;

public interface RealtimeService {

  public void setTime(Date time);

  public void setModelFactory(RealtimeModelFactory factory);

  public List<DistanceAway> getDistanceAwaysForStopAndDestination(
      String stopId, RouteDestinationItem destination);

  public List<VehicleResult> getLocationsForVehiclesServingRoute(String routeId);

  public List<NaturalLanguageStringBean> getServiceAlertsForRouteAndDirection(
      String routeId, String directionId);

  public List<NaturalLanguageStringBean> getServiceAlertsForStop(String stopId);

}