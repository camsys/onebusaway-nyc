package org.onebusaway.nyc.presentation.impl.realtime.siri.model;

public class SiriDistanceExtension {

  private Integer StopsFromCall = null;

  private Double CallDistanceAlongRoute = null;    

  private Double DistanceFromCall = null;

  private String PresentableDistance = null;
  
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
    return PresentableDistance;
  }

  public void setPresentableDistance(String presentableDistance) {
    PresentableDistance = presentableDistance;
  }

}