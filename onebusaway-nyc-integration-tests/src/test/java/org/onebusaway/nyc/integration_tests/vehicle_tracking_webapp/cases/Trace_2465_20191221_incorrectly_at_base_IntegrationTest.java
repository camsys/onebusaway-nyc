package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

public class Trace_2465_20191221_incorrectly_at_base_IntegrationTest extends AbstractTraceRunner {
  public Trace_2465_20191221_incorrectly_at_base_IntegrationTest() throws Exception {
    super("2465-2019-12-21.csv");
    setBundle("2019Sept_Prod_Brooklyn_r16_b07", "2019-12-21T14:25:00EDT");
  }
}
