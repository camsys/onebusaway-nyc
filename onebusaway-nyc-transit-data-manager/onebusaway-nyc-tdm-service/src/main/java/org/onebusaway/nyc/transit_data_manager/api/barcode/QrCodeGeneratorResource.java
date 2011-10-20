package org.onebusaway.nyc.transit_data_manager.api.barcode;

import java.awt.Image;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.transit_data_manager.barcode.BarcodeContentsConverter;
import org.onebusaway.nyc.transit_data_manager.barcode.BarcodeContentsConverterImpl;
import org.onebusaway.nyc.transit_data_manager.barcode.GoogleChartBarcodeGenerator;
import org.onebusaway.nyc.transit_data_manager.barcode.QrCodeGenerator;
import org.springframework.stereotype.Component;

@Path("/barcode")
@Component
public class QrCodeGeneratorResource {

  // Note that while i'm putting these in lower case, they will be made uppercase before encoding into a barcode.
  private String shortenedBustimeBarcodeUrl = "http://bt.mta.info";
  private String busStopPath = "/s";
  
  @Path("/getByStopId/{stopId}")
  @GET
  @Produces("image/png")
  public Response getBadQrCodeForBusStopId (@PathParam("stopId") int stopId, @DefaultValue("300") @QueryParam("img-dimension") int imgDimension) {
    
    StringBuilder contents = new StringBuilder();
    
    contents.append(shortenedBustimeBarcodeUrl);
    contents.append(busStopPath);
    contents.append("/");
    contents.append(String.valueOf(stopId));
    
    BarcodeContentsConverter contentConv = new BarcodeContentsConverterImpl();
    
    Image responseImg = null;
    String imgMimeType = null;
    
    try {
      String barcodeContents = contentConv.contentsForUrl(contents.toString());
      
      // Set to uppercase for more compact encoding.
      barcodeContents = barcodeContents.toUpperCase();
      
      QrCodeGenerator barcodeGen = new GoogleChartBarcodeGenerator();
      
      responseImg = barcodeGen.generateV2Code(imgDimension, imgDimension, barcodeContents);
      imgMimeType = barcodeGen.getResultMimetype();
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    Response response = null;
   
    if (responseImg != null) {
      //response = Response.ok().entity(responseImg).build();
      response = Response.ok(responseImg, imgMimeType).build();
    } else {
      response = Response.serverError().build();
    }
    
    return response;
    
    
  }
}
