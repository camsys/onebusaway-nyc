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

		Set<MtaBarcode> bcList = new HashSet<MtaBarcode>();

		MtaBarcode bcOne = new MtaBarcode("HTTP://BT.MTA.INFO/S/10");
		bcOne.setStopIdStr("10");
		bcList.add(bcOne);

		MtaBarcode bcTwo = new MtaBarcode("HTTP://BT.MTA.INFO/S/20");
		bcTwo.setStopIdStr("20");
		bcList.add(bcTwo);

		BufferedImage result1a = (BufferedImage) google.generateCode(bcOne.getContents(), 100, 100);
		BufferedImage result1b = (BufferedImage) zxing.generateCode(bcOne.getContents(), 100, 100);
		BufferedImage result2a = (BufferedImage) google.generateCode(bcTwo.getContents(), 100, 100);
		BufferedImage result2b = (BufferedImage) zxing.generateCode(bcTwo.getContents(), 100, 100);
		BufferedImage result2c = (BufferedImage) zxing.generateCode(bcTwo.getContents(), 200, 200);

		assertTrue(bufferedImagesEqual(result1a,result1b));
		assertTrue(bufferedImagesEqual(result2a,result2b));
		assertFalse(bufferedImagesEqual(result1a,result2a));
		assertFalse(bufferedImagesEqual(result1b,result2b));
		assertFalse(bufferedImagesEqual(result2b,result2c));

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
