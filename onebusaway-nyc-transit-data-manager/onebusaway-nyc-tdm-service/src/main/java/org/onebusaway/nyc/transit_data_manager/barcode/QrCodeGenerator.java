package org.onebusaway.nyc.transit_data_manager.barcode;

import java.awt.Image;

/**
 * Interface to generate QR codes.
 * @author sclark
 *
 */
public abstract class QrCodeGenerator {
  
  public QrCodeGenerator() {
    super();
    setDefaultErrorCorrectionLevel();
  }

  private char ecLevel;
  
  protected char getEcLevel () {
    return ecLevel;
  }
  
  /**
   * Set the error checking level for this generator.
   * This means allow recovery of up to x% data loss
   * L - 7%
   * M - 15%
   * Q - 25%
   * H - 30%
   * @param levelChar One of the above characters, representing an EC level.
   * @throws IllegalArgumentException when levelChar is not in the set [L,M,Q,H]
   */
  public void setErrorLevel(char levelChar) throws IllegalArgumentException {
    char level = Character.toUpperCase(levelChar);
    
    if ('L' == level) {
      setECorrectionL();
    } else if ('M' == level) {
      setECorrectionM();
    }  else if ('Q' == level) {
      setECorrectionQ();
    } else if ('H' == level) {
      setECorrectionH();
    } else {
      throw new IllegalArgumentException("Invalid QR Code EC level. Must be one of L, M, Q, H.");
    }
  }
  
  protected void setECorrectionL() { ecLevel = 'L'; }
  protected void setECorrectionM() { ecLevel = 'M'; }
  protected void setECorrectionQ() { ecLevel = 'Q'; }
  protected void setECorrectionH() { ecLevel = 'H'; }
  
  /**
   * Generate a version 2 QR code (25x25 modules) from input text. This 
   * method first checks to make sure that we don't exceed the maximum
   * length to be stored in a V2 code, which is determined by the EC level:
   * L - 77 digits, or 47 chars
   * M - 63 digits, or 38 chars
   * Q - 48 digits, or 29 chars
   * H - 34 digits, or 20 chars
   * @param width width of requested barcode image, in pixels
   * @param height height of requested barcode image, in pixels
   * @param bcText The String to be encoded in the barcode
   * @return
   * @throws Exception 
   */
  public abstract Image generateV2Code(int width, int height, String bcText) throws Exception;
  
  private void setDefaultErrorCorrectionLevel() {
    ecLevel = 'Q';
    setECorrectionQ();
  }
}
