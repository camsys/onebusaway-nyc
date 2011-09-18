package org.onebusaway.nyc.transit_data_federation.impl;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class RestApiLibrary {

	private String _host = null;

	private String _apiPrefix = "/api/";

	public RestApiLibrary(String host, String apiPrefix) {
		_host = host;
		_apiPrefix = apiPrefix;
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
		
		return new URI("http", _host, url, null).toURL();		
	}	
	
	public void executeApiMethodWithNoResult(String baseObject, String... params)
		throws Exception {

		URL requestUrl = buildUrl(baseObject, params);
		HttpURLConnection conn = (HttpURLConnection)requestUrl.openConnection();

		if(conn.getResponseCode() != HttpURLConnection.HTTP_OK)
			throw new Exception("TDM API method call " + requestUrl.toString() + " failed.");
	}
}
