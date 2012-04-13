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

import org.onebusaway.collections.Counter;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions.CategoricalDist;

import com.google.common.collect.Multiset;

import org.apache.commons.math.util.FastMath;
import org.junit.Test;

public class CategoricalDistTest {

  @Test
  public void testSampleA() {

    final CategoricalDist<String> cdf = new CategoricalDist<String>();
    cdf.logPut(FastMath.log(0.3*1e-5), "c");
    cdf.logPut(FastMath.log(0.3), "c");
    cdf.logPut(FastMath.log(0.2), "c");
    cdf.logPut(FastMath.log(0.01), "a");
    cdf.logPut(FastMath.log(0.001), "a");
    cdf.logPut(FastMath.log(0.2*1e-7), "b");

    final Counter<String> counter = new Counter<String>();
    final int iterations = 1000;

    for (int i = 0; i < iterations; i++)
      counter.increment(cdf.sample());

    final double a = counter.getCount("a") / (double)iterations;
    final double b = counter.getCount("b") / (double)iterations;
    final double c = counter.getCount("c") / (double)iterations;

    final double cummulativeProb = cdf.getCummulativeProb();
    assertEquals(a, cdf.density("a") / cummulativeProb, .05);
    assertEquals(b, cdf.density("b") / cummulativeProb, .05);
    assertEquals(c, cdf.density("c") / cummulativeProb, .05);
    
    cdf.logPut(FastMath.log(0.001), "d");
    
    Multiset<String> res = cdf.sample(iterations);
    assertEquals(res.count("a") / (double) iterations, cdf.density("a") / cummulativeProb, .05);
    assertEquals(res.count("b") / (double) iterations, cdf.density("b") / cummulativeProb, .05);
    assertEquals(res.count("c") / (double) iterations, cdf.density("c") / cummulativeProb, .05);
    assertEquals(res.count("d") / (double) iterations, cdf.density("d") / cummulativeProb, .05);
    
    
  }

  /*
   * 
   * private final int TRIES = 10000000;
   * 
   * @Test public void testSamples() {
   * 
   * CDFMap<String> cdf = new CDFMap<String>(); cdf.put(0.1, "a"); cdf.put(0.2,
   * "b"); cdf.put(0.3, "c");
   * 
   * List<String> values = cdf.sample(TRIES); Counter<String> counter = new
   * Counter<String>(); for (String value : values) counter.increment(value);
   * 
   * double expectedA = TRIES*0.1/0.6; double expectedB = TRIES*0.2/0.6; double
   * expectedC = TRIES*0.3/0.6;
   * 
   * double chi2 = Math.pow((double)counter.getCount("a") - expectedA,
   * 2)/expectedA; chi2 += Math.pow((double)counter.getCount("b") - expectedB,
   * 2)/expectedB; chi2 += Math.pow((double)counter.getCount("c") - expectedC,
   * 2)/expectedC;
   * 
   * double testStat = ChiSquareDist.barF(2, 6, chi2); assertTrue(testStat >
   * 0.05); }
   */
}
