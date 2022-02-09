package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.AbstractTraceRunner;

public class Trace_258_20220202_capi_switching_trips_IntegrationTest extends AbstractTraceRunner {

    public Trace_258_20220202_capi_switching_trips_IntegrationTest() throws Exception {
        super("258_20220202_capi_switching_trips.csv");
        setBundle("2022Jan_Prod_r03_b02", "2022-02-02T00:00:00EDT");
    }
}