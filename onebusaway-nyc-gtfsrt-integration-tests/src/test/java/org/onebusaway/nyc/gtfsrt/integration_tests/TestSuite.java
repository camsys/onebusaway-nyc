package org.onebusaway.nyc.gtfsrt.integration_tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Entry point for integration tests.
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({SampleIntegrationTest.class})
public class TestSuite {
    // the annotations do the work

    @Test
    public void testMe() {
        System.out.println("this is a test!");
    }
}
