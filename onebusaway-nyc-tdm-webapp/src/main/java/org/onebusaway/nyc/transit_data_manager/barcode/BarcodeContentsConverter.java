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
