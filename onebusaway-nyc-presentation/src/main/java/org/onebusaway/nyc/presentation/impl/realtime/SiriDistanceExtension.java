package org.onebusaway.nyc.presentation.impl.realtime;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name="Distance")
public class SiriDistanceExtension {
  
  //FIXME move to config service?
  private static final int atStopThresholdInFeet = 100;

  // FIXME move to config service?
  private static final int arrivingThresholdInFeet = 500;

  // FIXME move to config service?
  private static final int arrivingThresholdInStops = 0;

  // FIXME move to config service?
  private static final int showDistanceInStopsThresholdInStops = 3;
  
  private Integer StopsFromCall = null;

  private Double CallDistanceAlongRoute = null;    

  private Double DistanceFromCall = null;

  public Integer getStopsFromCall() {
    return StopsFromCall;
  }

  public void setStopsFromCall(Integer stopsFromCall) {
    StopsFromCall = stopsFromCall;
  }

  public Double getCallDistanceAlongRoute() {
    return CallDistanceAlongRoute;
  }

  public void setCallDistanceAlongRoute(Double callDistanceAlongRoute) {
    CallDistanceAlongRoute = callDistanceAlongRoute;
  }

  public Double getDistanceFromCall() {
    return DistanceFromCall;
  }

  public void setDistanceFromCall(Double distanceFromCall) {
    DistanceFromCall = distanceFromCall;
  }
  
  public String getPresentableDistance() {
    String r = "";

    // meters->feet
    double feetAway = getDistanceFromCall() * 3.2808399;

    if(feetAway <= atStopThresholdInFeet) {
      r = "at stop";

    } else if(feetAway <= arrivingThresholdInFeet && getStopsFromCall() <= arrivingThresholdInStops) {
      r = "approaching";
    
    } else {
      if(getStopsFromCall() <= showDistanceInStopsThresholdInStops) {
        if(getStopsFromCall() == 0)
          r = "< 1 stop away";
        else
          r = getStopsFromCall() == 1
          ? "1 stop away"
              : getStopsFromCall() + " stops away";
      } else {
        double milesAway = (float)feetAway / 5280;
        r = String.format("%1.1f miles away", milesAway);
      }
    }
    
    return r;
  }
}