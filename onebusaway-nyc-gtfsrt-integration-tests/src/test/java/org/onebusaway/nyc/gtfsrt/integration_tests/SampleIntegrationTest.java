package org.onebusaway.nyc.gtfsrt.integration_tests;

/**
 * First pass at a GTFS-RT integration test.
 */
public class SampleIntegrationTest extends AbstractInputRunner {
    public SampleIntegrationTest() throws Exception {
        super("3901", "2016Jan_ManBC_WkdOnly", "2016-06-02T13:00:00EDT");
    }
}
