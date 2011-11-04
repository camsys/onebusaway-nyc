package org.onebusaway.nyc.presentation.impl.realtime;

import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalsAndDeparturesQueryBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.org.siri.siri.MonitoredStopVisitStructure;
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

  private static Logger _log = LoggerFactory.getLogger(RealtimeServiceImpl.class);

  @Autowired
  private ConfigurationService _configurationService;

  @Autowired
  private TransitDataService _transitDataService;

  private Date _now = null;
  
  @Override
  public void setTime(Date time) {
    _now = time;
  }

  public long getTime() {
    if(_now != null)
      return _now.getTime();
    else
      return System.currentTimeMillis();
  }
  
  @Override
  public List<VehicleActivityStructure> getVehicleActivityForRoute(String routeId, String directionId, boolean includeNextStops) {
    List<VehicleActivityStructure> output = new ArrayList<VehicleActivityStructure>();

    ListBean<TripDetailsBean> trips = getAllTripsForRoute(routeId);

    for(TripDetailsBean tripDetails : trips.getList()) {
      // filtered out by user
      if(directionId != null && !tripDetails.getTrip().getDirectionId().equals(directionId)) {
        continue;
      }
      
      if(!include(tripDetails.getStatus()))
        continue;
        
      VehicleActivityStructure activity = new VehicleActivityStructure();
      activity.setRecordedAtTime(new Date(tripDetails.getStatus().getLastUpdateTime()));

      MonitoredVehicleJourney journey = SiriUtils.getMonitoredVehicleJourney(tripDetails, 
          tripDetails.getStatus().getNextStop(), includeNextStops);
      
      if(journey == null)
        continue;
      
      activity.setMonitoredVehicleJourney(journey);

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
  public VehicleActivityStructure getVehicleActivityForVehicle(String vehicleId, boolean includeNextStops) {    

    TripForVehicleQueryBean query = new TripForVehicleQueryBean();
    query.setTime(new Date(getTime()));
    query.setVehicleId(vehicleId);

    TripDetailsInclusionBean inclusion = new TripDetailsInclusionBean();
    inclusion.setIncludeTripStatus(true);
    inclusion.setIncludeTripBean(true);
    query.setInclusion(inclusion);

    TripDetailsBean tripDetails = _transitDataService.getTripDetailsForVehicleAndTime(query);
    
    if (tripDetails != null) {
      if(!include(tripDetails.getStatus()))
        return null;
      
      VehicleActivityStructure output = new VehicleActivityStructure();
      output.setRecordedAtTime(new Date(tripDetails.getStatus().getLastUpdateTime()));

      MonitoredVehicleJourney journey = SiriUtils.getMonitoredVehicleJourney(tripDetails, 
          tripDetails.getStatus().getNextStop(), includeNextStops);
      
      if(journey == null)
        return null;

      output.setMonitoredVehicleJourney(journey);
        
      return output;
    }
    
    return null;
  }

  @Override
  public List<MonitoredStopVisitStructure> getMonitoredStopVisitsForStop(String stopId, boolean includeNextStops) {

    List<MonitoredStopVisitStructure> output = new ArrayList<MonitoredStopVisitStructure>();

    for (ArrivalAndDepartureBean adBean : getArrivalsAndDeparturesForStop(stopId)) {
      TripStatusBean statusBean = adBean.getTripStatus();

      if(!include(statusBean) || !include(adBean, statusBean))
        continue;

      TripDetailsQueryBean query = new TripDetailsQueryBean();
      query.setServiceDate(statusBean.getServiceDate());
      query.setTripId(statusBean.getActiveTrip().getId());
      query.setTime(getTime());
      query.setVehicleId(statusBean.getVehicleId());
      
      ListBean<TripDetailsBean> tripDetailBeans = _transitDataService.getTripDetails(query);      

      for(TripDetailsBean tripDetails : tripDetailBeans.getList()) {
        // the filter below is technically not necessary, but just to double-check...
        TripStatusBean otherStatusBean = tripDetails.getStatus();

        if(!include(otherStatusBean) || !include(adBean, otherStatusBean))
          continue;
        
        MonitoredStopVisitStructure stopVisit = new MonitoredStopVisitStructure();
        stopVisit.setRecordedAtTime(new Date(statusBean.getLastUpdateTime()));
        
        MonitoredVehicleJourney journey = SiriUtils.getMonitoredVehicleJourney(tripDetails, 
            adBean.getStop(), includeNextStops);

        if(journey == null)
          continue;
        
        stopVisit.setMonitoredVehicleJourney(journey);

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
  public List<NaturalLanguageStringBean> getServiceAlertsForRouteAndDirection(String routeId,
      String directionId) {

    HashMap<String, List<NaturalLanguageStringBean>> serviceAlertIdsToDescriptions =
        new HashMap<String, List<NaturalLanguageStringBean>>();

    for (TripDetailsBean tripDetailsBean : getAllTripsForRoute(routeId).getList()) {
      TripStatusBean tripStatusBean = tripDetailsBean.getStatus();
      if(tripStatusBean == null || tripStatusBean.getSituations() == null
          || !tripStatusBean.getActiveTrip().getDirectionId().equals(directionId))
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
  
  private boolean include(TripStatusBean statusBean) {
    if(statusBean == null)
      return false;

    _log.debug(statusBean.getVehicleId() + " running through filter: ");
    
    // hide non-realtime
    if(statusBean.isPredicted() == false) {
      _log.debug("  " + statusBean.getVehicleId() + " filtered out because is not realtime.");
      return false;
    }

    if(statusBean.getVehicleId() == null || statusBean.getPhase() == null) {
      _log.debug("  " + statusBean.getVehicleId() + " filtered out because phase or vehicle id is null.");
      return false;
    }

    if(Double.isNaN(statusBean.getDistanceAlongTrip())) {
      _log.debug("  " + statusBean.getVehicleId() + " filtered out because D.A.T. is NaN.");
      return false;
    }
    
    // not in-service
    String phase = statusBean.getPhase();
    if(phase != null 
        && !phase.toUpperCase().equals("IN_PROGRESS")
        && !phase.toUpperCase().equals("LAYOVER_BEFORE") 
        && !phase.toUpperCase().equals("LAYOVER_DURING")) {
      _log.debug("  " + statusBean.getVehicleId() + " filtered out because phase is not in progress.");      
      return false;
    }

    // disabled
    String status = statusBean.getStatus();
    if(status != null && status.toUpperCase().equals("DISABLED")) {
      _log.debug("  " + statusBean.getVehicleId() + " filtered out because it is disabled.");
      return false;
    }
    
    // old data
    int hideTimeout = 
        _configurationService.getConfigurationValueAsInteger("display.hideTimeout", 300);    

    if (getTime() - statusBean.getLastUpdateTime() >= 1000 * hideTimeout) {
      _log.debug("  " + statusBean.getVehicleId() + " filtered out because data is stale.");
      return false;
    }

    return true;
  }
  
  private boolean include(ArrivalAndDepartureBean adBean, TripStatusBean status) {
    if(adBean == null)
      return false;
    
    // hide buses that left the stop recently
    if(adBean.getDistanceFromStop() < 0)
      return false;
    else
      if(adBean.getDistanceFromStop() > status.getTotalDistanceAlongTrip())
        return false;

    /**
     * So this complicated thing-a-ma-jig is to filter out buses that are at the terminals
     * when considering arrivals and departures for a stop.
     * 
     * The idea is that we label all buses that are in layover "at terminal" headed towards us, then filter 
     * out ones where that isn't true. The ones we need to specifically filter out are the ones that
     * are at the other end of the route--the other terminal--waiting to begin service on the trip
     * we're actually interested in.
     * 
     * Consider a route with terminals A and B:
     * A ----> B 
     *   <----
     *   
     * If we request arrivals and departures for the first trip from B to A, we'll see buses within a block
     * that might be at A waiting to go to B (trip 2), if the vehicle's block includes a trip from B->A later on. 
     * Those are the buses we want to filter out here.  
     */
    String phase = status.getPhase();
    TripBean activeTrip = status.getActiveTrip();
    TripBean tripBean = adBean.getTrip();

    // only consider buses that are in layover
    if(phase != null && 
        (phase.toUpperCase().equals("LAYOVER_BEFORE") 
         || phase.toUpperCase().equals("LAYOVER_DURING"))) {

      double distanceAlongTrip = status.getDistanceAlongTrip();
      double totalDistanceAlongTrip = status.getTotalDistanceAlongTrip();
      double ratio = distanceAlongTrip / totalDistanceAlongTrip;
      
      // if the bus isn't serving the trip this arrival and departure is for AND 
      // the bus is NOT on the previous trip in the block, but at the end of that trip (ready to serve
      // the trip this arrival and departure is for), filter that out.
      if(activeTrip != null
            && !tripBean.getId().equals(activeTrip.getId())
            && ((adBean.getBlockTripSequence() - 1) != status.getBlockTripSequence() && ratio > 0.50)) {
        _log.debug("  " + status.getVehicleId() + " filtered out due to at terminal/ratio");
        return false;
      }
    } else {
      // if the bus isn't serving the trip this arrival and departure is for, filter out.
      if (activeTrip != null
          && !tripBean.getId().equals(activeTrip.getId())) {
        _log.debug("  " + status.getVehicleId() + " filtered out due to not serving trip for A/D");
        return false;
      }
    }

    return true;
  }
  
}