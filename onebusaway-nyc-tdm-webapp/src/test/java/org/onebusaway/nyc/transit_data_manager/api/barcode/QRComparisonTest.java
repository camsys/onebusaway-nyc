package org.onebusaway.nyc.transit_data_manager.api.barcode;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.onebusaway.nyc.transit_data_manager.barcode.*;
import org.onebusaway.nyc.transit_data_manager.barcode.model.MtaBarcode;

public class QRComparisonTest extends QrCodeGeneratorResource {

	@Test
	public void testGenerateBarcodeZipFileFromUrlList() throws IOException {

		QrCodeGenerator google = new GoogleChartBarcodeGenerator();
		QrCodeGenerator zxing = new ZXingCodeGenerator();

		MtaBarcode bcOne = new MtaBarcode("HTTP://BT.MTA.INFO/S/10");
		MtaBarcode bcTwo = new MtaBarcode("HTTP://BT.MTA.INFO/S/20");
		MtaBarcode bcThree = new MtaBarcode("HTTP://BT.MTA.INFO/S/325");

		BufferedImage result1a = (BufferedImage) google.generateCode(bcOne.getContents(), 100, 100);
		BufferedImage result1b = (BufferedImage) zxing.generateCode(bcOne.getContents(), 100, 100);
		BufferedImage result2a = (BufferedImage) google.generateCode(bcTwo.getContents(), 100, 100);
		BufferedImage result2b = (BufferedImage) zxing.generateCode(bcTwo.getContents(), 100, 100);
		BufferedImage result2c = (BufferedImage) zxing.generateCode(bcTwo.getContents(), 200, 200);
		BufferedImage result3a = (BufferedImage) google.generateCode(bcThree.getContents(), 333, 444);
		BufferedImage result3b = (BufferedImage) zxing.generateCode(bcThree.getContents(), 333, 444);
		
		assertTrue(bufferedImagesEqual(result1a,result1b));
		assertTrue(bufferedImagesEqual(result2a,result2b));
		assertTrue(bufferedImagesEqual(result3a,result3b));
		assertFalse(bufferedImagesEqual(result1a,result2a));
		assertFalse(bufferedImagesEqual(result1b,result2b));
		assertFalse(bufferedImagesEqual(result2b,result2c));
		assertFalse(bufferedImagesEqual(result2c,result3a));
		assertFalse(bufferedImagesEqual(result1b,result3b));
	}

	boolean bufferedImagesEqual(BufferedImage img1, BufferedImage img2) {
		if (img1.getWidth() == img2.getWidth() && img1.getHeight() == img2.getHeight() ) {
			for (int x = 0; x < img1.getWidth(); x++) {
				for (int y = 0; y < img1.getHeight(); y++) {
					if (img1.getRGB(x, y) != img2.getRGB(x, y) ) return false;
				}
			}
		}
		else {
			return false;
		}
		return true;
	}
}
