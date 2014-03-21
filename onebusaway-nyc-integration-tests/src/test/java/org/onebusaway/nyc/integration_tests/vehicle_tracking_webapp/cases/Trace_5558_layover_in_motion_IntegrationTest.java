package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;
/**
 * Hide this bus when u-turning/parked off route, or properly show as layover 
 */
public class Trace_5558_layover_in_motion_IntegrationTest extends AbstractTraceRunner {
  public Trace_5558_layover_in_motion_IntegrationTest() throws Exception {
    super("5558-layover-in-motion-labeled.csv");
    setBundle("2013June_Prod_r04_b03", "2013-08-28T15:11:00EDT");

  }
}
