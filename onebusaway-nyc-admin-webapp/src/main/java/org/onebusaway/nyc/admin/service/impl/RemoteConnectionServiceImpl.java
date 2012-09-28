package org.onebusaway.nyc.admin.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.onebusaway.nyc.admin.service.RemoteConnectionService;

import org.h2.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteConnectionServiceImpl implements RemoteConnectionService{

	private static Logger log = LoggerFactory.getLogger(RemoteConnectionServiceImpl.class);
	
	@Override
	public String getContent(String url) {
		HttpURLConnection connection = null;
		String content = null;
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.setReadTimeout(10000);
			content = fromJson(connection);
		} catch (MalformedURLException e) {
			log.error("Exception connecting to " +url + ". The url might be malformed");
			e.printStackTrace();
		} catch (IOException e) {
			log.error("Exception connecting to " +url + ". Exception : " +e);
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		return content;
	}
	
	private String fromJson(HttpURLConnection connection) {
		try {
		  ByteArrayOutputStream baos = new ByteArrayOutputStream();
		  IOUtils.copy(connection.getInputStream(), baos);
			return baos.toString();
		} catch (IOException e) {
			 log.error("fromJson caught exception:", e);
			e.printStackTrace();
		}
		return null;
	}

}
