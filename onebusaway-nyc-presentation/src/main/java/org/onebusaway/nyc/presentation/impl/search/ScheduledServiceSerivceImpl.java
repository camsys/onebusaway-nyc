package org.onebusaway.nyc.presentation.impl.search;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

@Component
public class ScheduledServiceSerivceImpl implements ScheduledServiceService {
  
  private static Logger _log = LoggerFactory.getLogger(ScheduledServiceSerivceImpl.class);

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
    if(_routeIdToDirectionIdsWithServiceMap.isEmpty()) {
      return null;
    }
    
    if(_routeIdToDirectionIdsWithServiceMap.containsKey(routeBean.getId())) {
      ArrayList<String> directionsWithService = _routeIdToDirectionIdsWithServiceMap.get(routeBean.getId());
      
      if(directionsWithService.contains(stopGroup.getId())) {
        return true;
      }
    }
    
    return false;
  }
  
  // FIXME: there's got to be a better way to do this--move to TDF for access to the graphdao? 
  private void rebuildServiceCache() {
    _log.info("Rebuilding service cache for scheduled service service...");
    
    _routeIdToDirectionIdsWithServiceMap.clear();
    
    for(AgencyWithCoverageBean agency : _transitDataService.getAgenciesWithCoverage()) {
      List<String> routeIds = _transitDataService.getRouteIdsForAgencyId(agency.getAgency().getId()).getList();

      int i = 0;
      for(String routeId : routeIds) {
        TripsForRouteQueryBean query = new TripsForRouteQueryBean();
        query.setRouteId(routeId);
        query.setTime(getTime());
        
        _log.info("Getting trips for " + routeId + "(" + i + "/" + routeIds.size() + ")");
        ListBean<TripDetailsBean> trips = _transitDataService.getTripsForRoute(query);
        
        if(trips == null || trips.getList().isEmpty() == true) {
          continue;
        }
          
        ArrayList<String> directionsWithService = new ArrayList<String>();

        for(TripDetailsBean trip : trips.getList()) {
          // filter out interlined routes
          if(routeId != null && !trip.getTrip().getRoute().getId().equals(routeId))
            continue;
            
          directionsWithService.add(trip.getTrip().getDirectionId());
        }
        
        _routeIdToDirectionIdsWithServiceMap.put(routeId, directionsWithService);
        i++;
      }
    }
  }
  
  @PostConstruct
  protected void setup() throws Exception {
    Thread bootstrapThread = new BootstrapThread();
    bootstrapThread.start();

    if(_taskScheduler != null) {
      UpdateThread updateThread = new UpdateThread();
      _taskScheduler.schedule(updateThread, updateThread);
    }
  } 
  
  // a thread that can block for network IO while tomcat starts
  private class BootstrapThread extends Thread {

    @Override
    public void run() {     
      rebuildServiceCache();
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
