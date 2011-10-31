package org.onebusaway.nyc.transit_data_manager.api.barcode;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.onebusaway.nyc.transit_data_manager.api.CrewResource;
import org.onebusaway.nyc.transit_data_manager.barcode.BarcodeContentsConverter;
import org.onebusaway.nyc.transit_data_manager.barcode.BarcodeContentsConverterImpl;
import org.onebusaway.nyc.transit_data_manager.barcode.BarcodeImageType;
import org.onebusaway.nyc.transit_data_manager.barcode.GoogleChartBarcodeGenerator;
import org.onebusaway.nyc.transit_data_manager.barcode.QRErrorCorrectionLevel;
import org.onebusaway.nyc.transit_data_manager.barcode.QrCodeGenerator;
import org.onebusaway.nyc.transit_data_manager.barcode.model.MtaBarcode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import au.com.bytecode.opencsv.CSVReader;

@Path("/barcode")
@Component
@Produces("image/*")
public class QrCodeGeneratorResource {

  public QrCodeGeneratorResource() {
    super();
    
    setContentConv(new BarcodeContentsConverterImpl());
    setBarcodeGen(new GoogleChartBarcodeGenerator(QRErrorCorrectionLevel.Q));
  }
  
  private static Logger _log = LoggerFactory.getLogger(CrewResource.class);

  // Note that while i'm putting these in lower case, they will be made
  // uppercase before encoding into a barcode.
  private String shortenedBustimeBarcodeUrl = "http://bt.mta.info";
  private String busStopPath = "/s";
  
  private static String STOP_COLUMN_NAME = "STOP_ID";

  private static String ZIP_FILE_PREFIX = "QrCodes";
  private static String ZIP_FILE_SUFFIX = ".zip";

  private QrCodeGenerator barcodeGen;
  private BarcodeContentsConverter contentConv;

  public void setBarcodeGen(QrCodeGenerator barcodeGen) {
    this.barcodeGen = barcodeGen;
  }

  public void setContentConv(BarcodeContentsConverter contentConv) {
    this.contentConv = contentConv;
  }

  @Path("/getByStopId/{stopId}")
  @GET
  public Response getQrCodeForStopUrlById(
      @PathParam("stopId") int stopId,
      @DefaultValue("300") @QueryParam("img-dimension") int imgDimension,
      @DefaultValue("BMP") @QueryParam("img-type") String imgFormatName) {
    
    final RenderedImage responseImg;
    final BarcodeImageType imageType;
    
    // determine the image type parameter.
    if ("PNG".equals(imgFormatName)) {
      imageType = BarcodeImageType.PNG;
    } else if ("BMP".equals(imgFormatName)) {
      imageType = BarcodeImageType.BMP;
    }  else {
      imageType = BarcodeImageType.BMP;
    }

    String barcodeContents = generateBusStopContentsForStopId(stopId);
    
    boolean contentsFitBarcodeVersion = contentConv.fitsV2QrCode(QRErrorCorrectionLevel.Q, barcodeContents);
    
    Response response = null;
    
    if (!"".equals(barcodeContents) && contentsFitBarcodeVersion) {
      responseImg = barcodeGen.generateCode(imgDimension, imgDimension, barcodeContents);
      
      StreamingOutput output = new StreamingOutput() {
        
        @Override
        public void write(OutputStream os) throws IOException,
            WebApplicationException {
          ImageOutputStream ios = ImageIO.createImageOutputStream(os);
          ImageIO.write(responseImg, imageType.getFormatName(), ios);
          ios.close();
        }
      };
      
      response = Response.ok(output, imageType.getMimeType()).build();
      
    } else {

      response = Response.ok().build();
    }
    
    return response;

  }
  
