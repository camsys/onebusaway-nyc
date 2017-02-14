package org.onebusaway.nyc.admin.service.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.onebusaway.nyc.admin.service.RemoteConnectionService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.h2.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

@Component
public class RemoteConnectionServiceImpl implements RemoteConnectionService {

	private static Logger log = LoggerFactory.getLogger(RemoteConnectionServiceImpl.class);
	private int CONNECTION_READ_TIMEOUT = 20000;
	private ConfigurationService configurationService;
	
	
	@Autowired
	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}
	
	@Override
	public String getContent(String url) {
		HttpURLConnection connection = null;
		String content = null;
		try {
			connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestMethod("GET");
			connection.setDoOutput(true);
			connection.setReadTimeout(getConnectionReadTimeout());
			content = fromJson(connection);
		} catch (ConnectException e){
			log.error("Connection timed out when connecting to " +url + ". Exception : " +e);
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
	
	private int getConnectionReadTimeout() {
	    if (configurationService != null) {
	        return configurationService.getConfigurationValueAsInteger("admin.connection.readTimeout", CONNECTION_READ_TIMEOUT);
	    }
	    return CONNECTION_READ_TIMEOUT;
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
