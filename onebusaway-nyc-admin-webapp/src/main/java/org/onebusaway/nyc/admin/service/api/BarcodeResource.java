package org.onebusaway.nyc.admin.service.api;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Path("/barcode/")
@Component
/**
 * Proxy barcode request through to TDM to hide QR service from users. (TDM may not
 * be addressable/accessible). 
 *
 */
public class BarcodeResource {

  private static Logger _log = LoggerFactory.getLogger(BarcodeResource.class);
  
  @Path("/getByStopId/{stopId}")
  @GET
  @Produces("image/jpeg")
  public Response proxy(@PathParam("stopId") String stopId, 
      @DefaultValue("99") @QueryParam("img-dimension") String imgDimension) {
    // proxy request here to TDM
    String uri = "http://tdm/api/barcode/getByStopId/" + stopId;
    uri = uri + "?" + "img-dimension=" + imgDimension;

    Response response = Response.ok(proxyRequest(uri)).build();
    return response;
  }

  protected InputStream proxyRequest(String uri) {
    HttpURLConnection connection = null;
    InputStream is = null;
    try {
      connection = (HttpURLConnection) new URL(uri).openConnection();
      connection.setRequestMethod("GET");
      connection.setDoOutput(true);
      connection.setReadTimeout(10000);
      is = connection.getInputStream();
    } catch (Exception any) {
      _log.error("proxyRequest failed:", any);
    }
    return is;
  }
  
}
