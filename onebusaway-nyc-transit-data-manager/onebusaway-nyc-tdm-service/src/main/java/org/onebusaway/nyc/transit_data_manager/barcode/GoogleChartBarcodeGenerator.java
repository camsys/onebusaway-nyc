package org.onebusaway.nyc.transit_data_manager.barcode;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.MimeType;
import javax.imageio.ImageIO;

/**
 * Generates QR Codes using the Google Charts Infographics service.
 * 
 * @author sclark
 * 
 */
public class GoogleChartBarcodeGenerator implements QrCodeGenerator {

  private static String URL_ENCODING_NAME = "UTF-8";

  private static String HEIGHT_REPLACE = "__HHH__";
  private static String WIDTH_REPLACE = "__WWW__";
  
  private static String EC_LEVEL_REPLACE = "__EC__";
  private static String MARGIN_REPLACE = "__MM__";

  /*
   * Google documentation, 20111007 Taken from
   * http://code.google.com/apis/chart/infographics/docs/overview.html
   * 
   * NOTE also (added 20111010) that google states how their generator works at:
   * http://code.google.com/apis/chart/infographics/docs/qr_codes.html
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
   * documentation. 
   * chs - Size of the image in pixels, in the format <width>x<height> 
   * cht - Type of image: 'qr' means QR code.
   * chl - The data to encode. Must be URL-encoded.
   * choe - How to encode the data in the QR code. Here are the 
   * available values: UTF-8 [Default], Shift_JIS, ISO-8859-1
   * chld - value is <error_correction_level>|<margin>, where 
   * error_correction_level is one of LMQH and margin is an int in rows with a default of 4
   */

  private static String CHART_ROOT_URL = "https://chart.googleapis.com/chart";
  private static String CHART_PARAMNAME_TYPE = "cht=";
  private static String CHART_PARAMNAME_SIZE = "chs=";
  private static String CHART_PARAMNAME_DATA = "chl=";
  private static String CHART_PARAMNAME_ENCDNG = "choe=";
  private static String CHART_PARAMNAME_EC_MARGN = "chld=";
  private static String CHART_PARAMVALUE_TYPE_QR = "qr";
  private static String CHART_PARAMVALUE_SIZE_REPLACE = "__WWW__x__HHH__";
  private static String CHART_PARAMVALUE_EC_MARGN_REPLACE = "__EC__|__MM__";
  
  private static int CHART_PARAMVALUE_MARGN_DEFAULT = 4;
  private static long CHART_MAX_PIXELS = 300000;

  // My max character arrays, two columns. first digit maxes then alphanum maxes. in LMQH order.
  private static int[][] v2MaxCharsArray = new int[][] {{77, 63, 48, 34}, {47, 38, 29, 20}};
  
  private static String CHART_IMAGE_MIMETYPE = "image/png";

  private char ecLevel = 'Q';
  
  private Image generateBarcode(int width, int height, String text)
      throws Exception {
    if (checkRequestImageSizeIsTooLarge(width, height)) {
      throw new Exception("Requested Image size exceeds Google Chart maximum of 300000 pixels.");
    }
    
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
      urlBldr.append(CHART_PARAMNAME_EC_MARGN + getEcLevelMarginParamValue(ecLevel, CHART_PARAMVALUE_MARGN_DEFAULT));
      urlBldr.append("&");
      urlBldr.append(CHART_PARAMNAME_DATA);
      
      urlBldr.append(urlEncodedText);

      url = urlBldr.toString();

      URL qrRequestUrl = new URL(url);

      resultImage = ImageIO.read(qrRequestUrl);

    } catch (UnsupportedEncodingException unsupEncE) {
      throw new Exception("URLEncoder.encode could not deal with "
          + URL_ENCODING_NAME + " encoding", unsupEncE);
    } catch (MalformedURLException malUrlE) {
      throw new Exception("URL could not deal with the url " + url
          + ". Check the protocol of the url.", malUrlE);
    }

    return resultImage;
  }

  private String getSizeParamValue(int width, int height) {
    String paramValue = CHART_PARAMVALUE_SIZE_REPLACE;

    paramValue = paramValue.replaceAll(WIDTH_REPLACE, String.valueOf(width));
    paramValue = paramValue.replaceAll(HEIGHT_REPLACE, String.valueOf(height));

    return paramValue;
  }
  
  private String getEcLevelMarginParamValue (char ecLevel, int margin) {
    String paramValue = CHART_PARAMVALUE_EC_MARGN_REPLACE;
    
    paramValue = paramValue.replaceAll(EC_LEVEL_REPLACE, String.valueOf(ecLevel));
    paramValue = paramValue.replaceAll(MARGIN_REPLACE, String.valueOf(margin));
    
    return paramValue;
  }

  @Override
  public String getResultMimetype() {
    return CHART_IMAGE_MIMETYPE;
  }

  @Override
  public void setErrorLevel(char levelChar) throws Exception {
    char level = Character.toUpperCase(levelChar);
    
    if ('L' == level || 'M' == level || 'Q' == level || 'H' == level) {
      ecLevel = level;
    } else {
      throw new Exception("Invalid QR Code EC level. Must be one of L, M, Q, H.");
    }
  }

  @Override
  public Image generateV2Code(int width, int height, String bcText) throws Exception {
    // First need to check that we don't have too many characters
    if (checkHasTooManyChars(v2MaxCharsArray, bcText)) {
      throw new Exception("Too many characters in input.");
    }
    
    // Now we should be able to generate the barcode just fine.
    return generateBarcode(width, height, bcText);
  }

  /**
   * Check to see if there are too many characters for this error checking level and QR code version/size.
   * @param charLimitsArray A two dimensional array with the first column holding the max for digits only and
   * the second for the maxes for alphanumeric chars. The rows are ordered by EC level, index 0-3 representing LMQH.
   * @param input the text to check for too many chars
   * @return true, if there are too many characters in the input.
   */
  private boolean checkHasTooManyChars (int [][] charLimitsArray, String input) {
    int inputLength = input.length();
    
    boolean isNumeric = false;
    
    // i'll try a regular expression and see if it matches the entire string.
    Pattern digitPattern = Pattern.compile("\\A[0-9]*\\z");
    Matcher inputDigitMatcher = digitPattern.matcher(input);
    
    if(inputDigitMatcher.matches()) {
      isNumeric = true;
    }
    
    // the array representing the max number of chars in this order: LMQH
    int[] maxChars = isNumeric ? charLimitsArray[0] : charLimitsArray[1];
    
    int charMax = 0;
    if (ecLevel == 'L') {
      charMax = maxChars[0];
    } else if (ecLevel == 'M') {
      charMax = maxChars[1];
    } else if (ecLevel == 'Q') {
      charMax = maxChars[2];
    } else if (ecLevel == 'H') {
      charMax = maxChars[3];
    }
    
    boolean hasTooManyChars = true;
    if (inputLength > charMax) {
      hasTooManyChars = true;
    } else {
      hasTooManyChars = false;
    }
    
    return hasTooManyChars;
  }
  
  private boolean checkRequestImageSizeIsTooLarge(int width, int height) {
    boolean isTooLarge = false;
    
    long totalPixels = width * height;
    
    if (totalPixels > CHART_MAX_PIXELS) isTooLarge = true;
    
    return isTooLarge;
  }
}
