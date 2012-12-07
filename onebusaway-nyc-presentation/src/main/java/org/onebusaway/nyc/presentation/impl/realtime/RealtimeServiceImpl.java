package org.onebusaway.nyc.presentation.impl.realtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import org.onebusaway.nyc.presentation.impl.realtime.SiriSupport.OnwardCallsMode;
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
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleActivityStructure.MonitoredVehicleJourney;

/**
 * A source of SIRI classes containing real time data, subject to the conventions expressed
 * in the PresentationService.
 * 
 * @author jmaki
 *
 */
@Component
public class RealtimeServiceImpl implements RealtimeService {

  private NycTransitDataService _nycTransitDataService;

  private PresentationService _presentationService;
  
  private SiriXmlSerializer _siriXmlSerializer = new SiriXmlSerializer();

  private SiriJsonSerializer _siriJsonSerializer = new SiriJsonSerializer();

  private Long _now = null;
  
  @Override
  public void setTime(long time) {
    _now = time;
    _presentationService.setTime(time);
  }

  public long getTime() {
    if(_now != null)
      return _now;
    else
      return System.currentTimeMillis();
  }

  @Autowired
  public void setNycTransitDataService(NycTransitDataService transitDataService) {
    _nycTransitDataService = transitDataService;
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

  /**
   * SIRI METHODS
   */
  @Override
  public List<VehicleActivityStructure> getVehicleActivityForRoute(String routeId, String directionId, int maximumOnwardCalls) {
    List<VehicleActivityStructure> output = new ArrayList<VehicleActivityStructure>();
    
    boolean useTimePredictionsIfAvailable =
    		_presentationService.useTimePredictionsIfAvailable();
    
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
      if(useTimePredictionsIfAvailable == true) {
    	  timePredictionRecords = _nycTransitDataService.getPredictionRecordsForTrip(tripDetails.getStatus());
      }
      
      activity.setMonitoredVehicleJourney(new MonitoredVehicleJourney());      
      SiriSupport.fillMonitoredVehicleJourney(activity.getMonitoredVehicleJourney(), 
          tripDetails.getTrip(), tripDetails.getStatus(), null, OnwardCallsMode.VEHICLE_MONITORING,
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

	boolean useTimePredictionsIfAvailable =
	  		_presentationService.useTimePredictionsIfAvailable();
  
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
      if(useTimePredictionsIfAvailable == true) {
    	  timePredictionRecords = _nycTransitDataService.getPredictionRecordsForTrip(tripDetailsForCurrentTrip.getStatus());
      }

      output.setMonitoredVehicleJourney(new MonitoredVehicleJourney());
      SiriSupport.fillMonitoredVehicleJourney(output.getMonitoredVehicleJourney(), 
    	  tripDetailsForCurrentTrip.getTrip(), tripDetailsForCurrentTrip.getStatus(), null, OnwardCallsMode.VEHICLE_MONITORING,
    	  _presentationService, _nycTransitDataService, maximumOnwardCalls,
    	  timePredictionRecords);

      return output;
    }
    
    return null;
  }

  @Override
  public List<MonitoredStopVisitStructure> getMonitoredStopVisitsForStop(String stopId, int maximumOnwardCalls) {
    List<MonitoredStopVisitStructure> output = new ArrayList<MonitoredStopVisitStructure>();

	boolean useTimePredictionsIfAvailable =
	  		_presentationService.useTimePredictionsIfAvailable();

    for (ArrivalAndDepartureBean adBean : getArrivalsAndDeparturesForStop(stopId)) {
      TripStatusBean statusBeanForCurrentTrip = adBean.getTripStatus();
      TripBean tripBeanForAd = adBean.getTrip();

      if(statusBeanForCurrentTrip == null)
    	  continue;

      if(!_presentationService.include(statusBeanForCurrentTrip) || !_presentationService.include(adBean, statusBeanForCurrentTrip))
          continue;

      MonitoredStopVisitStructure stopVisit = new MonitoredStopVisitStructure();
      stopVisit.setRecordedAtTime(new Date(statusBeanForCurrentTrip.getLastUpdateTime()));
        
      List<TimepointPredictionRecord> timePredictionRecords = null;
      if(useTimePredictionsIfAvailable == true) {
    	  timePredictionRecords = _nycTransitDataService.getPredictionRecordsForTrip(statusBeanForCurrentTrip);
      }

      stopVisit.setMonitoredVehicleJourney(new MonitoredVehicleJourneyStructure());
      SiriSupport.fillMonitoredVehicleJourney(stopVisit.getMonitoredVehicleJourney(), 
    	  tripBeanForAd, statusBeanForCurrentTrip, adBean.getStop(), OnwardCallsMode.STOP_MONITORING,
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

  /**
   * CURRENT IN-SERVICE VEHICLE STATUS FOR ROUTE
   */

  /**
   * Returns true if there are vehicles in service for given route+direction
   */
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

  /**
   * Returns true if there are vehicles in service for given route+direction that will stop
   * at the indicated stop in the future.
   */
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
  
  /**
   * SERVICE ALERTS METHODS
   */
  
  @Override
  public List<ServiceAlertBean> getServiceAlertsForRoute(String routeId) {
    return getServiceAlertsForRouteAndDirection(routeId, null); 
  }
  
  @Override
  public List<ServiceAlertBean> getServiceAlertsForRouteAndDirection(
      String routeId, String directionId) {
    SituationQueryBean query = new SituationQueryBean();
    SituationQueryBean.AffectsBean affects = new SituationQueryBean.AffectsBean();
    query.getAffects().add(affects);

    affects.setRouteId(routeId);
    if (directionId != null) {
      affects.setDirectionId(directionId);
    } else {
      /*
       * TODO
       * The route index is not currently being populated correctly; query by route and direction,
       * and supply both directions if not present
       */
      SituationQueryBean.AffectsBean affects1 = new SituationQueryBean.AffectsBean();
      query.getAffects().add(affects1);
      affects1.setRouteId(routeId);
      affects1.setDirectionId("0");
      SituationQueryBean.AffectsBean affects2 = new SituationQueryBean.AffectsBean();
      query.getAffects().add(affects2);
      affects2.setRouteId(routeId);
      affects2.setDirectionId("1");
    }
    
    ListBean<ServiceAlertBean> serviceAlerts = _nycTransitDataService.getServiceAlerts(query);
    return serviceAlerts.getList();
  }
  
  @Override
  public List<ServiceAlertBean> getServiceAlertsGlobal() {
    SituationQueryBean query = new SituationQueryBean();
    SituationQueryBean.AffectsBean affects = new SituationQueryBean.AffectsBean();
    
    affects.setAgencyId("__ALL_OPERATORS__");
    query.getAffects().add(affects);

    ListBean<ServiceAlertBean> serviceAlerts = _nycTransitDataService.getServiceAlerts(query);
    return serviceAlerts.getList();
  }
  
  /**
   * PRIVATE METHODS
   */
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