package org.onebusaway.nyc.transit_data_manager.api.barcode;

import java.awt.Image;
import java.awt.image.BufferedImage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.onebusaway.nyc.transit_data_manager.barcode.BarcodeContentsConverter;
import org.onebusaway.nyc.transit_data_manager.barcode.BarcodeContentsConverterImpl;
import org.onebusaway.nyc.transit_data_manager.barcode.GoogleChartBarcodeGenerator;
import org.onebusaway.nyc.transit_data_manager.barcode.StringToImageBarcodeGenerator;
import org.springframework.stereotype.Component;

@Path("/barcode")
@Component
public class QrCodeGeneratorResource {

  private String mbtaSubwayBaseUrl = "http://www.mbta.com/schedules_and_maps/subway/lines/stations/";
  private String mbtaStopIdParam = "stopId=";
  
  @Path("/getByStopId/{stopId}")
  @GET
  @Produces("image/png")
  public Response getBadQrCodeForBusStopId (@PathParam("stopId") int stopId) {
    StringBuilder contents = new StringBuilder();
    
    contents.append(mbtaSubwayBaseUrl);
    contents.append("?");
    contents.append(mbtaStopIdParam);
    contents.append(String.valueOf(stopId));
    
    BarcodeContentsConverter contentConv = new BarcodeContentsConverterImpl();
    
    Image responseImg = null;
    String imgMimeType = null;
    
    try {
      String barcodeContents = contentConv.contentsForUrl(contents.toString());
      
      StringToImageBarcodeGenerator barcodeGen = new GoogleChartBarcodeGenerator();
      
      responseImg = barcodeGen.generateBarcode(100, 100, barcodeContents);
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
