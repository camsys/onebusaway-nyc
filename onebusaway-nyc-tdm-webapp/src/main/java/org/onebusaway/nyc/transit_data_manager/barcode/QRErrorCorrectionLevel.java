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
 * The level of error correction to be included in the barcode.
 * Specified in terms of what percentage of data loss can be recovered from.
 * 
 * <p>
 * QR codes support four levels of error correction to enable recovery of 
 * missing, misread, or obscured data. Greater redundancy is achieved at 
 * the cost of being able to store less data. See the appendix for details. 
 * Here are the supported values:
 * <ul>
 * <li>L - Allows recovery of up to 7% data loss</li>
 * <li>M - Allows recovery of up to 15% data loss</li>
 * <li>Q - Allows recovery of up to 25% data loss</li>
 * <li>H - Allows recovery of up to 30% data loss</li>
 * </ul>
 * 
 * 
 * </p>
 * @author sclark
 *
 */
public enum QRErrorCorrectionLevel {
  /**
   * Recovery up to 7% data loss
   */
  L ('L'),
  /**
   * Recovery up to 15% data loss
   */
  M ('M'),
  /**
   * Recovery up to 25% data loss
   */
  Q ('Q'),
  /**
   * Recovery up to 30% data loss
   */
  H ('H');
  
  private final char errorCorrectionLevelChar;
  
  QRErrorCorrectionLevel(char errorCorrectionLevelChar) {
    this.errorCorrectionLevelChar = errorCorrectionLevelChar;
  }

  public char getErrorCorrectionLevelChar() {
    return errorCorrectionLevelChar;
  }
}
