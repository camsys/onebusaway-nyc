package org.onebusaway.nyc.transit_data_manager.barcode;

import java.awt.Image;

/**
 * This is designed to specify only the interface
 * for actually generating a barcode from a string.
 * The string will have preformatted to match
 * standard barcode formats. (see http://code.google.com/p/zxing/wiki/BarcodeContents)
 * for more info on that part.
 * 
 * @author sclark
 *
 */
public interface StringToImageBarcodeGenerator {
  /**
   * Generate the barcode image from input text
   * @param width width of requested barcode image, in pixels
   * @param height height of requested barcode image, in pixels
   * @param bcText The String to be encoded in the barcode
   * @return The resulting barcode as an Image.
   * @throws Exception 
   */
  Image generateBarcode(int width, int height, String bcText) throws Exception;
  
  /**
   * Get the mimetype of the resulting image.
   * @return
   */
  String getResultMimetype();
}
