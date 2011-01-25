package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

/**
 * Block = 2008_12888406
 * 
 * Driver left the depot around 4:50 AM, headed for Bay Ridge to start block #
 * 12888406 at 5:05 AM. Leaving depot, the DSC is 12, but the driver never
 * changes the DSC at the start of the trip. OBA tracks the entire trip as
 * "DEADHEAD_BEFORE" because we haven't seen a DSC to indicate which trip we
 * might be on yet. When the driver reaches Cobble Hill, he switches on the
 * proper DSC at 6:25 (6431), and we attempt to assign a block. However, we
 * think he's at the start of a block (as opposed to one trip into an existing
 * block), so we attempt to find a block that starts at Cobble Hill (as opposed
 * to using the already in-progress block). The filter fails to find a block
 * with enough likelihood, so it keeps it DEADHEAD_BEFORE.
 * 
 * 
 * @author bdferris
 */
public class Trace_7564_20101203T004633_IntegrationTest extends AbstractTraceRunner {

  public Trace_7564_20101203T004633_IntegrationTest() {
    super("7564-2010-12-03T00-46-33.csv.gz");
  }
}
