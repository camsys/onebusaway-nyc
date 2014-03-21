package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

public class Trace_3088_in_progress_IntegrationTest extends AbstractTraceRunner {
  public Trace_3088_in_progress_IntegrationTest() throws Exception {
    super("MTABC_3088_should_be_in_progress.csv");
    setBundle("2014Jan_AllCity_r09_b3", "2014-03-10T11:21:00EDT");
  }
}
