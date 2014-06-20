package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

public class Trace_4655_mtabus_as_nyct_IntegrationTest extends
    AbstractTraceRunner {
  public Trace_4655_mtabus_as_nyct_IntegrationTest() throws Exception {
    super("4655_mtabus_as_nyct.csv");
    setBundle("2014April_Prod_r12_b01", "2014-06-02T5:10:00EDT");
  }
}
