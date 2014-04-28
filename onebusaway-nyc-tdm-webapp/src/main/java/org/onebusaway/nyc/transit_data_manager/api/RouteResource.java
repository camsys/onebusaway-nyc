package org.onebusaway.nyc.transit_data_manager.api;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.transit_data_manager.json.JsonTool;
import org.onebusaway.nyc.transit_data_manager.json.model.RouteDetail;
import org.onebusaway.nyc.transit_data_manager.json.model.RouteDetailList;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data_federation.services.blocks.BlockGeospatialService;
import org.onebusaway.transit_data_federation.services.blocks.BlockIndexService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
  /**
   * return route information for the given list of dscs (comma delimited).
   * @param dscStr
   * @return
   */
  public Response getRoutes(@PathParam("dsc") String dscStr) {
    List<String> dscList = parse(dscStr); 
    
    RouteDetailList routeDetailList = new RouteDetailList();
    routeDetailList.setStatus("unknown");
    String output = null;
    StringWriter writer = null;
    Response response = null;
    // lookup routeId for that DSC
    try {
      for (String dsc : dscList) {
        Set<AgencyAndId> routeIds = _dscService.getRouteCollectionIdsForDestinationSignCode(dsc, null);
        if (!routeIds.isEmpty()) {
          AgencyAndId routeId = routeIds.iterator().next();
          RouteDetail routeDetail = new RouteDetail();
          routeDetailList.getRoutes().add(routeDetail);
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
        } else {
          RouteDetail routeDetail = new RouteDetail();
          routeDetail.setDsc(dsc);
          routeDetailList.getRoutes().add(routeDetail);
        }
      } //end dsc      
      routeDetailList.setStatus("OK");
    } catch (Exception any){
      routeDetailList.setStatus("ERROR");
    }

    try {
      writer = new StringWriter();
      jsonTool.writeJson(writer, routeDetailList);
      output = writer.toString();
    } catch (IOException e) {
       // we tried our best to serve a message, give up     
       response = Response.serverError().build();
       return response;
    } finally {
        try {
          writer.close();
        } catch (IOException e) {}
    }
    
    response = Response.ok(output).build();
    return response;
  }

  private List<String> parse(String dscStr) {
    List<String> dscs = null;
    if (dscStr.indexOf(',') == -1) {
      dscs = new ArrayList<String>();
      dscs.add(dscStr);
    } else {
      dscs = Arrays.asList(dscStr.split("\\,"));
    }
    
    return dscs;
  }

  public void setJsonTool(JsonTool jsonTool) {
    this.jsonTool = jsonTool;
  }

  public void setTransitDataService(NycTransitDataService transitDataService) {
    this._transitDataService = transitDataService;
  }

  public void setDscService(DestinationSignCodeService dscService) {
    this._dscService = dscService;
  }
  
}
