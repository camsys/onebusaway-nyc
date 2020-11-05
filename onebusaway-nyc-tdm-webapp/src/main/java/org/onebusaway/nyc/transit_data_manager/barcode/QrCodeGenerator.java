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

/**
 * Abstract class for generating QR codes. Made abstract thinking that we may
 * likely use ZXing instead of Google Charts at some point in the future and we
 * will share some of these methods.
 * 
 * @author sclark
 * 
 */
public abstract class QrCodeGenerator {

  private static int DEFAULT_QUIET_ZONE_ROWS = 4;
  
  public QrCodeGenerator() {
    super();

    setEcLevel(QRErrorCorrectionLevel.Q); // Default to EC Level Q (25%)
  }

  private QRErrorCorrectionLevel ecLevel;

  protected QRErrorCorrectionLevel getEcLevel() {
    return ecLevel;
  }

  public void setEcLevel(QRErrorCorrectionLevel ecLevel) {
    this.ecLevel = ecLevel;
  }

  /**
   * Generate a barcode from input text. Assume that input has already been checked
   * to not exceed length constraints.
   * @param bcText The STring to be encoded
   * @param width width of requested image
   * @param height height of requested image
   * @return A Rendered Image containing the centered barcode and safe margin, potentially including additional margin to keep everything even. 
   */
  public RenderedImage generateCode(String bcText, int width, int height) {
    return generateCode(bcText, width, height, DEFAULT_QUIET_ZONE_ROWS);
  }
  
  /**
   * Generates a barcode from input text. Assumes that the input has already
   * been checked to not exceed length constraints.
   * 
   * @param width width of requested barcode image, in pixels
   * @param height height of requested barcode image, in pixels
   * @param bcText The String to be encoded in the barcode
   * @param quietZoneRows The number of rows to use as the quiet zone. This is the guaranteed minimum. The recomended/required number is 4.
   * @return a RenderedImage containing the barcode, or null if unsuccesful.
   */
  public RenderedImage generateCode(String bcText, int width, int height, int quietZoneRows) {
    RenderedImage resultImage = null;

    try {
      resultImage = generateBarcode(bcText, width, height, quietZoneRows);
    } catch (Exception e) {
      resultImage = null;
    }

    return resultImage;
  }

  protected abstract RenderedImage generateBarcode(String text, int width,
      int height, int quietZoneRows) throws Exception;
}
