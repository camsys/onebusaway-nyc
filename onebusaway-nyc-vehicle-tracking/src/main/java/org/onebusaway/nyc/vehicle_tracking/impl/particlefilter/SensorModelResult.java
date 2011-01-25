/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
