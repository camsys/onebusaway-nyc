package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

public class Trace_9458_20160706_flips_trip_IntegrationTest extends AbstractTraceRunner {
  public Trace_9458_20160706_flips_trip_IntegrationTest() throws Exception {
    super("9458_flips_trip.csv");
    setBundle("2016April_Prod_r01_b05", "2016-06-02T13:00:00EDT");
  }
}
