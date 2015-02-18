package org.onebusaway.nyc.presentation.service.realtime;

import java.util.List;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.nyc.transit_data_federation.siri.SiriJsonSerializerV2;
import org.onebusaway.nyc.transit_data_federation.siri.SiriXmlSerializerV2;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

import uk.org.siri.siri_2.AnnotatedStopPointStructure;
import uk.org.siri.siri_2.MonitoredStopVisitStructure;
import uk.org.siri.siri_2.VehicleActivityStructure;

public interface RealtimeServiceV2 {

  public void setTime(long time);

  public PresentationService getPresentationService();
  
  public SiriJsonSerializerV2 getSiriJsonSerializer();
  
  public SiriXmlSerializerV2 getSiriXmlSerializer();
  
  
  public VehicleActivityStructure getVehicleActivityForVehicle(String vehicleId, 
      int maximumOnwardCalls, long currentTime);
  
  public List<VehicleActivityStructure> getVehicleActivityForRoute(String routeId, 
	      String directionId, int maximumOnwardCalls, long currentTime);
	    
  public List<MonitoredStopVisitStructure> getMonitoredStopVisitsForStop(String stopId, 
      int maximumOnwardCalls, long currentTime);  

  
  public boolean getVehiclesInServiceForRoute(String routeId, String directionId, long currentTime);

  public boolean getVehiclesInServiceForStopAndRoute(String stopId, String routeId, long currentTime);

  
  // FIXME TODO: refactor these to receive a passed in collection of MonitoredStopVisits or VehicleActivities?
  public List<ServiceAlertBean> getServiceAlertsForRoute(String routeId);

  public List<ServiceAlertBean> getServiceAlertsForRouteAndDirection(
      String routeId, String directionId);
  
  public List<ServiceAlertBean> getServiceAlertsGlobal();

  public List<AnnotatedStopPointStructure> getAnnotatedStopPointStructuresForCoordinates(
		CoordinateBounds bounds, String detailLevel, long currentTime);
    
}