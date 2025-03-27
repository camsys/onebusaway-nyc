/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import org.junit.Test;
import org.onebusaway.nyc.transit_data_federation.model.nyc.RouteType;
import org.onebusaway.util.AgencyAndIdLibrary;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NycRouteTypeServiceTest {

    @Test
    public void when_routetypes_retreived_as_json_then_total_routetypes_is_306() throws IOException {
        InputStream inputStream =  getClass().getResourceAsStream("route_types.json");
        NycRouteTypeService routeTypeService = new NycRouteTypeService();
        routeTypeService.updateNycRouteTypeData(inputStream);
        assertEquals(306, routeTypeService.getRouteTypes().size());
    }

    @Test
    public void when_routetypes_retreived_as_json_then_check_x37_is_express() throws IOException {
        InputStream inputStream =  getClass().getResourceAsStream("route_types.json");
        NycRouteTypeService routeTypeService = new NycRouteTypeService();
        routeTypeService.updateNycRouteTypeData(inputStream);
        RouteType routeType = routeTypeService.getRouteTypes().get(AgencyAndIdLibrary.convertFromString("NYCT_X37"));
        assertTrue(routeType.equals(RouteType.EXPRESS));
        assertTrue(routeTypeService.isRouteExpress(AgencyAndIdLibrary.convertFromString("NYCT_X37")));
    }
}
