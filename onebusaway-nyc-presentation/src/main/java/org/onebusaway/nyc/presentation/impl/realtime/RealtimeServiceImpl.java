package org.onebusaway.nyc.presentation.impl.realtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.onebusaway.nyc.presentation.impl.realtime.SiriSupport.OnwardCallsMode;
import org.onebusaway.nyc.presentation.service.predictions.PredictionIntegrationService;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriExtensionWrapper;
import org.onebusaway.nyc.transit_data_federation.siri.SiriJsonSerializer;
import org.onebusaway.nyc.transit_data_federation.siri.SiriXmlSerializer;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalsAndDeparturesQueryBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleActivityStructure.MonitoredVehicleJourney;

@Component
public class RealtimeServiceImpl implements RealtimeService {

  private NycTransitDataService _nycTransitDataService;

  private PresentationService _presentationService;
  
  private PredictionIntegrationService _predictionService;

  private SiriXmlSerializer _siriXmlSerializer = new SiriXmlSerializer();

  private SiriJsonSerializer _siriJsonSerializer = new SiriJsonSerializer();

  private Date _now = null;
  
  @Override
  public void setTime(Date time) {
    _now = time;
    _presentationService.setTime(time);
  }

  public long getTime() {
    if(_now != null)
      return _now.getTime();
    else
      return System.currentTimeMillis();
  }

  @Autowired
  public void setNycTransitDataService(NycTransitDataService transitDataService) {
    _nycTransitDataService = transitDataService;
  }

  @Autowired
  public void setPredictionIntegrationService(PredictionIntegrationService predictionService) {
	  _predictionService = predictionService;
  }

  @Autowired
  public void setPresentationService(PresentationService presentationService) {
    _presentationService = presentationService;
  }
  
  @Override
  public PresentationService getPresentationService() {
    return _presentationService;
  }  

  @Override
  public SiriJsonSerializer getSiriJsonSerializer() {
    return _siriJsonSerializer;
  }
  
  @Override
  public SiriXmlSerializer getSiriXmlSerializer() {
    return _siriXmlSerializer;
  }

  @Override
  public List<VehicleActivityStructure> getVehicleActivityForRoute(String routeId, String directionId, int maximumOnwardCalls) {
    List<VehicleActivityStructure> output = new ArrayList<VehicleActivityStructure>();
    
    ListBean<TripDetailsBean> trips = getAllTripsForRoute(routeId);
    for(TripDetailsBean tripDetails : trips.getList()) {
      // filter out interlined routes
      if(routeId != null && !tripDetails.getTrip().getRoute().getId().equals(routeId))
        continue;

      // filtered out by user
      if(directionId != null && !tripDetails.getTrip().getDirectionId().equals(directionId))
        continue;
      
      if(!_presentationService.include(tripDetails.getStatus()))
        continue;
        
      VehicleActivityStructure activity = new VehicleActivityStructure();
      activity.setRecordedAtTime(new Date(tripDetails.getStatus().getLastUpdateTime()));

      List<TimepointPredictionRecord> timePredictionRecords = null;
      if(_presentationService.useTimePredictionsIfAvailable(tripDetails.getStatus()) == true) {
    	  timePredictionRecords = _predictionService.getTimepointPredictions(tripDetails.getStatus());
      }
      
      activity.setMonitoredVehicleJourney(new MonitoredVehicleJourney());      
      SiriSupport.fillMonitoredVehicleJourney(activity.getMonitoredVehicleJourney(), 
          tripDetails, null, OnwardCallsMode.VEHICLE_MONITORING,
          _presentationService, _nycTransitDataService, maximumOnwardCalls, 
          timePredictionRecords);
            
      output.add(activity);
    }

    Collections.sort(output, new Comparator<VehicleActivityStructure>() {
      public int compare(VehicleActivityStructure arg0, VehicleActivityStructure arg1) {
        try {
          SiriExtensionWrapper wrapper0 = (SiriExtensionWrapper)arg0.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().getAny();
          SiriExtensionWrapper wrapper1 = (SiriExtensionWrapper)arg1.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().getAny();
          return wrapper0.getDistances().getDistanceFromCall().compareTo(wrapper1.getDistances().getDistanceFromCall());        
        } catch(Exception e) {
          return -1;
        }
      }
    });
    
    return output;
  }

