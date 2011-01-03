package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

/**
 * Starts in progress
 * 
 * Trip # 2008_20101024EE_088600_B63_0002_B63_25
 * 
 * Block # 2008_12888418
 * 
 * At some point, Eastbound on Atlantic Ave and had sent a DSC of 12, even
 * though block is still in progress. But it was inferred to be in phase
 * deadhead_before, and showing headsign 4631. confusing!
 * 
 * This trip has it all!
 * 
 * First off, we start the block already in progress, with a number of
 * potentially ambiguous block/trip assignments:
 * 
 * 2008_12888421 - headed the wrong direction 2008_12888583 - weird stub trip
 * that matches our own block perfectly, but terminates at Bay Ridge
 * 2008_12888404 - also pretty close
 * 
 * Around 8:23 pm, upon the termination of Cobble Hill bound trip
 * 2008_20101024EE_115900_B63_0089_B63_28 about 8 minutes ahead of schedule, the
 * operator goes out of service with a DSC of 12 and then proceeds to Atlantic
 * Ave between 3rd and 4th Ave and parks the bus from 8:30-8:48pm. At this
 * point, the driver sets the DSC to 4631 and resumes service (right on schedule
 * in fact) but finishes the end of the trip about 10 minutes early. He then
 * picks a Bay Ridge layover spot that I had not seen before (3rd ave between
 * Marine Ave and 99th St).
 * 
 * The GPS seems to cut out from around 9:51 to 10:05pm. It comes back on at
 * 10:05pm with a DSC of 0000 for one record and then 4630. At this point, 10:05
 * pm, the driver leaves the layover on time headed towards Cobble Hill on time.
 * 
 * Around 10:22 pm, we start to lose track of the bus.  Maybe the block?
 * 
 * @author bdferris
 */
public class Trace_7561_20101209T124755 extends AbstractTraceRunner {

  public Trace_7561_20101209T124755() {
    super("7561-2010-12-09T12-47-55.csv.gz");
  }
}
