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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;
import org.junit.Test;
import org.onebusaway.collections.Counter;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions.CategoricalDist;

import umontreal.iro.lecuyer.probdist.ChiDist;
import umontreal.iro.lecuyer.probdist.ChiSquareDist;

public class CategoricalDistTest {

  
  @Test
  public void testSampleA() {

    CategoricalDist<String> cdf = new CategoricalDist<String>();
    cdf.put(0.1, "a");
    cdf.put(0.2, "b");
    cdf.put(0.3, "c");

    Counter<String> counter = new Counter<String>();
    double iterations = 1000;

    for (int i = 0; i < iterations; i++)
      counter.increment(cdf.sample());

    double a = counter.getCount("a") / iterations;
    double b = counter.getCount("b") / iterations;
    double c = counter.getCount("c") / iterations;

    assertEquals(a, 0.1 / 0.6, .05);
    assertEquals(b, 0.2 / 0.6, .05);
    assertEquals(c, 0.3 / 0.6, .05);
  }

/*  
  
  private final int TRIES = 10000000;
  
  @Test
  public void testSamples() {

    CDFMap<String> cdf = new CDFMap<String>();
    cdf.put(0.1, "a");
    cdf.put(0.2, "b");
    cdf.put(0.3, "c");

    List<String> values = cdf.sample(TRIES);
    Counter<String> counter = new Counter<String>();
    for (String value : values)
      counter.increment(value);

    double expectedA = TRIES*0.1/0.6;
    double expectedB = TRIES*0.2/0.6;
    double expectedC = TRIES*0.3/0.6;
    
    double chi2 = Math.pow((double)counter.getCount("a") - expectedA, 2)/expectedA;
    chi2 += Math.pow((double)counter.getCount("b") - expectedB, 2)/expectedB;
    chi2 += Math.pow((double)counter.getCount("c") - expectedC, 2)/expectedC;
    
    double testStat = ChiSquareDist.barF(2, 6, chi2);
    assertTrue(testStat > 0.05);
  }*/
}
