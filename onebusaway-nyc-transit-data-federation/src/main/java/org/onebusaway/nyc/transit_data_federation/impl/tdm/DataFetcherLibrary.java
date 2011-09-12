package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.onebusaway.nyc.transit_data_federation.impl.tdm.model.ConfigurationItem;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class DataFetcherLibrary {

	private static final String _tdmHostname = "ec2-50-17-46-3.compute-1.amazonaws.com";

	private static final String _apiEndpointPath = "/api/";

	private static Gson _gson = new Gson();

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

	private static Serializable responseItemFactory(String type, JsonElement item) {
		
		if(type.equals("config")) {
			return _gson.fromJson(item, ConfigurationItem.class);
		}
		
		return null;
	}
	
	public static ArrayList<? extends Serializable> 
			getItemsForRequest(String baseObject, String... params) throws Exception {
		
		ArrayList<Serializable> output = new ArrayList<Serializable>();

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

	    // find "content" in the response
	    for(Entry<String,JsonElement> item : response.entrySet()) {
	    	String type = item.getKey();
	    	JsonElement responseItemWrapper = item.getValue();
	    	
	    	if(type.equals("status"))
	    		continue;
	    	
	    	// our response "body" is always an array of things
	    	JsonArray responseItems = responseItemWrapper.getAsJsonArray();
	    	for(JsonElement responseItem : responseItems) {
	    		output.add(responseItemFactory(type, responseItem));	    		
	    	}
	    }
	    
	    br.close();
		inStream.close();
		
		return output;
	}
}
