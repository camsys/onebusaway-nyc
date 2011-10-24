package org.onebusaway.nyc.presentation.impl.realtime;

import org.onebusaway.nyc.presentation.model.realtime.DistanceAway;
import org.onebusaway.nyc.presentation.service.ArrivalDepartureBeanListFilter;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalsAndDeparturesQueryBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Component
public class RealtimeServiceImpl implements RealtimeService {
  
  @Autowired
  private TransitDataService _transitDataService;
  
  @Autowired
  private ArrivalDepartureBeanListFilter _adBeanListFilter;
  
  private Date _now = null;
  
  @Override
  public void setTime(Date time) {
    _now = time;
  }
  
  private long getTime() {
    if(_now != null)
      return _now.getTime();
    else
      return System.currentTimeMillis();    
  }
  
  @Override
  public List<DistanceAway> getDistanceAwaysForStopAndHeadsign(String stopId, String headsign) {
    
    List<DistanceAway> output = new ArrayList<DistanceAway>();

    List<ArrivalAndDepartureBean> arrivalsAndDepartures = 
        getArrivalsAndDeparturesForStop(stopId);
    
    // since this is data used for display of realtime data only, we use the bean filter
    List<ArrivalAndDepartureBean> filteredArrivalsAndDepartures = 
        _adBeanListFilter.filter(arrivalsAndDepartures);
    
    for (ArrivalAndDepartureBean arrivalAndDepartureBean : filteredArrivalsAndDepartures) {
      TripBean tripBean = arrivalAndDepartureBean.getTrip();
      if(!tripBean.getTripHeadsign().equals(headsign))
        continue;
      
      TripStatusBean tripStatusBean = arrivalAndDepartureBean.getTripStatus();      
      Integer stopsAway = arrivalAndDepartureBean.getNumberOfStopsAway();
      Integer feetAway = (int)(arrivalAndDepartureBean.getDistanceFromStop() * 3.2808399);
      
      DistanceAway distanceAway = new DistanceAway(stopsAway, feetAway, tripStatusBean);      
      output.add(distanceAway);
    }
    
    Collections.sort(output);
    
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
    tripRouteQueryBean.setMaxCount(100);
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
    query.setMinutesBefore(15);
    query.setMinutesAfter(90);
    
    StopWithArrivalsAndDeparturesBean stopWithArrivalsAndDepartures = 
      _transitDataService.getStopWithArrivalsAndDepartures(stopId, query);

    return stopWithArrivalsAndDepartures.getArrivalsAndDepartures();
  }
}