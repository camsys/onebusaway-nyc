package org.onebusaway.nyc.transit_data_manager.barcode;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.activation.MimeType;
import javax.imageio.ImageIO;

/**
 * Generates QR Codes using the Google Charts Infographics service.
 * 
 * @author sclark
 * 
 */
public class GoogleChartBarcodeGenerator implements
    StringToImageBarcodeGenerator {

  private static String URL_ENCODING_NAME = "UTF-8";
  
  private static String HEIGHT_REPLACE = "__HHH__";
  private static String WIDTH_REPLACE = "__WWW__";

  /*
   * Google documentation, 20111007 Taken from
   * http://code.google.com/apis/chart/infographics/docs/overview.html
   * 
   * The infographics server returns an image in response to a URL GET or POST
   * request. All the data required to create the graphic is included in the
   * URL, including the image type and size. For example, copy and paste the
   * following URL in your browser:
   * 
   * https://chart.googleapis.com/chart?chs=150x150&cht=qr&chl=Hello%20world
   * 
   * The image you see is a QR code representation of the phrase "Hello World".
   * Try changing the phrase to your own name and refresh your browser. That's
   * all it takes!
   * 
   * Here's a little more explanation of the URL:
   * 
   * 
   * https://chart.googleapis.com/chart?chs=150x150&cht=qr&chl=Hello%20world
   * 
   * https://chart.googleapis.com/chart? - All infographic URLs start with this
   * root URL, followed by one or more parameter/value pairs. The required and
   * optional parameters are specific to each image; read your image
   * documentation. chs - Size of the image in pixels, in the format
   * <width>x<height> cht - Type of image: 'qr' means QR code. chl - The data to
   * encode. Must be URL-encoded.
   */

  private static String CHART_ROOT_URL = "https://chart.googleapis.com/chart";
  private static String CHART_PARAMNAME_SIZE = "chs=";
  private static String CHART_PARAMNAME_TYPE = "cht=";
  private static String CHART_PARAMNAME_DATA = "chl=";
  private static String CHART_PARAMVALUE_TYPE_QR = "qr";
  private static String CHART_PARAMVALUE_SIZE_REPLACE = "__WWW__x__HHH__";

  private static String CHART_IMAGE_MIMETYPE = "image/png"; 
  
  @Override
  public Image generateBarcode(int width, int height, String text) throws Exception {
    Image resultImage = null;

    String url = "";
    
    try {
      String urlEncodedText = URLEncoder.encode(text, URL_ENCODING_NAME);

      StringBuilder urlBldr = new StringBuilder();

      urlBldr.append(CHART_ROOT_URL);
      urlBldr.append("?");
      urlBldr.append(CHART_PARAMNAME_SIZE + getSizeParamValue(width, height));
      urlBldr.append("&");
      urlBldr.append(CHART_PARAMNAME_TYPE + CHART_PARAMVALUE_TYPE_QR);
      urlBldr.append("&");
      urlBldr.append(CHART_PARAMNAME_DATA);
      urlBldr.append(urlEncodedText);

      url = urlBldr.toString();
      
      URL qrRequestUrl = new URL(url);

      resultImage = ImageIO.read(qrRequestUrl);
      
    } catch (UnsupportedEncodingException unsupEncE) {
      throw new Exception("URLEncoder.encode could not deal with " + URL_ENCODING_NAME + " encoding", unsupEncE);
    } catch (MalformedURLException malUrlE) {
      throw new Exception("URL could not deal with the url " + url +". Check the protocol of the url.", malUrlE);
    }

    return resultImage;
  }

  private String getSizeParamValue(int width, int height) {
    String paramValue = CHART_PARAMVALUE_SIZE_REPLACE;

    paramValue = paramValue.replaceAll(WIDTH_REPLACE, String.valueOf(width));
    paramValue = paramValue.replaceAll(HEIGHT_REPLACE, String.valueOf(height));

    return paramValue;
  }

  @Override
  public String getResultMimetype() {
    return CHART_IMAGE_MIMETYPE;
  }

}
