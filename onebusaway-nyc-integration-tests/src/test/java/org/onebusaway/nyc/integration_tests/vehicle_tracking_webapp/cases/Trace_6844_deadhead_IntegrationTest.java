package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

public class Trace_6844_deadhead_IntegrationTest extends AbstractTraceRunner {

  public Trace_6844_deadhead_IntegrationTest() throws Exception {
    super("6844_should_be_deadhead_during.csv");
    setBundle("2014April_Prod_r06_b02", "2014-04-09T18:10:00EDT");
  }
  
}
