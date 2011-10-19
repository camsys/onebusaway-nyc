package org.onebusaway.nyc.transit_data_manager.barcode;

import java.awt.Image;
import java.util.Hashtable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class ZXingBarcodeGenerator extends QrCodeGenerator {

  public ZXingBarcodeGenerator() {
    super();
  }

  private String charSetStr = "UTF-8";
  private ErrorCorrectionLevel eCorrLevel = null;

  private ErrorCorrectionLevel getErrorCorrectionLevel() {
    return eCorrLevel;
  }

  @Override
  public Image generateV2Code(int width, int height, String bcText)
      throws Exception {

    QRCodeWriter writer = new QRCodeWriter();

    Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
    hints.put(EncodeHintType.ERROR_CORRECTION, getErrorCorrectionLevel());
    hints.put(EncodeHintType.CHARACTER_SET, charSetStr);

    BitMatrix matrix = writer.encode(bcText, BarcodeFormat.QR_CODE, width,
        height, hints);

    Image resultImg = MatrixToImageWriter.toBufferedImage(matrix);

    return resultImg;
  }

  protected void setECorrectionL() {
    super.setECorrectionL();
    eCorrLevel = ErrorCorrectionLevel.L;
  }

  protected void setECorrectionM() {
    super.setECorrectionM();
    eCorrLevel = ErrorCorrectionLevel.M;
  }

  protected void setECorrectionQ() {
    super.setECorrectionQ();
    eCorrLevel = ErrorCorrectionLevel.Q;
  }

  protected void setECorrectionH() {
    super.setECorrectionH();
    eCorrLevel = ErrorCorrectionLevel.H;
  }

}
