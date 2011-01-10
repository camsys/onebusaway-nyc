package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

import java.util.ArrayList;
import java.util.List;

public class SensorModelResult {

  private String name;

  private double probability = 1.0;

  private List<SensorModelResult> results;

  public SensorModelResult(String name) {
    this(name, 1.0);
  }

  public SensorModelResult(String name, double probability) {
    this.name = name;
    this.probability = probability;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public double getProbability() {
    return probability;
  }

  public void setProbability(double probability) {
    this.probability = probability;
  }

  public List<SensorModelResult> getResults() {
    return results;
  }

  public void setResults(List<SensorModelResult> results) {
    this.results = results;
  }

  public SensorModelResult addResult(String name, double probability) {
    return addResult(new SensorModelResult(name, probability));
  }

  public SensorModelResult addResultAsAnd(String name, double probability) {
    return addResultAsAnd(new SensorModelResult(name, probability));
  }

  public SensorModelResult addResult(SensorModelResult result) {

    this.probability *= result.probability;

    if (results == null)
      results = new ArrayList<SensorModelResult>();
    results.add(result);

    return result;
  }

  public SensorModelResult addResultAsAnd(SensorModelResult result) {
    this.probability *= result.probability;
    return addResult(result);
  }

}
