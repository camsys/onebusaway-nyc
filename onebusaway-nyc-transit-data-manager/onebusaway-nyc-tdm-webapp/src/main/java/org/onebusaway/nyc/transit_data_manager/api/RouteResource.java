package org.onebusaway.nyc.transit_data_manager.api;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.onebusaway.nyc.transit_data_manager.json.model.RouteDetail;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data_federation.services.blocks.BlockGeospatialService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

@Path("/route")
@Component
/**
 * WebService support for querying routes, specifically route to destination sign code
 * (DSC) mappings. 
 *
 */
public class RouteResource {
  
  private static Logger _log = LoggerFactory.getLogger(RouteResource.class);
  
  @Autowired
  private NycTransitDataService _transitDataService;
  @Autowired
  private DestinationSignCodeService _dscService;
  @Autowired
  JsonTool jsonTool;
  public RouteResource() {

  }

  @Path("/dsc/{dsc}")
  @GET
  @Produces("application/json")
  public Response getRoutes(@PathParam("dsc") String dsc) {
    // lookup routeId for that DSC
    Set<AgencyAndId> routeIds = _dscService.getRouteCollectionIdsForDestinationSignCode(dsc);
    if (!routeIds.isEmpty()) {
      AgencyAndId routeId = routeIds.iterator().next();
      RouteDetail routeDetail = new RouteDetail();
      routeDetail.setDsc(dsc);
      routeDetail.setRouteId(routeId.getId());
      routeDetail.setAgencyId(routeId.getAgencyId());
      // now lookup short/long name for that routeId
      RouteBean routeBean = _transitDataService.getRouteForId(routeId.toString());
      if (routeBean != null) {
        routeDetail.setDescription(routeBean.getDescription());
        routeDetail.setLongName(routeBean.getLongName());
        routeDetail.setShortName(routeBean.getShortName());
      }
      StringWriter writer = null;
      String output = null;
      try {
        writer = new StringWriter();
        jsonTool.writeJson(writer, routeDetail);
        output = writer.toString();
      } catch (IOException e) {
        _log.error("routeDetail parsing error:", e);
      } finally {
        try {
          writer.close();
        } catch (IOException e) {}
      }
      Response response = Response.ok(output).build();
      return response;
    }

    return null;
  }

  public void setJsonTool(JsonTool jsonTool) {
    this.jsonTool = jsonTool;
  }
  
}
