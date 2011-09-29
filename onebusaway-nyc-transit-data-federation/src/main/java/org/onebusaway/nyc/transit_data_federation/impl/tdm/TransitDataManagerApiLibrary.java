package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.onebusaway.nyc.transit_data_federation.impl.RestApiLibrary;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TransitDataManagerApiLibrary {

  private static final String _tdmHostname = "tdm.staging.obanyc.com";

  private static final Integer _tdmPort = 80;

  private static final String _apiEndpointPath = "/api/";

  private static final RestApiLibrary _restApiLibrary = new RestApiLibrary(_tdmHostname, _tdmPort, _apiEndpointPath);

  public static void executeApiMethodWithNoResult(String baseObject, String... params) throws Exception {
    _restApiLibrary.executeApiMethodWithNoResult(baseObject, params);
  }

  public static ArrayList<JsonObject> getItemsForRequest(String baseObject, String... params) throws Exception {		

    URL requestUrl = _restApiLibrary.buildUrl(baseObject, params);
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
