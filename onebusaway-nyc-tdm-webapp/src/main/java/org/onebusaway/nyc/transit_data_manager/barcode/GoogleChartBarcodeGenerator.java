/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.transit_data_manager.barcode;

import java.awt.image.RenderedImage;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import javax.imageio.ImageIO;

/**
 * Generates QR Codes using the Google Charts Infographics service.
 * 
 * @author sclark
 * 
 */
@Deprecated
public class GoogleChartBarcodeGenerator extends QrCodeGenerator {

  public GoogleChartBarcodeGenerator() {
    super();
  }

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
   * documentation. chs - Size of the image in pixels, in the format
   * <width>x<height> cht - Type of image: 'qr' means QR code. chl - The data to
   * encode. Must be URL-encoded. choe - How to encode the data in the QR code.
   * Here are the available values: UTF-8 [Default], Shift_JIS, ISO-8859-1 chld
   * - value is <error_correction_level>|<margin>, where error_correction_level
   * is one of LMQH and margin is an int in rows with a default of 4
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

  private static long CHART_MAX_PIXELS = 300000;

  @Override
  protected RenderedImage generateBarcode(String text, int width, int height, int quietZoneRows)
      throws Exception {
    if (checkRequestImageSizeIsTooLarge(width, height)) {
      throw new IllegalArgumentException(
          "Requested Image size exceeds Google Chart maximum of 300000 pixels.");
    }

    RenderedImage resultImage = null;

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
      urlBldr.append(CHART_PARAMNAME_EC_MARGN
          + getEcLevelWithMarginParamValue(getEcLevel(), quietZoneRows));
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

  private String getEcLevelWithMarginParamValue(QRErrorCorrectionLevel ecLevel, int margin) {
    String paramValue = CHART_PARAMVALUE_EC_MARGN_REPLACE;

    paramValue = paramValue.replaceAll(EC_LEVEL_REPLACE,
        String.valueOf(ecLevel.getErrorCorrectionLevelChar()));
    paramValue = paramValue.replaceAll(MARGIN_REPLACE, String.valueOf(margin));

    return paramValue;
  }

  private boolean checkRequestImageSizeIsTooLarge(int width, int height) {
    boolean isTooLarge = false;

    long totalPixels = width * height;

    if (totalPixels > CHART_MAX_PIXELS)
      isTooLarge = true;

    return isTooLarge;
  }

  
}
