package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import org.junit.Test;
import org.onebusaway.util.AgencyAndIdLibrary;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertTrue;

public class NycRouteTypeServiceTest {

    @Test
    public void testUpdateExpressRoutes() throws IOException {
        InputStream stream =  getClass().getResourceAsStream("supplimental_route_types.csv");
        NycRouteTypeService nycRouteTypeService = new NycRouteTypeService(stream);
//        NycRouteTypeService nycRouteTypeService = new NycRouteTypeService();
        assertTrue(nycRouteTypeService.isRouteExpress(AgencyAndIdLibrary.convertFromString("NYCT_X37")));

    }
}
