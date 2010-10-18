package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

/**
 * This was taken from 04513.gps.csv.
 * 
 * blockA = 2008_12024495
 * 
 * duration = 4:55-8:55am
 * 
 * layovers = 5:41-6:00am, 6:41-6:55am, 7:45-8:00am
 * 
 * blockB = 2008_12024326
 * 
 * duration = 9:42am-2:30 pm
 * 
 * layovers = 10:36-10:51am, 11:56a-12:12pm, 1:15-1:25 pm
 * 
 * blockC = 2008_12024530
 * 
 * duration = 3:05-4:55 pm,
 * 
 * layover = 3:56-4:02pm
 * 
 * blockD = 2008_12024348
 * 
 * duration = 5:35-9:50pm
 * 
 * layover = 6:36-6:40pm, 7:40-7:50pm, 8:41-8:55pm
 * 
 * @author bdferris
 */
public class LongTraceIntegrationTest extends AbstractTraceRunner {

  public LongTraceIntegrationTest() {
    super("04513-0.gps.csv.gz");
  }
}
