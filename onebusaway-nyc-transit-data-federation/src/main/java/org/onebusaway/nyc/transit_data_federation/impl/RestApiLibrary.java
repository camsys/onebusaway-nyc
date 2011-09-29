package org.onebusaway.nyc.transit_data_federation.impl;

import java.net.HttpURLConnection;
import java.net.URL;

public class RestApiLibrary {

	private String _host = null;

	private String _apiPrefix = "/api/";

	private int _port = 80;
	
	public RestApiLibrary(String host, Integer port, String apiPrefix) {
		_host = host;
		_apiPrefix = apiPrefix;
		if(port != null)
		  _port = port;
	}
	
	public URL buildUrl(String baseObject, String... params) throws Exception {
		String url = _apiPrefix;

		url += baseObject;

		if(params.length > 0) {
			url += "/";
			for(int i = 0; i < params.length; i++) {
				String param = params[i];
				url += param;				
				if(i < params.length - 1)
					url += "/";
			}
		}

		return new URL("http", _host, _port, url);
	}	
	
	public void executeApiMethodWithNoResult(String baseObject, String... params)
		throws Exception {

		URL requestUrl = buildUrl(baseObject, params);
		HttpURLConnection conn = (HttpURLConnection)requestUrl.openConnection();
		
		if(conn.getResponseCode() != HttpURLConnection.HTTP_OK)
			throw new Exception("TDM API method call " + requestUrl.toString() + " failed.");
	}
}
