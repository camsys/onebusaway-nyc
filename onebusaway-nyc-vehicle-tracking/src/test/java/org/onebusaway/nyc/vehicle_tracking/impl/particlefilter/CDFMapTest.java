package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;
import org.onebusaway.collections.Counter;

public class CDFMapTest {

  @Test
  public void testSampleA() {

    CDFMap<String> cdf = new CDFMap<String>();
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

  @Test
  public void testSamples() {

    CDFMap<String> cdf = new CDFMap<String>();
    cdf.put(0.1, "a");
    cdf.put(0.2, "b");
    cdf.put(0.3, "c");

    List<String> values = cdf.sample(12);
    Counter<String> counter = new Counter<String>();
    for (String value : values)
      counter.increment(value);

    assertEquals(2, counter.getCount("a"));
    assertEquals(4, counter.getCount("b"));
    assertEquals(6, counter.getCount("c"));
  }
}
