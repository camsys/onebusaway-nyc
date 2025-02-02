package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;

public class NycRouteTypeServiceTest {

    @Test
    public void testUpdateExpressRoutes() throws IOException {
        InputStream stream =  getClass().getResourceAsStream("supplimental_route_types.csv");
        NycRouteTypeService nycRouteTypeService = new NycRouteTypeService(stream);
//        NycRouteTypeService nycRouteTypeService = new NycRouteTypeService();
        assertTrue(nycRouteTypeService.isRouteExpress("NYCT_X37"));

    }
}