  @Path("/batchGen")
  @Consumes({"text/comma-separated-values", "text/csv"})
  @POST
  public Response batchGenerateBarcodes(InputStream inputFileStream) {

    _log.info("batchGenerateBarcodes Started.");
    
    int stopColumnIdx = -1;

    /*
     * First check the header to make sure our data is in an acceptable format
     * Take note of the stopid column to use it later.
     */

    CSVReader reader = new CSVReader(new InputStreamReader(inputFileStream));

    // First make sure we have a STOP_COLUMN_NAME column.
    // Take note of its index.
    List<String[]> allLines = null;
    String[] headerLine;
    List<String[]> dataLines = null;

    try {
      allLines = reader.readAll();
      headerLine = allLines.get(0);
      _log.info("found " + allLines.size()
          + " total lines in input. header is " + Arrays.toString(headerLine));

      dataLines = allLines.subList(1, allLines.size());
    } catch (IOException e) {
      e.printStackTrace();
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    }

    for (int i = 0; i < headerLine.length; i++) {
      if (STOP_COLUMN_NAME.equalsIgnoreCase(headerLine[i])) {
        if (stopColumnIdx == -1) {
          stopColumnIdx = i;
        } else { // multiple columns with the same column name?
          throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
      }
    }

    if (stopColumnIdx == -1) { // We didn't find the column
      throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    /*
     * Now that we have the header sorted out, create a barcode for each line
     * and add it to the zipfile.
     */

    List<MtaBarcode> urlsToEncode = new ArrayList<MtaBarcode>();

    Iterator<String[]> linesIt = dataLines.iterator();
    String[] line;
    while (linesIt.hasNext()) {
      line = linesIt.next();

      _log.info("encoding stopnum " + line[stopColumnIdx]);

      int stopId = Integer.parseInt(line[stopColumnIdx]);

      String barcodeContents = generateBusStopContentsForStopId(stopId);
      
      boolean contentsFitBarcodeVersion = contentConv.fitsV2QrCode(
          QRErrorCorrectionLevel.Q, barcodeContents);

      if (!"".equals(barcodeContents) && contentsFitBarcodeVersion) {

        MtaBarcode barcode = new MtaBarcode();

        barcode.setContents(barcodeContents);
        barcode.setStopId(stopId);

        urlsToEncode.add(barcode);

      }
    }

    Response response = null;

    final File resultZipFile;
    try {
      resultZipFile = generateBarcodeZipFileFromUrlList(urlsToEncode);

      StreamingOutput output = new StreamingOutput() {

        @Override
        public void write(OutputStream os) throws IOException,
            WebApplicationException {

          InputStream in = new FileInputStream(resultZipFile);
          int b;
          while ((b = in.read()) != -1) {
            os.write(b);
          }

          in.close();
          os.close();
        }
      };

      response = Response.ok(output, "application/zip").header(
          "Content-disposition",
          "attachment; filename=" + ZIP_FILE_PREFIX + ZIP_FILE_SUFFIX).build();
    } catch (IOException e) {
      response = Response.serverError().build();
    }

    return response;
  }

  protected File generateBarcodeZipFileFromUrlList(List<MtaBarcode> bcList)
      throws IOException {

    File tempFile = getTempFile(ZIP_FILE_PREFIX, ZIP_FILE_SUFFIX);

    FileOutputStream fos = new FileOutputStream(tempFile);
    ZipOutputStream zipOutput = new ZipOutputStream(fos);

    for (MtaBarcode bc : bcList) {
      RenderedImage responseImg = barcodeGen.generateCode(200, 200,
          bc.getContents());

      ZipEntry zipEntry = new ZipEntry(String.valueOf(bc.getStopId()) + ".bmp");

      zipOutput.putNextEntry(zipEntry);

      ImageIO.write(responseImg, "BMP", zipOutput);
    }

    zipOutput.close();

    return tempFile;
  }

  private File getTempFile(String prefix, String suffix) throws IOException {
    // Create the zip file & output stream.
    File tempFile;

    try {
      tempFile = File.createTempFile(prefix, suffix);
    } catch (IOException e) {
      try {
        tempFile = File.createTempFile(prefix + "_B", suffix);
      } catch (IOException e1) {
        throw new IOException("Could not create temporary file", e);
      }
    }

    _log.info("Generated temp file " + tempFile.getPath());

    return tempFile;
  }

  private String getStopUrl(int stopId) {
    StringBuilder contents = new StringBuilder();

    contents.append(shortenedBustimeBarcodeUrl);
    contents.append(busStopPath);
    contents.append("/");
    contents.append(String.valueOf(stopId));

    return contents.toString();
  }
  
  private String generateBusStopContentsForStopId (int stopId) {
    String barcodeContents = contentConv.contentsForUrl(getStopUrl(stopId));

    // Set to uppercase for more compact encoding.
    barcodeContents = barcodeContents.toUpperCase();
    
    return barcodeContents;
  }
}
