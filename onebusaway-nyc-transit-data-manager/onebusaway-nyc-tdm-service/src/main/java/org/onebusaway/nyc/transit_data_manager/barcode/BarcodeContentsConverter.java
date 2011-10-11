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
  String contentsForUrl(String url) throws Exception;
}
