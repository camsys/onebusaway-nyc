package org.onebusaway.nyc.transit_data_manager.api.barcode;

import java.awt.Image;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.onebusaway.nyc.transit_data_manager.barcode.BarcodeContentsConverter;
import org.onebusaway.nyc.transit_data_manager.barcode.BarcodeContentsConverterImpl;
import org.onebusaway.nyc.transit_data_manager.barcode.GoogleChartBarcodeGenerator;
import org.onebusaway.nyc.transit_data_manager.barcode.QRErrorCorrectionLevel;
import org.onebusaway.nyc.transit_data_manager.barcode.QrCodeGenerator;
import org.springframework.stereotype.Component;

@Path("/barcode")
@Component
@Produces("image/x-ms-bmp")
public class QrCodeGeneratorResource {

  public QrCodeGeneratorResource() {
    setContentConv(new BarcodeContentsConverterImpl());
  }

  private static String MIME_TYPE_BITMAP = "image/x-ms-bmp";
  private static String MIME_TYPE_PNG = "image/png";

  // Note that while i'm putting these in lower case, they will be made
  // uppercase before encoding into a barcode.
  private String shortenedBustimeBarcodeUrl = "http://bt.mta.info";
  private String busStopPath = "/s";

  private BarcodeContentsConverter contentConv;

  public void setContentConv(BarcodeContentsConverter contentConv) {
    this.contentConv = contentConv;
  }

  @Path("/getByStopId/{stopId}")
  @GET
  public Response getQrCodeForStopUrlById(
      @PathParam("stopId") int stopId,
      @DefaultValue("300") @QueryParam("img-dimension") int imgDimension,
      @DefaultValue("BMP") @QueryParam("img-type") String imgType) {

    final RenderedImage responseImg;
    String imgMimeType = MIME_TYPE_BITMAP;

    String barcodeContents = contentConv.contentsForUrl(getStopUrl(stopId));

    // Set to uppercase for more compact encoding.
    barcodeContents = barcodeContents.toUpperCase();
    
    boolean contentsFitBarcodeVersion = contentConv.fitsV2QrCode(QRErrorCorrectionLevel.Q, barcodeContents);
    
    Response response = null;
    
    if (!"".equals(barcodeContents) && contentsFitBarcodeVersion) {
      QrCodeGenerator barcodeGen = new GoogleChartBarcodeGenerator(QRErrorCorrectionLevel.Q);

      responseImg = barcodeGen.generateCode(imgDimension, imgDimension, barcodeContents);
      
      StreamingOutput output = new StreamingOutput() {
        
        @Override
        public void write(OutputStream os) throws IOException,
            WebApplicationException {
          ImageOutputStream ios = ImageIO.createImageOutputStream(os);
          ImageIO.write(responseImg, "BMP", ios);
          ios.close();
        }
      };
      
      response = Response.ok(output, imgMimeType).build();
      
    } else {

      response = Response.ok().build();
    }
    
    return response;

  }

  private String getStopUrl(int stopId) {
    StringBuilder contents = new StringBuilder();

    contents.append(shortenedBustimeBarcodeUrl);
    contents.append(busStopPath);
    contents.append("/");
    contents.append(String.valueOf(stopId));

    return contents.toString();
  }
}
