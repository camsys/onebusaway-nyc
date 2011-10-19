package org.onebusaway.nyc.presentation.impl.search;

import org.onebusaway.nyc.presentation.model.EnumFormattingContext;
import org.onebusaway.nyc.presentation.model.realtime_data.DistanceAway;
import org.onebusaway.nyc.presentation.service.ArrivalDepartureBeanListFilter;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalsAndDeparturesQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.services.TransitDataService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class SearchSupport {
  
  @Autowired
  private TransitDataService _transitDataService;
  
  @Autowired
  private ConfigurationService _configurationService;
    
  @Autowired
  private ArrivalDepartureBeanListFilter _adBeanListFilter;
    
  private Date _now = null;
  
  public void setTime(Date time) {
    _now = time;
  }
  
  private long getTime() {
    if(_now != null)
      return _now.getTime();
    else
      return System.currentTimeMillis();
  }
  
  public List<DistanceAway> getDistanceAwaysForStopAndHeadsign(StopBean stopBean, String headsign,
      boolean filterToThingsLessThanOneStopAway, EnumFormattingContext formattingContext) {
    
    List<DistanceAway> output = new ArrayList<DistanceAway>();

    List<ArrivalAndDepartureBean> arrivalsAndDepartures = 
        getArrivalsAndDeparturesForStop(stopBean);
    
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
      
      if(filterToThingsLessThanOneStopAway == true && stopsAway >= 1) 
        continue;
      
      DistanceAway distanceAway = new DistanceAway(stopsAway, feetAway, 
          formattingContext, tripStatusBean, _configurationService);
      
      output.add(distanceAway);
    }
    
    return output;
  }
  
  public List<ArrivalAndDepartureBean> getFilteredArrivalsAndDeparturesForStop(StopBean stopBean) {
    List<ArrivalAndDepartureBean> arrivalsAndDepartures = 
        getArrivalsAndDeparturesForStop(stopBean);

    return _adBeanListFilter.filter(arrivalsAndDepartures);
  }
  
  public List<ArrivalAndDepartureBean> getArrivalsAndDeparturesForStop(StopBean stopBean) {
    ArrivalsAndDeparturesQueryBean query = new ArrivalsAndDeparturesQueryBean();
    query.setTime(getTime());
    query.setMinutesBefore(60);
    query.setMinutesAfter(90);
    
    StopWithArrivalsAndDeparturesBean stopWithArrivalsAndDepartures = 
      _transitDataService.getStopWithArrivalsAndDepartures(stopBean.getId(), query);

    return stopWithArrivalsAndDepartures.getArrivalsAndDepartures();
  }
}