package org.onebusaway.nyc.util.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Map.Entry;

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
	
  public String getContentsOfUrlAsString(URL requestUrl) throws Exception {
    URLConnection conn = requestUrl.openConnection();
    InputStream inStream = conn.getInputStream();
    BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
    
    StringBuilder output = new StringBuilder();
    while(br.ready()) {
      output.append(br.readLine());
    }

    br.close();
    inStream.close();

    return output.toString();
  }

  public ArrayList<JsonObject> getJsonObjectsForString(String string) throws Exception {
    JsonParser parser = new JsonParser();
    JsonObject response = (JsonObject)parser.parse(string);

    // check status
    if(response.has("status")) {
      if(!response.get("status").getAsString().equals("OK"))
        throw new Exception("Response error: status was not OK");
    } else
      throw new Exception("Invalid response: no status element was found.");

    ArrayList<JsonObject> output = new ArrayList<JsonObject>();

    // find "content" in the response
    for(Entry<String,JsonElement> item : response.entrySet()) {
      String type = item.getKey();
      JsonElement responseItemWrapper = item.getValue();

      if(type.equals("status"))
        continue;

      // our response "body" is always one array of things
      try {
        for(JsonElement arrayElement : responseItemWrapper.getAsJsonArray()) {
          output.add(arrayElement.getAsJsonObject());
        }
      } catch (Exception e) {
        continue;
      }
    }

    return output;
  }
  
	public boolean executeApiMethodWithNoResult(URL requestUrl)
		throws Exception {

		HttpURLConnection conn = (HttpURLConnection)requestUrl.openConnection();
		
		if(conn.getResponseCode() == HttpURLConnection.HTTP_OK)
		  return true;
		else
		  return false;
	}
}
