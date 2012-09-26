package org.onebusaway.nyc.admin.service.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

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
		BufferedReader rd;
		try {
			rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = rd.readLine()) != null) {
				sb.append(line + '\n');
			}
			return sb.toString();
		} catch (IOException e) {
			 log.error("fromJson caught exception:", e);
			e.printStackTrace();
		}
		return null;
	}


}
