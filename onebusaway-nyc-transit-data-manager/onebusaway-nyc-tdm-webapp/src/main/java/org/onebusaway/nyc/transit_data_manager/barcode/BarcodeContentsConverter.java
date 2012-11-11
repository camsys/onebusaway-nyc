package org.onebusaway.nyc.transit_data_manager.barcode;

/**
 * An interface designed to represent the different types of barcode contents
 * as explained in
 * http://code.google.com/p/zxing/wiki/BarcodeContents
 * 
 * In general, call a function such as contentsForUrl or contentsForContact
 * and the barcode representation will be returned, ready for encoding.
 * 
 * I'll just start with the URL as that's all we will use for now.
 * 
 * @author sclark
 *
 */
public interface BarcodeContentsConverter {
  /**
   * Returns an url is it should be encoded in a barcode.
   * @param url the valid url to be put into a barcode
   * @return The contents representing the url, or an empty string if the url is malformed. 
   */
  String contentsForUrl(String url) ;
  
  /**
   * Checks if the contents will fit into a V2 barcode.
   * @param contents The contents to check, typically the result of one of the other methods in this object
   * @return boolean True if the contents will fit inside a V2 barcode.
   */
  public boolean fitsV2QrCode(QRErrorCorrectionLevel ecLevel, String contents);
}
