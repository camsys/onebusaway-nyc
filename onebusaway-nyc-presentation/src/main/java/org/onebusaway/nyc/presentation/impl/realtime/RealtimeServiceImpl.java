package org.onebusaway.nyc.presentation.impl.realtime;

import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriExtensionWrapper;
import org.onebusaway.nyc.transit_data_federation.siri.SiriJsonSerializer;
import org.onebusaway.nyc.transit_data_federation.siri.SiriXmlSerializer;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalsAndDeparturesQueryBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.VehicleActivityStructure;
import uk.org.siri.siri.VehicleActivityStructure.MonitoredVehicleJourney;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Component
public class RealtimeServiceImpl implements RealtimeService {

  private TransitDataService _transitDataService;

  private PresentationService _presentationService;
  
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
  public void setTransitDataService(TransitDataService transitDataService) {
    _transitDataService = transitDataService;
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
      // filter out interlined routes? sometimes OBA returns vehicles serving routes that are not the same one 
      // we requested! not 100% sure why...
      if(routeId != null && !tripDetails.getTrip().getRoute().getId().equals(routeId))
        continue;

      // filtered out by user
      if(directionId != null && !tripDetails.getTrip().getDirectionId().equals(directionId))
        continue;
      
      if(!_presentationService.include(tripDetails.getStatus()))
        continue;
        
      VehicleActivityStructure activity = new VehicleActivityStructure();
      activity.setRecordedAtTime(new Date(tripDetails.getStatus().getLastUpdateTime()));

      activity.setMonitoredVehicleJourney(new MonitoredVehicleJourney());
      SiriSupport.fillMonitoredVehicleJourney(activity.getMonitoredVehicleJourney(), 
          tripDetails.getTrip(), tripDetails, tripDetails.getStatus().getNextStop(), 
          _presentationService, _transitDataService, getTime(), maximumOnwardCalls);
      
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

    TripDetailsBean tripDetails = _transitDataService.getTripDetailsForVehicleAndTime(query);
    
    if (tripDetails != null) {
      if(!_presentationService.include(tripDetails.getStatus()))
        return null;
      
      VehicleActivityStructure output = new VehicleActivityStructure();
      output.setRecordedAtTime(new Date(tripDetails.getStatus().getLastUpdateTime()));

      output.setMonitoredVehicleJourney(new MonitoredVehicleJourney());
      SiriSupport.fillMonitoredVehicleJourney(output.getMonitoredVehicleJourney(), 
          tripDetails.getTrip(), tripDetails, tripDetails.getStatus().getNextStop(), 
          _presentationService, _transitDataService, getTime(), maximumOnwardCalls);

      return output;
    }
    
    return null;
  }

