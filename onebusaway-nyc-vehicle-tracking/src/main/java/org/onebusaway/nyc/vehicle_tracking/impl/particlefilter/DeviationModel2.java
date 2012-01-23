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

import cern.jet.random.Normal;
import cern.jet.random.engine.RandomEngine;

public class DeviationModel2 implements ProbabilityFunction {

  private Normal _normal;

  private double _sigma;

  private double _offset;

  public DeviationModel2(double sigma) {
    this(sigma, 2.0);
  }

  public DeviationModel2(double sigma, double offset) {
    _normal = new Normal(0, 1.0, RandomEngine.makeDefault());
    _sigma = sigma;
    _offset = offset;
  }

  @Override
  public double probability(double deviation) {
    return _normal.cdf(_offset * (1 - deviation / _sigma));
  }
}
