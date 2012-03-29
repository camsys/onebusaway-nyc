package org.onebusaway.nyc.transit_data_manager.api.barcode;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.onebusaway.nyc.transit_data_manager.barcode.BarcodeContentsConverter;
import org.onebusaway.nyc.transit_data_manager.barcode.BarcodeContentsConverterImpl;
import org.onebusaway.nyc.transit_data_manager.barcode.BarcodeImageType;
import org.onebusaway.nyc.transit_data_manager.barcode.GoogleChartBarcodeGenerator;
import org.onebusaway.nyc.transit_data_manager.barcode.QRErrorCorrectionLevel;
import org.onebusaway.nyc.transit_data_manager.barcode.QrCodeGenerator;
import org.onebusaway.nyc.transit_data_manager.barcode.model.MtaBarcode;
import org.onebusaway.nyc.transit_data_manager.util.ZipFileBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

@Path("/barcode")
@Component
public class QrCodeGeneratorResource {

  public QrCodeGeneratorResource() {
    super();

    setContentConv(new BarcodeContentsConverterImpl());
    setBarcodeGen(new GoogleChartBarcodeGenerator());
    barcodeGen.setEcLevel(QRErrorCorrectionLevel.Q);
  }

  private static String REPLACE_STR = "__REPLACE__";
  private static String STOP_COLUMN_NAME = "STOP_ID";

  private static Logger _log = LoggerFactory.getLogger(QrCodeGeneratorResource.class);

  @Autowired
  private String urlToEmbedStopIdReplace;

  private static String ZIP_FILE_PREFIX = "QrCodes";
  private static String ZIP_FILE_SUFFIX = ".zip";

  private QrCodeGenerator barcodeGen;
  private BarcodeContentsConverter contentConv;

  public void setUrlToEmbedStopIdReplace(String replaceUrl) {
    this.urlToEmbedStopIdReplace = replaceUrl;
  }

  public void setBarcodeGen(QrCodeGenerator barcodeGen) {
    this.barcodeGen = barcodeGen;
  }

  public void setContentConv(BarcodeContentsConverter contentConv) {
    this.contentConv = contentConv;
  }

  @Path("/getByStopId/{stopId}")
  @GET
  public Response getQrCodeForStopUrlById(@PathParam("stopId")
  int stopId, @DefaultValue("99")
  @QueryParam("img-dimension")
  final int imgDimension, @DefaultValue("BMP")
  @QueryParam("img-type")
  String imgFormatName, @DefaultValue("4")
  @QueryParam("margin-rows")
  final int quietZoneRows) {

    _log.info("Starting getQrCodeForStopUrlById.");

    final BarcodeImageType imageType = parseImgFormatName(imgFormatName);

    final String barcodeContents = generateBusStopContentsForStopId(String.valueOf(stopId));

    // boolean contentsFitBarcodeVersion = contentConv.fitsV2QrCode(
    // QRErrorCorrectionLevel.Q, barcodeContents);

    boolean contentsFitBarcodeVersion = true;

    if (!contentsFitBarcodeVersion) {
      throw new WebApplicationException(new IllegalArgumentException(
          "Input stop id too long to fit V2 Qr Code"),
          Response.Status.BAD_REQUEST);
    }

    Response response = null;

    if (!"".equals(barcodeContents) && contentsFitBarcodeVersion) {
      StreamingOutput output = new StreamingOutput() {

        @Override
        public void write(OutputStream os) throws IOException,
            WebApplicationException {

          genBarcodeWriteToOutputStream(barcodeContents, imgDimension,
              quietZoneRows, imageType, os);

          if (os != null)
            os.close();
        }
      };

      response = Response.ok(output, imageType.getMimeType()).header(
          "Content-Disposition",
          "attachment; filename=\"" + String.valueOf(stopId) + "."
              + imageType.getFormatName() + "\"").build();

    } else {
      response = Response.serverError().build();
    }

    _log.info("Returning Response in getQrCodeForStopUrlById.");
    return response;
  }

