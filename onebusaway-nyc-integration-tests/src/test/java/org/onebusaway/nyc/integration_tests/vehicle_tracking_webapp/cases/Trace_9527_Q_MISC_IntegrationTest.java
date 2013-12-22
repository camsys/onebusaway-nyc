package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

public class Trace_9527_Q_MISC_IntegrationTest extends AbstractTraceRunner{

  public Trace_9527_Q_MISC_IntegrationTest() throws Exception {
    super("9527_Q_MISC.csv");
    setBundle("2013Sept_AllCity_r15_b02", "2013-11-26T00:00:00EDT");
  }
}
