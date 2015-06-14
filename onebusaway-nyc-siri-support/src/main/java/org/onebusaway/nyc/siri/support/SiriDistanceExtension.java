package org.onebusaway.nyc.siri.support;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * A special addition to the XSD-generated SIRI classes to encapsulate
 * the MTA-specific distance-based formulations of arrivals.
 * 
 * These have been submitted as extensions to the official SIRI spec. 
 * 
 * @author jmaki
 *
 */
@XmlRootElement
public class SiriDistanceExtension {

  private Integer StopsFromCall = null;

  private Double CallDistanceAlongRoute = null;    

  private Double DistanceFromCall = null;

  private String PresentableDistance = null;
  
  @XmlElement(name="StopsFromCall")
  public Integer getStopsFromCall() {
    return StopsFromCall;
  }

  public void setStopsFromCall(Integer stopsFromCall) {
    StopsFromCall = stopsFromCall;
  }

  @XmlElement(name="CallDistanceAlongRoute")
  public Double getCallDistanceAlongRoute() {
    return CallDistanceAlongRoute;
  }

  public void setCallDistanceAlongRoute(Double callDistanceAlongRoute) {
    CallDistanceAlongRoute = callDistanceAlongRoute;
  }

  @XmlElement(name="DistanceFromCall")
  public Double getDistanceFromCall() {
    return DistanceFromCall;
  }

  public void setDistanceFromCall(Double distanceFromCall) {
    DistanceFromCall = distanceFromCall;
  }

  @XmlElement(name="PresentableDistance")
  public String getPresentableDistance() {
    return PresentableDistance;
  }

  public void setPresentableDistance(String presentableDistance) {
    PresentableDistance = presentableDistance;
  }

}