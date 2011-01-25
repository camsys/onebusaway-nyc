package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

/**
 * 
 * Block # 12888412 with a first trip of 20101024EE_040000_B63_0089_B63_11.
 * 
 * Every time it's correctly associated to the block at the start of the run.
 * The initial block assignment isn't super strong because the driver sets the
 * DSC in the middle of the layover, so maybe we hit one of those cases where it
 * didn't keep the proper block assignment once he started the moving?
 * 
 * @author bdferris
 */
public class Trace_7564_20101206T005551_IntegrationTest extends AbstractTraceRunner {

  public Trace_7564_20101206T005551_IntegrationTest() {
    super("7564-2010-12-06T00-55-51.csv.gz");
  }
}
