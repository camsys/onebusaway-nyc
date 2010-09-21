package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

/**
 * Case: Bus sends DSC for known route A, but is following a known route B.
 * 
 * In this specific trace, there is some amount of gps noise (20m), the bus
 * starts out 5 minutes late and is 12 minutes late by the end of the run, and
 * finally the DSC is mis-labled to be a trip headed in the opposite direction.
 * 
 * @author bdferris
 */
public class WrongRouteDestinationSignCodeIntegrationTest extends
    AbstractTraceRunner {

  public WrongRouteDestinationSignCodeIntegrationTest() {
    super("2008_20100627CC_146000_B62_0013_B62_34-noisy-wrongDsc.csv.gz");
  }
}
