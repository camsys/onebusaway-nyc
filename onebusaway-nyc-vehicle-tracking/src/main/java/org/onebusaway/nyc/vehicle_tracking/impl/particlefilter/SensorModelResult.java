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

import com.google.common.base.Objects;

import org.apache.commons.math.util.FastMath;

import java.util.ArrayList;
import java.util.List;

public class SensorModelResult {

  private String name;

  private double logProbability = 0.0;

  private double probability = 1.0;

  private List<SensorModelResult> results;

  private boolean refresh = true;

  public SensorModelResult(String name)
      throws BadProbabilityParticleFilterException {
    this(name, 1.0);
  }

  public SensorModelResult(String name, double probability)
      throws BadProbabilityParticleFilterException {
    if (Double.isNaN(probability) || probability < 0d || probability > 1d)
      throw new BadProbabilityParticleFilterException(
          "invalid weight assignment: weight=" + probability);
    this.name = name;
    this.logProbability = FastMath.log(probability);
    this.probability = probability;
    this.refresh = false;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  /**
   * Returns, and lazily computes, the non-log probability. <br>
   * XXX Note that this "compute-on-demand" approach is not thread-safe.
   * 
   * @return non-log probability
   */
  public double getProbability() {
    if (this.refresh) {
      this.probability = FastMath.exp(logProbability);
      this.refresh = false;
    }
    return this.probability;
  }

  public double getLogProbability() {
    return logProbability;
  }

  public void setProbability(double probability)
      throws BadProbabilityParticleFilterException {
    if (Double.isNaN(probability) || probability < 0d || probability > 1d)
      throw new BadProbabilityParticleFilterException(
          "invalid weight assignment: weight=" + probability);
    this.logProbability = FastMath.log(probability);
    this.probability = probability;
    this.refresh = false;
  }

  public void setLogProbability(double probability)
      throws BadProbabilityParticleFilterException {
    if (Double.isNaN(probability) || probability > 0d)
      throw new BadProbabilityParticleFilterException(
          "invalid weight assignment: weight=" + probability);
    this.logProbability = probability;
    this.refresh = true;
  }

  public List<SensorModelResult> getResults() {
    return results;
  }

  public void setResults(List<SensorModelResult> results) {
    this.results = results;
  }

  public SensorModelResult addResult(String name, double probability)
      throws BadProbabilityParticleFilterException {
    return addResult(new SensorModelResult(name, probability));
  }

  public SensorModelResult addLogResultAsAnd(String name, double logProbability)
      throws BadProbabilityParticleFilterException {
    final SensorModelResult newResult = new SensorModelResult(name);
    newResult.setLogProbability(logProbability);
    return addResultAsAnd(newResult);
  }

  public SensorModelResult addResultAsAnd(String name, double probability)
      throws BadProbabilityParticleFilterException {
    return addResultAsAnd(new SensorModelResult(name, probability));
  }

  public SensorModelResult addResult(SensorModelResult result) {
    if (results == null)
      results = new ArrayList<SensorModelResult>();
    results.add(result);

    return result;
  }

  public SensorModelResult addResultAsAnd(SensorModelResult result) {
    this.logProbability += result.logProbability;
    this.refresh = true;
    return addResult(result);
  }

  @Override
  public String toString() {
    final Objects.ToStringHelper toStringHelper = Objects.toStringHelper("SensorModelResult");
    toStringHelper.add(name, getProbability());
    if (results != null) {
      for (final SensorModelResult res : results) {
        toStringHelper.addValue("\n\t" + res.toString());
      }
    }
    return toStringHelper.toString();
  }

}
