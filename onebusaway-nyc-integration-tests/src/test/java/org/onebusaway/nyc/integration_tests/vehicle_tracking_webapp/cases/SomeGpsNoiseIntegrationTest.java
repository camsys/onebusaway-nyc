package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

/**
 * A simple test case with some gps noice (20m).
 * 
 * @author bdferris
 */
public class SomeGpsNoiseIntegrationTest extends AbstractTraceRunner {

  public SomeGpsNoiseIntegrationTest() {
    super("2008_20100627CA_145000_B62_0014_B62_32-noisy.csv.gz");
  }
}
