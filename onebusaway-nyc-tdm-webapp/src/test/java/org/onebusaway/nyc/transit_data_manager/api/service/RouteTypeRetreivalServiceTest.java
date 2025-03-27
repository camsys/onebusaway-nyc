package org.onebusaway.nyc.transit_data_manager.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import org.junit.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.model.nyc.RouteType;
import org.onebusaway.util.AgencyAndIdLibrary;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class RouteTypeRetreivalServiceTest {

    @Test
    public void when_routetypes_retreived_then_check_that_s40_routetype_is_feeder() throws IOException {
        InputStream inputStream =  getClass().getResourceAsStream("supplemental_route_types.csv");
        RouteTypeRetreivalServiceImpl routeTypeRetreivalService = new RouteTypeRetreivalServiceImpl();
        routeTypeRetreivalService.updateRouteTypeData(inputStream);
        RouteType routeType = routeTypeRetreivalService
                .getRouteTypes()
                .get(AgencyAndIdLibrary.convertFromString("NYCT_S40"));

        assertTrue(routeType.equals(RouteType.FEEDER));
    }

    @Test
    public void when_routetypes_retreived_then_check_that_x37_routetype_is_express() throws IOException {
        InputStream inputStream =  getClass().getResourceAsStream("supplemental_route_types.csv");
        RouteTypeRetreivalServiceImpl routeTypeRetreivalService = new RouteTypeRetreivalServiceImpl();
        routeTypeRetreivalService.updateRouteTypeData(inputStream);
        RouteType routeType = routeTypeRetreivalService
                .getRouteTypes()
                .get(AgencyAndIdLibrary.convertFromString("NYCT_X37"));

        ObjectMapper mapper = new ObjectMapper().registerModule(new JaxbAnnotationModule());
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        String json = writer.writeValueAsString( routeTypeRetreivalService.getRouteTypes());

        // Print the JSON
        System.out.println(json);

        assertTrue(routeType.equals(RouteType.EXPRESS));
    }
}
