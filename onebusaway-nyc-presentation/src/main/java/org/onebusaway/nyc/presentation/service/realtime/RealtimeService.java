package org.onebusaway.nyc.presentation.service.realtime;

import org.onebusaway.nyc.transit_data_federation.siri.SiriJsonSerializer;
import org.onebusaway.nyc.transit_data_federation.siri.SiriXmlSerializer;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;

import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.VehicleActivityStructure;

import java.util.Date;
import java.util.List;

public interface RealtimeService {

  public void setTime(Date time);

  public PresentationService getPresentationService();
  
  public SiriJsonSerializer getSiriJsonSerializer();
  
  public SiriXmlSerializer getSiriXmlSerializer();
  
  public List<VehicleActivityStructure> getVehicleActivityForRoute(String routeId, 
      String directionId, int maximumOnwardCalls);
    
  public VehicleActivityStructure getVehicleActivityForVehicle(String vehicleId, 
      int maximumOnwardCalls);
  
  public List<MonitoredStopVisitStructure> getMonitoredStopVisitsForStop(String stopId, 
      int maximumOnwardCalls);  

  // FIXME TODO: refactor these to receive a passed in collection of MonitoredStopVisits or VehicleActivities?
  public List<NaturalLanguageStringBean> getServiceAlertsForRoute(String routeId);

  public List<NaturalLanguageStringBean> getServiceAlertsForRouteAndDirection(
      String routeId, String directionId);

  public List<NaturalLanguageStringBean> getServiceAlertsForStop(String stopId);
    
}