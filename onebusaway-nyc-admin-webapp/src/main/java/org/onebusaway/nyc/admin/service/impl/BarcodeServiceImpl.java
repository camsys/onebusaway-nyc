package org.onebusaway.nyc.admin.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.onebusaway.nyc.admin.service.BarcodeService;
import org.onebusaway.nyc.admin.service.RemoteConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BarcodeServiceImpl implements BarcodeService {

	private RemoteConnectionService remoteConnectionService;
	
	@Override
	public InputStream getQRCodesInBatch(File stopIdFile, int dimensions) throws IOException{
		String tdmHost = System.getProperty("tdm.host");
		String url = buildURL(tdmHost, "/barcode/batchGen?img-dimension=", dimensions);
		InputStream barCodeZip = remoteConnectionService.postBinaryData(url, stopIdFile, InputStream.class);
		return barCodeZip;
	}
	
	private String buildURL(String host, String api, int dimensionParam) {
		 return "http://" + host + "/api" + api + String.valueOf(dimensionParam);
	}

	/**
	 * @param remoteConnectionService the remoteConnectionService to set
	 */
	@Autowired
	public void setRemoteConnectionService(
			RemoteConnectionService remoteConnectionService) {
		this.remoteConnectionService = remoteConnectionService;
	}

}
