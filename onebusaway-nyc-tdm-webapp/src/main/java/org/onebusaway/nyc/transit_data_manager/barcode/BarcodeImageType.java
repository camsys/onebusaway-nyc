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
 * The image type of the barcode, as well as the
 * associated mime type and potentially additional related
 * information.
 * @author sclark
 *
 */
public enum BarcodeImageType {
  /**
   * Bitmap/BMP files.
   */
  BMP ("bmp", "image/x-ms-bmp"),
  /**
   * PNG files.
   */
  PNG ("png", "image/png");
  
  private final String formatName;
  private final String mimeType;
  
  BarcodeImageType(String formatName, String mimeType) {
    this.formatName = formatName;
    this.mimeType = mimeType;
  }

  public String getFormatName() {
    return formatName;
  }

  public String getMimeType() {
    return mimeType;
  }
}
