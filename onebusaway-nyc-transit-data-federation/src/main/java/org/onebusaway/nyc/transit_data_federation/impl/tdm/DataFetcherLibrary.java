package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DataFetcherLibrary {

	private static final String _tdmHostname = "tdm.staging.obanyc.com";

	private static final String _apiEndpointPath = "/api/";

	private static URL buildUrl(String baseObject, String... params) throws Exception {
		String url = _apiEndpointPath;

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
		
		return new URI("http", _tdmHostname, url, null).toURL();		
	}	
	
	public static ArrayList<JsonObject> getItemsForRequest(String baseObject, String... params) throws Exception {
		
		URL requestUrl = buildUrl(baseObject, params);
		URLConnection conn = requestUrl.openConnection();
		
		InputStream inStream = conn.getInputStream();
		BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
		JsonParser parser = new JsonParser();
	    JsonObject response = (JsonObject)parser.parse(br);

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
	    
	    br.close();
		inStream.close();
		
		return output;
	}
}