  @Path("/batchGen")
  @Consumes({"text/comma-separated-values", "text/csv"})
  @POST
  public Response batchGenerateBarcodes(@DefaultValue("99")
  @QueryParam("img-dimension")
  int imgDimension, @DefaultValue("BMP")
  @QueryParam("img-type")
  String imgFormatName, @DefaultValue("4")
  @QueryParam("margin-rows")
  int quietZoneRows, InputStream inputFileStream) {

    _log.info("batchGenerateBarcodes Started.");

    BarcodeImageType imageType = parseImgFormatName(imgFormatName);

    Set<MtaBarcode> urlsToEncode;
    try {
      urlsToEncode = parseListResultBarcodesFromInputCsv(inputFileStream);
    } catch (IOException e) {
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    } finally {
      if (inputFileStream != null)
        try {
          inputFileStream.close();
        } catch (IOException e) {
          throw new WebApplicationException(e,
              Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    Response response = null;

    final File resultZipFile;
    try {
      
      _log.info("Batch generating " + urlsToEncode.size() + " barcodes.");
      
      resultZipFile = generateBarcodeZipFileFromUrlList(urlsToEncode,
          imgDimension, imageType, quietZoneRows);
    } catch (IOException e1) {
      _log.warn("Error generating zip file.");
      throw new WebApplicationException(e1,
          Response.Status.INTERNAL_SERVER_ERROR);
    };

    long zipFileLength = resultZipFile.length();

    StreamingOutput output = new StreamingOutput() {

      @Override
      public void write(OutputStream os) throws IOException,
          WebApplicationException {

        FileChannel inChannel = null;
        WritableByteChannel outChannel = null;

        try {
          inChannel = new FileInputStream(resultZipFile).getChannel();
          outChannel = Channels.newChannel(os);

          inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
          if (outChannel != null)
            outChannel.close();
          if (inChannel != null)
            inChannel.close();
        }

      }
    };

    response = Response.ok(output, "application/zip").header(
        "Content-disposition",
        "attachment; filename=" + ZIP_FILE_PREFIX + ZIP_FILE_SUFFIX).header(
        "Content-Length", zipFileLength).build();

    _log.info("Returning Response in batchGenerateBarcodes.");

    return response;
  }

  private Set<MtaBarcode> parseListResultBarcodesFromInputCsv(
      InputStream inputCsvFileStream) throws IOException {

    int stopColumnIdx = -1;

    CSVReader reader = null;
    
    String [] headerLine;
    
    try {
      reader = new CSVReader(new InputStreamReader(inputCsvFileStream));

      headerLine = reader.readNext();
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

    Set<MtaBarcode> urlsToEncode = new HashSet<MtaBarcode>();

    String[] nextLine;
    
    while ((nextLine = reader.readNext()) != null) {
      _log.debug("adding stopnum " + nextLine[stopColumnIdx]
          + "to the list of items to encode.");

      String stopIdStr = nextLine[stopColumnIdx];

      String barcodeContents = generateBusStopContentsForStopId(stopIdStr);

      if (!"".equals(barcodeContents) ) {

        MtaBarcode barcode = new MtaBarcode(barcodeContents);

        barcode.setStopIdStr(stopIdStr);
        
        if (!urlsToEncode.contains(barcode)) {
          urlsToEncode.add(barcode);
        }
      }
    }

    if (reader != null)
      try{
        reader.close();
      } catch (IOException e) { } // do nothing.

    return urlsToEncode;
  }

  /**
   * This method generates a zip file containing a barcode image for each
   * barcode in bcList. Additionally, the zip file contains one output file
   * specific to the Hastus application in use by the MTA.
   * 
   * @param urlsToEncode This method generates a barcode image file for each unique
   *          barcode in this set.
   * @param imageDimension
   * @param imgType
   * @param quietZoneRows
   * @return
   * @throws IOException
   */
  protected File generateBarcodeZipFileFromUrlList(Set<MtaBarcode> urlsToEncode,
      int imageDimension, BarcodeImageType imgType, int quietZoneRows)
      throws IOException {

    File tempFile = getTempFile(ZIP_FILE_PREFIX, ZIP_FILE_SUFFIX);

    ZipFileBuilder zipBuilder = null;

    CSVWriter datafileWriter = null;

    try {
      zipBuilder = new ZipFileBuilder(tempFile);

      // The format of the data file is specified in a document provided by the
      // mta.

      // It is a semicolon delimited file with the following form:
      // Keyword; stop code; QR Code file name; description of the stop
      // attachment

      // Basically, it looks like the following:
      // stpattach;202097;202097.bmp;QR Code for stop 202097
      // stpattach;503967;503967.bmp;QR Code for stop 503967

      int datafileNumElements = 4;

      // The positions of the different fields.
      int datafileKeywordIdx = 0;
      int datafileStopcodeIdx = 1;
      int datafileFilenameIdx = 2;
      int datafileDescriptionIdx = 3;

      // The various strings that are basically constant.
      String datafileFilename = "hastus_datafile.txt";
      String datafileKeywordValue = "stpattach";
      String datafileDescriptionReplaceStr = "QR Code for stop ~~STOP_ID~~";
      String datafileDescriptionStopIdReplaceToken = "~~STOP_ID~~";

      // Need to keep track of the barcodes we produce, so create a list of
      // String[],
      // This can then be passed into OpenCSVs csvwriter later.
      List<String[]> barcodesDatafile = new ArrayList<String[]>();

      // Loop over all input barcodes, generating an image for each.
      for (MtaBarcode bc : urlsToEncode) {

        String imgFileName = String.valueOf(bc.getStopIdStr()) + "."
            + imgType.getFormatName();

        _log.debug("writing " + imgFileName + " to tempfile.");

        genBarcodeWriteToOutputStream(bc.getContents(), imageDimension,
            quietZoneRows, imgType, zipBuilder.addFile(imgFileName));

        // Now update barcodesDatafile with the info for this barcode.
        String[] datafileRow = new String[datafileNumElements];
        datafileRow[datafileKeywordIdx] = datafileKeywordValue;
        datafileRow[datafileStopcodeIdx] = bc.getStopIdStr();
        datafileRow[datafileFilenameIdx] = imgFileName;
        datafileRow[datafileDescriptionIdx] = datafileDescriptionReplaceStr.replaceFirst(
            datafileDescriptionStopIdReplaceToken, bc.getStopIdStr());

        barcodesDatafile.add(datafileRow);
      }

      datafileWriter = new CSVWriter(new OutputStreamWriter(
          zipBuilder.addFile(datafileFilename)), ';',
          CSVWriter.NO_QUOTE_CHARACTER);
      datafileWriter.writeAll(barcodesDatafile);

    } finally {
      if (datafileWriter != null)
        datafileWriter.close();

      if (zipBuilder != null)
        zipBuilder.close();

    }

    return tempFile;
  }

  private void genBarcodeWriteToOutputStream(String barcodeContents,
      int imageDimension, int quietZoneRows, BarcodeImageType imgType,
      OutputStream outputStream) throws IOException {
    RenderedImage img = barcodeGen.generateCode(barcodeContents,
        imageDimension, imageDimension, quietZoneRows);

    ImageIO.write(img, imgType.getFormatName(), outputStream);
  }

  private File getTempFile(String prefix, String suffix) throws IOException {
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

  private String getStopUrl(String stopId) {
    return getEmbeddedStopIdUrl(stopId);
  }

  private String generateBusStopContentsForStopId(String stopId) {
    String barcodeContents = contentConv.contentsForUrl(getStopUrl(stopId));

    return barcodeContents;
  }

  private BarcodeImageType parseImgFormatName(String imgFormatName) {
    BarcodeImageType imageType;

    // determine the image type parameter.
    if ("PNG".equalsIgnoreCase(imgFormatName)) {
      imageType = BarcodeImageType.PNG;
    } else if ("BMP".equalsIgnoreCase(imgFormatName)) {
      imageType = BarcodeImageType.BMP;
    } else {
      imageType = BarcodeImageType.BMP;
    }

    return imageType;
  }

  /**
   * This method takes a stopId and returns a string representing the URL to
   * embed in barcodes for that stop Id. It assumes the property
   * urlToEmbedStopIdReplace in this class contains a replacement string,
   * '__REPLACE__'
   * 
   * @param stopId The stop Id to substitute for __REPLACE__.
   * @return A string with the stop id url for embedding into barcodes. Does not
   *         capitalize it or anything which may be needed for good barcodes.
   */
  private String getEmbeddedStopIdUrl(String stopIdStr) {
    String stopIdUrl = urlToEmbedStopIdReplace.replaceAll(REPLACE_STR,
        stopIdStr);

    return stopIdUrl;
  }
}
