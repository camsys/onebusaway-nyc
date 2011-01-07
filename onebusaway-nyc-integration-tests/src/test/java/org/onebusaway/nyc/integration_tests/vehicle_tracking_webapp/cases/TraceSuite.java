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
    Trace_0927_20101209T124742_IntegrationTest.class,
    Trace_1325_20101215T014845_IntegrationTest.class,
    Trace_1379_20101211T010025_IntegrationTest.class,
    Trace_1404_20101210T034249_IntegrationTest.class,
    Trace_3649_20101125T121801_IntegrationTest.class,
    Trace_5318_20101202T172138_IntegrationTest.class,
    Trace_7560_20101122T084226_IntegrationTest.class,
    Trace_7560_20101122T221007_IntegrationTest.class,
    Trace_7560_20101123T031734_IntegrationTest.class,
    Trace_7560_20101123T234515_IntegrationTest.class})
public class TraceSuite {

}
