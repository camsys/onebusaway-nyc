package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

/**
 * block = 2008_12888481
 * 
 * In this case, we correctly track the vehicle to the end of block # 12888481,
 * terminating at Bay Ridge. The vehicle dead heads back to the base along 4th
 * Avenue, but pauses for ~ 10 minutes at 40.63477,-74.02344 (4th Ave & Bay
 * Ridge Ave approximately) along the way. That pause is long enough to be
 * inferred as LAYOVER_AFTER, indicating a layover after the completion of a
 * block. At that point, we attempt to start a new block for the vehicle, which
 * is why the vehicle is marked as on-route even though it heads back to the
 * base. It's worth noting that the driver never changes his DSC from 4630 after
 * completing the block and dead-heading back.
 * 
 * @author bdferris
 */
public class Trace_7564_20101201T010042_IntegrationTest extends AbstractTraceRunner {

  public Trace_7564_20101201T010042_IntegrationTest() {
    super("7564-2010-12-01T01-00-42.csv.gz");
  }
}
