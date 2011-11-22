package org.onebusaway.nyc.presentation.model.realtime;


import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class SiriExtensionWrapper {
  
  private SiriDistanceExtension distances;

  public SiriDistanceExtension getDistances() {
    return distances;
  }

  public void setDistances(SiriDistanceExtension distances) {
    this.distances = distances;
  }

}