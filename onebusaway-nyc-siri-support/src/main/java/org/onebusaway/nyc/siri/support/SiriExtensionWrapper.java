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
public class SiriExtensionWrapper {
  
  private SiriDistanceExtension distances;

  private SiriApcExtension capacities;

  @XmlElement(name="Distances")
  public SiriDistanceExtension getDistances() {
    return distances;
  }

  public void setDistances(SiriDistanceExtension distances) {
    this.distances = distances;
  }

  @XmlElement(name="Capacities")
  public SiriApcExtension getCapacities() { return capacities; }

  public void setCapacities(SiriApcExtension capacities) { this.capacities = capacities; }

}