package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

public class Trace_2782_deadhead_nyct_bus_as_mta_IntegrationTest extends AbstractTraceRunner {
  public Trace_2782_deadhead_nyct_bus_as_mta_IntegrationTest() throws Exception {
    super("2782_deadhead_nyct_bus_as_mta.csv");
    setBundle("2014Jan_AllCity_r09_b3", "2014-01-29T09:50:00EDT");
  }
}
