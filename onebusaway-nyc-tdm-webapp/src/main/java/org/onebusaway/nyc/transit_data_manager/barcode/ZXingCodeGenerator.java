package org.onebusaway.nyc.transit_data_manager.barcode;

import java.awt.Color;
import java.awt.image.*;
import java.util.Hashtable;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * 
 * Generates QR Codes using the ZXing library.
 * Replacement for the GoogleChartBarcodeGenerator, which uses the Google Charts Infographics 
 * service, which has been officially deprecated as of April 20, 2012
 * 
 * @author altang
 * 
 */

public class ZXingCodeGenerator extends QrCodeGenerator {
	private static String ENCODING_NAME = "UTF-8";

	public ZXingCodeGenerator() {
		super();
	}

	@Override
	protected RenderedImage generateBarcode(String text, int width, int height,
			int quietZoneRows) throws Exception {
		BitMatrix matrix = null;
		Writer writer = new MultiFormatWriter();
		try {
			Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
			hints.put(EncodeHintType.CHARACTER_SET, ENCODING_NAME);
			hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.valueOf(getEcLevel().toString()));	
			hints.put(EncodeHintType.MARGIN, quietZoneRows);
			matrix = writer.encode(text, BarcodeFormat.QR_CODE, width, height, hints);
		} catch (Exception e) {
			e.printStackTrace();
		}
		RenderedImage image = toBufferedImage(matrix);
		return image;
	}

	public static RenderedImage toBufferedImage(BitMatrix matrix) {
		int width = matrix.getWidth();
		int height = matrix.getHeight();
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Color white = new Color(255, 255, 255);
		Color black = new Color(0, 0, 0);
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				image.setRGB(x, y, matrix.get(x, y) ? black.getRGB() : white.getRGB());
			}
		}
		return image;
	}
}