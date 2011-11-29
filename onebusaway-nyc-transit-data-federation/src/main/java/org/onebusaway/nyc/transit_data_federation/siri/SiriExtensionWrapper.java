package org.onebusaway.nyc.transit_data_federation.siri;


import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

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