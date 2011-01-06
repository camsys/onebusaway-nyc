package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * All the trace tests in one suite so they can quickly be run from within
 * Eclipse for testing.
 * 
 * @author bdferris
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    Trace_0927_20101209T124742.class, Trace_1325_20101215T014845.class,
    Trace_1379_20101211T010025.class})
public class TraceSuite {

}
