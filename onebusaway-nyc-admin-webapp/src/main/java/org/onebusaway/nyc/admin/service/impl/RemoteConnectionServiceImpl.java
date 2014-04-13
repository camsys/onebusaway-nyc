package org.onebusaway.nyc.admin.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.onebusaway.nyc.admin.service.RemoteConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

@Component
public class RemoteConnectionServiceImpl implements RemoteConnectionService {

	private static Logger log = LoggerFactory.getLogger(RemoteConnectionServiceImpl.class);
	

	@Override
	public String postContent(String url, Map<String, String> params) {
		HttpURLConnection connection = null;
		String content = null;
		
		try {
			StringBuilder postData = new StringBuilder();
	        for (Entry<String, String> param : params.entrySet()) {
	            if (postData.length() != 0) postData.append('&');
	            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
	            postData.append('=');
	            postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
	        }
	        byte[] postDataBytes = postData.toString().getBytes("UTF-8");

			connection = (HttpURLConnection) new URL(url).openConnection();
	        connection.setRequestMethod("POST");
	        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
	        connection.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
	        connection.setDoOutput(true);
	        connection.getOutputStream().write(postDataBytes);
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
	
	@Override
	public <T> T postBinaryData(String url, File data, Class<T> responseType) {
		T response = null;
		
		ClientConfig config = new DefaultClientConfig();
		Client client = Client.create(config);
		WebResource resource = client.resource(url);
		
		try {
			response = resource.accept("text/csv").type("text/csv")
					.post(responseType, new FileInputStream(data));
		} catch (UniformInterfaceException e) {
			log.error("Unable to read response from the server.");
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			log.error("CSV File not found. It is not uploaded correctly");
			e.printStackTrace();
		}
		
		return response;
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
