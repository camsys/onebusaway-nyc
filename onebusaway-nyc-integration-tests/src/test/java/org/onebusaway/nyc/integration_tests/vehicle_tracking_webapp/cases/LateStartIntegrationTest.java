package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;


/**
 * Trip gets a late start (10 minutes). Normally, it would be block
 * 2008_20100627CC_097800_B62_0013_B62_18, but gets assigned to
 * 2008_20100627CC_098800_B62_0013_B62_26 instead, which comes 10 minutes later.
 * 
 * Simulation params:
 * 
 * blockId=2008_20100627CC_097800_B62_0013_B62_18
 * 
 * serviceDate = 1285041600000
 * 
 * include_start=true
 * 
 * schedule_deviations=0.0=10,0.5=15,1.0=20
 * 
 * location_sigma=10
 * 
 * @author bdferris
 */
public class LateStartIntegrationTest extends AbstractTraceRunner {

  public LateStartIntegrationTest() {
    super("2008_20100627CC_097800_B62_0013_B62_18-lateStart.csv.gz");
    setExpectedBlockId("2008_20100627CC_098800_B62_0013_B62_26");
    setAllowBlockMismatchOnFirstNRecords(2);
  }

}
