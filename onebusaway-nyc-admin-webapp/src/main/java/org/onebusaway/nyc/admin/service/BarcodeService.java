package org.onebusaway.nyc.admin.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Service interface to retrieve qr code images from remote server
 * @author abelsare
 *
 */
public interface BarcodeService {

	/**
	 * Gets QR codes for stop ids specified in csv file. This method retrieves QR codes in a batch as
	 * a zip file from remote server
	 * @param dimensions dimensions of the images
	 * @return input stream of zip file containing qr code images
	 */
	public InputStream getQRCodesInBatch(File stopIdFile, int dimensions) throws IOException;
}
