package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

public class Trace_4855_export_trips_IntegrationTest extends AbstractTraceRunner {
  
  public Trace_4855_export_trips_IntegrationTest() throws Exception {
    super("4855-export-trips-should-increment.csv");
    setBundle("2014April_Prod_r06_b02", "2014-04-07T6:51:00EDT");
  }
}
