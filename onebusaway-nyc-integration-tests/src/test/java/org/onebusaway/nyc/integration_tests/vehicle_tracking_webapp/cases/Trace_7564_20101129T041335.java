package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

/**
 * Block = 2008_12888410
 * 
 * Interesting trace because it goes off route (deviation onto 4th Ave from 60th
 * to 50th)
 * 
 * Comment:
 * https://openplans.basecamphq.com/projects/5152119/todo_items/74384063
 * /comments
 * 
 * One issue I see is that we lose GPS from about 12:25 to 12:37 an incorrectly
 * assign to a different block when the GPS comes back. I think that incorrect
 * block assignment might lead to lingering problems later on at 3:25 pm...
 * 
 * 
 * @author bdferris
 */
public class Trace_7564_20101129T041335 extends AbstractTraceRunner {

  public Trace_7564_20101129T041335() {
    super("7564-2010-11-29T04-13-35.csv.gz");
  }
}
