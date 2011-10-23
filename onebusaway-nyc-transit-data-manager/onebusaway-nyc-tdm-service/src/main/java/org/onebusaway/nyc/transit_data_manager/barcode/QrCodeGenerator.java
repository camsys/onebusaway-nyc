package org.onebusaway.nyc.transit_data_manager.barcode;

import java.awt.image.RenderedImage;

/**
 * Interface to generate QR codes.
 * 
 * @author sclark
 * 
 */
public interface QrCodeGenerator {

  /**
   * Set the error checking level for this generator. This means allow recovery
   * of up to x% data loss L - 7% M - 15% Q - 25% H - 30%
   * 
   * @param levelChar One of the above characters, representing an EC level.
   * @throws Exception
   */
  void setErrorLevel(QRErrorCorrectionLevel errorCorrectionLevel);

  /**
   * Generates a barcode from input text. Assumes that the input has already
   * been checked to not exceed length constraints.
   * 
   * @param width width of requested barcode image, in pixels
   * @param height height of requested barcode image, in pixels
   * @param bcText The String to be encoded in the barcode
   * @return a RenderedImage containing the barcode, or null if unsuccesful.
   */
  RenderedImage generateCode(int width, int height, String bcText);

}
