package org.onebusaway.nyc.presentation.service.realtime;

import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.VehicleActivityStructure;

import java.util.Date;
import java.util.List;

public interface RealtimeService {

  public void setTime(Date time);

  public PresentationService getPresentationService();
  
  public List<VehicleActivityStructure> getVehicleActivityForRoute(String routeId, 
      String directionId, boolean includeNextStops);
    
  public VehicleActivityStructure getVehicleActivityForVehicle(String vehicleId, 
      boolean includeNextStops);
  
  public List<MonitoredStopVisitStructure> getMonitoredStopVisitsForStop(String stopId, 
      boolean includeNextStops);  
  
  public List<NaturalLanguageStringBean> getServiceAlertsForRouteAndDirection(
      String routeId, String directionId);

  public List<NaturalLanguageStringBean> getServiceAlertsForStop(String stopId);
    
}