  @Override
  public VehicleActivityStructure getVehicleActivityForVehicle(String vehicleId, int maximumOnwardCalls) {    
    TripForVehicleQueryBean query = new TripForVehicleQueryBean();
    query.setTime(new Date(getTime()));
    query.setVehicleId(vehicleId);

    TripDetailsInclusionBean inclusion = new TripDetailsInclusionBean();
    inclusion.setIncludeTripStatus(true);
    inclusion.setIncludeTripBean(true);
    query.setInclusion(inclusion);

    TripDetailsBean tripDetailsForCurrentTrip = _nycTransitDataService.getTripDetailsForVehicleAndTime(query);
    
    if (tripDetailsForCurrentTrip != null) {
      if(!_presentationService.include(tripDetailsForCurrentTrip.getStatus()))
        return null;
      
      VehicleActivityStructure output = new VehicleActivityStructure();
      output.setRecordedAtTime(new Date(tripDetailsForCurrentTrip.getStatus().getLastUpdateTime()));

      List<TimepointPredictionRecord> timePredictionRecords = null;
      if(_presentationService.useTimePredictionsIfAvailable(tripDetailsForCurrentTrip.getStatus()) == true) {
    	  timePredictionRecords = _predictionService.getTimepointPredictions(tripDetailsForCurrentTrip.getStatus());
      }

      output.setMonitoredVehicleJourney(new MonitoredVehicleJourney());
      SiriSupport.fillMonitoredVehicleJourney(output.getMonitoredVehicleJourney(), 
    	  tripDetailsForCurrentTrip, null, OnwardCallsMode.VEHICLE_MONITORING,
    	  _presentationService, _nycTransitDataService, maximumOnwardCalls,
    	  timePredictionRecords);

      return output;
    }
    
    return null;
  }

  @Override
  public List<MonitoredStopVisitStructure> getMonitoredStopVisitsForStop(String stopId, int maximumOnwardCalls) {
    List<MonitoredStopVisitStructure> output = new ArrayList<MonitoredStopVisitStructure>();

    for (ArrivalAndDepartureBean adBean : getArrivalsAndDeparturesForStop(stopId)) {
      TripStatusBean _statusBean = adBean.getTripStatus();      
      if(_statusBean == null)
    	  continue;
      
      TripDetailsQueryBean query = new TripDetailsQueryBean();
      query.setServiceDate(adBean.getServiceDate());
      query.setTripId(adBean.getTrip().getId());
      query.setTime(getTime());
      query.setVehicleId(_statusBean.getVehicleId());

      TripDetailsInclusionBean inclusion = new TripDetailsInclusionBean();
      inclusion.setIncludeTripStatus(true);
      inclusion.setIncludeTripBean(true);
      query.setInclusion(inclusion);

      TripDetailsBean tripDetailsForADTrip = _nycTransitDataService.getSingleTripDetails(query);      
      TripStatusBean statusBean = tripDetailsForADTrip.getStatus();      

      if(!_presentationService.include(statusBean) || !_presentationService.include(adBean, statusBean))
          continue;

      MonitoredStopVisitStructure stopVisit = new MonitoredStopVisitStructure();
      stopVisit.setRecordedAtTime(new Date(statusBean.getLastUpdateTime()));
        
      List<TimepointPredictionRecord> timePredictionRecords = null;
      if(_presentationService.useTimePredictionsIfAvailable(statusBean) == true) {
    	  timePredictionRecords = _predictionService.getTimepointPredictions(statusBean);
      }

      stopVisit.setMonitoredVehicleJourney(new MonitoredVehicleJourneyStructure());
      SiriSupport.fillMonitoredVehicleJourney(stopVisit.getMonitoredVehicleJourney(), 
    	  tripDetailsForADTrip, adBean.getStop(), OnwardCallsMode.STOP_MONITORING,
    	  _presentationService, _nycTransitDataService, maximumOnwardCalls,
    	  timePredictionRecords);

      output.add(stopVisit);
    }
    
    Collections.sort(output, new Comparator<MonitoredStopVisitStructure>() {
      public int compare(MonitoredStopVisitStructure arg0, MonitoredStopVisitStructure arg1) {
        try {
          SiriExtensionWrapper wrapper0 = (SiriExtensionWrapper)arg0.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().getAny();
          SiriExtensionWrapper wrapper1 = (SiriExtensionWrapper)arg1.getMonitoredVehicleJourney().getMonitoredCall().getExtensions().getAny();
          return wrapper0.getDistances().getDistanceFromCall().compareTo(wrapper1.getDistances().getDistanceFromCall());
        } catch(Exception e) {
          return -1;
        }
      }
    });
    
    return output;
  }

