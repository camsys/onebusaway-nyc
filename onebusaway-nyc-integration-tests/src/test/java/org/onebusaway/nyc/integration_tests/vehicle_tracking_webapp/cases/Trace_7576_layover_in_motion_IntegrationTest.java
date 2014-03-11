package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

  public class Trace_7576_layover_in_motion_IntegrationTest extends AbstractTraceRunner {
    public Trace_7576_layover_in_motion_IntegrationTest() throws Exception {
      super("7576-layover-in-motion-labeled.csv");
      setBundle("2013June_Prod_r04_b03", "2013-08-22T10:24:00EDT");

    }
  }


