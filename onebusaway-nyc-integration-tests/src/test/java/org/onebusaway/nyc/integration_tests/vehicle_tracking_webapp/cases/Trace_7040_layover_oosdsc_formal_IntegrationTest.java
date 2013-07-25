package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

/**
 * Trace of a layover with an out-of-service DSC, with formal inference.  This should be
 * inferred as LAYOVER, and pick up IN_PROGRESS once the DSC is changed (or soon after).
 *
 */
public class Trace_7040_layover_oosdsc_formal_IntegrationTest extends AbstractTraceRunner {

  public Trace_7040_layover_oosdsc_formal_IntegrationTest() throws Exception {
    super("7040-layover-oosdsc-formal-labeled.csv");
    setBundle("2013June_Prod_r03_b02", "2013-07-15T19:40:00EDT");
  }
}
