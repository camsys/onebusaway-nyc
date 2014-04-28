package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;
/**
 * Hide this bus when u-turning/parked off route, or properly show as layover 
 */
public class Trace_4399_20131225_IntegrationTest extends AbstractTraceRunner {
  public Trace_4399_20131225_IntegrationTest() throws Exception {
    super("4399_deadhead_layover.csv");
    setBundle("2013Sept_Prod_r08_b04", "2013-12-25T11:53:00EDT");
    
  }
}