  @Override
  public boolean getVehiclesInServiceForRoute(String routeId, String directionId) {
	  ListBean<TripDetailsBean> trips = getAllTripsForRoute(routeId);
	  for(TripDetailsBean tripDetails : trips.getList()) {
		  // filter out interlined routes
		  if(routeId != null && !tripDetails.getTrip().getRoute().getId().equals(routeId))
			  continue;

		  // filtered out by user
		  if(directionId != null && !tripDetails.getTrip().getDirectionId().equals(directionId))
			  continue;

		  if(!_presentationService.include(tripDetails.getStatus()))
			  continue;

		  return true;
	  } 

	  return false;
  }

  @Override
  public boolean getVehiclesInServiceForStopAndRoute(String stopId, String routeId) {
	  for (ArrivalAndDepartureBean adBean : getArrivalsAndDeparturesForStop(stopId)) {
		  TripStatusBean statusBean = adBean.getTripStatus();
		  if(!_presentationService.include(statusBean) || !_presentationService.include(adBean, statusBean))
			  continue;

		  // filtered out by user
		  if(routeId != null && !adBean.getTrip().getRoute().getId().equals(routeId))
			  continue;

		  return true;
	  }

	  return false;
  }
  
  @Override
  public List<ServiceAlertBean> getServiceAlertsForRoute(String routeId) {
    return getServiceAlertsForRouteAndDirection(routeId, null); 
  }
  
  @Override
  public List<ServiceAlertBean> getServiceAlertsForRouteAndDirection(
      String routeId, String directionId) {

    SituationQueryBean query = new SituationQueryBean();
    if (directionId == null) {
      query.addRoute(routeId.toString(), "0");
      query.addRoute(routeId.toString(), "1");
    } else {
      query.addRoute(routeId.toString(), directionId);
    }
    ListBean<ServiceAlertBean> serviceAlerts = _nycTransitDataService.getServiceAlerts(query);

    return serviceAlerts.getList();
  }
  
  private ListBean<TripDetailsBean> getAllTripsForRoute(String routeId) {
    TripsForRouteQueryBean tripRouteQueryBean = new TripsForRouteQueryBean();
    tripRouteQueryBean.setRouteId(routeId);
    tripRouteQueryBean.setTime(getTime());
    
    TripDetailsInclusionBean inclusionBean = new TripDetailsInclusionBean();
    inclusionBean.setIncludeTripBean(true);
    inclusionBean.setIncludeTripStatus(true);
    tripRouteQueryBean.setInclusion(inclusionBean);

    return _nycTransitDataService.getTripsForRoute(tripRouteQueryBean);
  } 
  
  private List<ArrivalAndDepartureBean> getArrivalsAndDeparturesForStop(String stopId) {
    ArrivalsAndDeparturesQueryBean query = new ArrivalsAndDeparturesQueryBean();
    query.setTime(getTime());
    query.setMinutesBefore(5 * 60);
    query.setMinutesAfter(5 * 60);
    
    StopWithArrivalsAndDeparturesBean stopWithArrivalsAndDepartures =
        _nycTransitDataService.getStopWithArrivalsAndDepartures(stopId, query);

    return stopWithArrivalsAndDepartures.getArrivalsAndDepartures();
  }

}