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
