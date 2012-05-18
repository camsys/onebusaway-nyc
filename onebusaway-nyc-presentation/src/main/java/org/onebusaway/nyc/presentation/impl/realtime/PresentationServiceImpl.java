package org.onebusaway.nyc.presentation.impl.realtime;

import org.onebusaway.nyc.presentation.service.realtime.PresentationService;

import org.onebusaway.nyc.transit_data_federation.siri.SiriDistanceExtension;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class PresentationServiceImpl implements PresentationService {
  
  private static Logger _log = LoggerFactory.getLogger(PresentationServiceImpl.class);

  @Autowired
  private ConfigurationService _configurationService;

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
  public Boolean isInLayover(TripStatusBean statusBean) {
    if(statusBean != null) {
      String phase = statusBean.getPhase();

      if (phase != null &&
          (phase.toUpperCase().equals("LAYOVER_DURING") || phase.toUpperCase().equals("LAYOVER_BEFORE"))) {
        return true;
      } else
        return false;
    }

    return null;
  }

  @Override
  public Boolean isBlockLevelInference(TripStatusBean statusBean) {
    if(statusBean != null) {
      String status = statusBean.getStatus();

      if(status != null)
        return status.contains("blockLevelInference");
      else
        return false;
    }

    return null;
  }
  
  @Override
  public Boolean isOnDetour(TripStatusBean statusBean) {
    if(statusBean != null) {
      String status = statusBean.getStatus();

      if(status != null)
        return status.contains("deviated");
      else
        return false;
    }

    return null;
  }
  
  @Override
  public String getPresentableDistance(SiriDistanceExtension distances) {
    return getPresentableDistance(distances, "approaching", "stop", "stops", "mile", "miles", "away");
  }
  
  @Override
  public String getPresentableDistance(SiriDistanceExtension distances, String approachingText, 
      String oneStopWord, String multipleStopsWord, String oneMileWord, String multipleMilesWord, String awayWord) {

    int atStopThresholdInFeet = 
        _configurationService.getConfigurationValueAsInteger("display.atStopThresholdInFeet", 100);    

    int approachingThresholdInFeet = 
        _configurationService.getConfigurationValueAsInteger("display.approachingThresholdInFeet", 500);    
    
    int distanceAsStopsThresholdInFeet = 
        _configurationService.getConfigurationValueAsInteger("display.distanceAsStopsTresholdInFeet", 2640);    
    
    int distanceAsStopsThresholdInStops = 
        _configurationService.getConfigurationValueAsInteger("display.distanceAsStopsThresholdInStops", 3);    

    int distanceAsStopsMaximumThresholdInFeet = 
        _configurationService.getConfigurationValueAsInteger("display.distanceAsStopsMaximumThresholdInFeet", 2640);    
    
    String r = "";

    // meters->feet
    double feetAway = distances.getDistanceFromCall() * 3.2808399;

    if(feetAway < atStopThresholdInFeet) {
      r = "at " + oneStopWord;

    } else if(feetAway < approachingThresholdInFeet) {
      r = approachingText;
    
    } else {
      if(feetAway <= distanceAsStopsMaximumThresholdInFeet && 
          (distances.getStopsFromCall() <= distanceAsStopsThresholdInStops 
          || feetAway <= distanceAsStopsThresholdInFeet)) {
        
        if(distances.getStopsFromCall() == 0)
          r = "< 1 " + oneStopWord + " " + awayWord;
        else
          r = distances.getStopsFromCall() == 1
          ? "1 " + oneStopWord + " " + awayWord
              : distances.getStopsFromCall() + " " + multipleStopsWord + " " + awayWord;

      } else {
        double milesAway = (float)feetAway / 5280;
        r = String.format("%1.1f " + multipleMilesWord + " " + awayWord, milesAway);
      }
    }
    
    return r;
  }
  
  /***
   * These rules are common to vehicles coming to both SM and VM calls. 
   */
  @Override
  public boolean include(TripStatusBean statusBean) {
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
    
    // old data that should be hidden
    int expiredTimeout = 
        _configurationService.getConfigurationValueAsInteger("display.hideTimeout", 300);    

    if (getTime() - statusBean.getLastUpdateTime() >= 1000 * expiredTimeout) {
      _log.debug("  " + statusBean.getVehicleId() + " filtered out because data is expired.");
      return false;
    }

    return true;
  }
  
  /***
   * These rules are just for SM calls. 
   */
  @Override
  public boolean include(ArrivalAndDepartureBean adBean, TripStatusBean status) {
    if(adBean == null || status == null)
      return false;
    
    // hide buses that left the stop recently
    if(adBean.getDistanceFromStop() < 0)
      return false;
    
    // hide buses that are on detour from a-d queries
    if(isOnDetour(status))
      return false;

    // wrap-around logic
    String phase = status.getPhase();
    TripBean activeTrip = status.getActiveTrip();
    TripBean tripBean = adBean.getTrip();

    if(isBlockLevelInference(status)) {
    	// if ad is not on the trip this bus is, or the previous trip, filter out
    	if(!adBean.getTrip().getId().equals(status.getActiveTrip().getId()) 
    			&& !(adBean.getBlockTripSequence() - 1 == status.getBlockTripSequence())) {
		  _log.debug("  " + status.getVehicleId() + " filtered out due to trip block sequence");
		  return false;
		}
    } else {
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

    	// only consider buses that are in layover
	    if(phase != null && 
	        (phase.toUpperCase().equals("LAYOVER_BEFORE") || phase.toUpperCase().equals("LAYOVER_DURING"))) {
	
	      double distanceAlongTrip = status.getDistanceAlongTrip();
	      double totalDistanceAlongTrip = status.getTotalDistanceAlongTrip();
	      double ratio = distanceAlongTrip / totalDistanceAlongTrip;
	      
	      // if the bus isn't serving the trip this arrival and departure is for AND 
	      // the bus is NOT on the previous trip in the block, but at the end of that trip (ready to serve
	      // the trip this arrival and departure is for), filter that out.
	      if(activeTrip != null
	            && !tripBean.getId().equals(activeTrip.getId())
	            && !((adBean.getBlockTripSequence() - 1) == status.getBlockTripSequence() && ratio > 0.50)) {
	        _log.debug("  " + status.getVehicleId() + " filtered out due to at terminal/ratio");
	        return false;
	      }
	    } else {
	      // if the bus isn't serving the trip this arrival and departure is for, filter out--
	      // since the bus is not in layover now.
	      if (activeTrip != null
	          && !tripBean.getId().equals(activeTrip.getId())) {
	        _log.debug("  " + status.getVehicleId() + " filtered out due to not serving trip for A/D");
	        return false;
	      }
	    }
    }
    
    return true;
  }

}
