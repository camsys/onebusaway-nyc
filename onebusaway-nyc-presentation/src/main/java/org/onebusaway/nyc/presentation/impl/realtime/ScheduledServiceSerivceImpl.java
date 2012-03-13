package org.onebusaway.nyc.presentation.impl.realtime;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.presentation.service.realtime.ScheduledServiceService;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

@Component
public class ScheduledServiceSerivceImpl implements ScheduledServiceService {
  
  private Map<String, ArrayList<String>> _routeIdToDirectionIdsWithServiceMap = new HashMap<String,ArrayList<String>>();
  
  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;
  
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
  public Boolean hasUpcomingScheduledService(RouteBean routeBean, StopGroupBean stopGroup) {
    if(_routeIdToDirectionIdsWithServiceMap.containsKey(routeBean.getId())) {
      ArrayList<String> directionsWithService = _routeIdToDirectionIdsWithServiceMap.get(routeBean.getId());
      
      if(directionsWithService.contains(stopGroup.getId())) {
        return true;
      }
    }
    
    return false;
  }
  
  private void rebuildServiceCache() {
    _routeIdToDirectionIdsWithServiceMap.clear();
    
    for(AgencyWithCoverageBean agency : _transitDataService.getAgenciesWithCoverage()) {
      for(String routeId : _transitDataService.getRouteIdsForAgencyId(agency.getAgency().getId()).getList()) {
        RouteBean routeBean = _transitDataService.getRouteForId(routeId);        

        TripsForRouteQueryBean query = new TripsForRouteQueryBean();
        query.setRouteId(routeBean.getId());
        query.setTime(getTime());
          
        ListBean<TripDetailsBean> trips = _transitDataService.getTripsForRoute(query);

        if(trips == null || trips.getList().isEmpty() == true) {
          continue;
        }
          
        ArrayList<String> directionsWithService = new ArrayList<String>();

        for(TripDetailsBean trip : trips.getList()) {
          // filter out interlined routes
          if(routeBean.getId() != null && !trip.getTrip().getRoute().getId().equals(routeBean.getId()))
            continue;
            
          directionsWithService.add(trip.getTrip().getDirectionId());
        }
        
        _routeIdToDirectionIdsWithServiceMap.put(routeBean.getId(), directionsWithService);
      }
    }
  }
  
  @Refreshable(dependsOn = RefreshableResources.TRANSIT_GRAPH) 
  public void refresh() {
    rebuildServiceCache();    
  }
  
  @PostConstruct
  protected void setup() throws Exception {
    rebuildServiceCache();

    if(_taskScheduler != null) {
      UpdateThread updateThread = new UpdateThread();
      _taskScheduler.schedule(updateThread, updateThread);
    }
  } 
  
  private class UpdateThread extends TimerTask implements Trigger {

    @Override
    public void run() {  
      rebuildServiceCache();
    }
    
    @Override
    public Date nextExecutionTime(TriggerContext arg0) {
      Date lastTime = arg0.lastScheduledExecutionTime();
      if(lastTime == null) {
        lastTime = new Date();
      }

      Calendar calendar = new GregorianCalendar();
      calendar.setTime(lastTime);
      calendar.set(Calendar.MILLISECOND, 0);
      calendar.set(Calendar.SECOND, 0);
      calendar.set(Calendar.MINUTE, 0);
      
      int hour = calendar.get(Calendar.HOUR);
      calendar.set(Calendar.HOUR, hour + 1);
      
      return calendar.getTime();
    }  
  }
  
}
