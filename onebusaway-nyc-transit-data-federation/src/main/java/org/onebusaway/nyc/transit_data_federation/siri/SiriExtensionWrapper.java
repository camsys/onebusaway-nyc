package org.onebusaway.nyc.transit_data_federation.siri;


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

  @XmlElement(name="Distances")
  public SiriDistanceExtension getDistances() {
    return distances;
  }

  public void setDistances(SiriDistanceExtension distances) {
    this.distances = distances;
  }

}