  @Override
  public List<MonitoredStopVisitStructure> getMonitoredStopVisitsForStop(String stopId, int maximumOnwardCalls) {

    List<MonitoredStopVisitStructure> output = new ArrayList<MonitoredStopVisitStructure>();

    for (ArrivalAndDepartureBean adBean : getArrivalsAndDeparturesForStop(stopId)) {
      TripStatusBean statusBean = adBean.getTripStatus();

      if(!_presentationService.include(statusBean) || !_presentationService.include(adBean, statusBean))
        continue;

      TripDetailsQueryBean query = new TripDetailsQueryBean();
      query.setServiceDate(statusBean.getServiceDate());
      query.setTripId(statusBean.getActiveTrip().getId());
      query.setTime(getTime());
      query.setVehicleId(statusBean.getVehicleId());

      ListBean<TripDetailsBean> tripDetailBeans = _transitDataService.getTripDetails(query);      

      for(TripDetailsBean tripDetails : tripDetailBeans.getList()) {
        // should be the same as the bean above, but just to double check:
        TripStatusBean otherStatusBean = tripDetails.getStatus();

        if(!_presentationService.include(otherStatusBean) || !_presentationService.include(adBean, otherStatusBean))
          continue;
        
        MonitoredStopVisitStructure stopVisit = new MonitoredStopVisitStructure();
        stopVisit.setRecordedAtTime(new Date(statusBean.getLastUpdateTime()));
        
        stopVisit.setMonitoredVehicleJourney(new MonitoredVehicleJourneyStructure());
        SiriSupport.fillMonitoredVehicleJourney(stopVisit.getMonitoredVehicleJourney(), 
            adBean.getTrip(), tripDetails, adBean.getStop(), 
            _presentationService, _transitDataService, getTime(), maximumOnwardCalls);
          
        output.add(stopVisit);
      }
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
  public List<NaturalLanguageStringBean> getServiceAlertsForRoute(String routeId) {
    return getServiceAlertsForRouteAndDirection(routeId, null); 
  }
  
  @Override
  public List<NaturalLanguageStringBean> getServiceAlertsForRouteAndDirection(String routeId,
      String directionId) {

    HashMap<String, List<NaturalLanguageStringBean>> serviceAlertIdsToDescriptions =
        new HashMap<String, List<NaturalLanguageStringBean>>();

    for (TripDetailsBean tripDetailsBean : getAllTripsForRoute(routeId).getList()) {
      TripStatusBean tripStatusBean = tripDetailsBean.getStatus();
      if(tripStatusBean == null || tripStatusBean.getSituations() == null)
        continue;
      
      if(directionId != null && !tripStatusBean.getActiveTrip().getDirectionId().equals(directionId))
        continue;

      for(ServiceAlertBean serviceAlert : tripStatusBean.getSituations()) {
        serviceAlertIdsToDescriptions.put(serviceAlert.getId(), serviceAlert.getDescriptions());
      }
    }
    
    // merge lists from unique service alerts together
    List<NaturalLanguageStringBean> output = new ArrayList<NaturalLanguageStringBean>();
    for(Collection<NaturalLanguageStringBean> descriptions : serviceAlertIdsToDescriptions.values()) {
      output.addAll(descriptions);
    }

    return output;
  }
  
  @Override
  public List<NaturalLanguageStringBean> getServiceAlertsForStop(String stopId) {
    HashMap<String, List<NaturalLanguageStringBean>> serviceAlertIdsToDescriptions =
        new HashMap<String, List<NaturalLanguageStringBean>>();
    
    List<ArrivalAndDepartureBean> arrivalsAndDepartures = getArrivalsAndDeparturesForStop(stopId);
    
    for (ArrivalAndDepartureBean arrivalAndDepartureBean : arrivalsAndDepartures) {
      TripStatusBean tripStatusBean = arrivalAndDepartureBean.getTripStatus();
      if(tripStatusBean == null || tripStatusBean.getSituations() == null)
        continue;

      for(ServiceAlertBean serviceAlert : tripStatusBean.getSituations()) {
        serviceAlertIdsToDescriptions.put(serviceAlert.getId(), serviceAlert.getDescriptions());
      }
    }
    
    // merge lists from unique service alerts together
    List<NaturalLanguageStringBean> output = new ArrayList<NaturalLanguageStringBean>();
    for(Collection<NaturalLanguageStringBean> descriptions : serviceAlertIdsToDescriptions.values()) {
      output.addAll(descriptions);
    }

    return output;
  }
  
  private ListBean<TripDetailsBean> getAllTripsForRoute(String routeId) {
    TripsForRouteQueryBean tripRouteQueryBean = new TripsForRouteQueryBean();
    tripRouteQueryBean.setRouteId(routeId);
    tripRouteQueryBean.setTime(getTime());
    
    TripDetailsInclusionBean inclusionBean = new TripDetailsInclusionBean();
    inclusionBean.setIncludeTripBean(true);
    inclusionBean.setIncludeTripStatus(true);
    tripRouteQueryBean.setInclusion(inclusionBean);

    return _transitDataService.getTripsForRoute(tripRouteQueryBean);
  } 
  
  private List<ArrivalAndDepartureBean> getArrivalsAndDeparturesForStop(String stopId) {
    ArrivalsAndDeparturesQueryBean query = new ArrivalsAndDeparturesQueryBean();
    query.setTime(getTime());
    query.setMinutesBefore(60);
    query.setMinutesAfter(90);
    
    StopWithArrivalsAndDeparturesBean stopWithArrivalsAndDepartures =
      _transitDataService.getStopWithArrivalsAndDepartures(stopId, query);

    return stopWithArrivalsAndDepartures.getArrivalsAndDepartures();
  }

}