package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.transit_data_federation.impl.RestApiLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class TransitDataManagerApiLibrary {

  private static Logger _log = LoggerFactory.getLogger(TransitDataManagerApiLibrary.class);

  private String _tdmHostname = null;

  private Integer _tdmPort = 80;

  private String _apiEndpointPath = "/api/";

  /**
    * Constructor injection necessary due to the usage of RestApiLibrary.
    */
  public TransitDataManagerApiLibrary (String hostname, Integer port, String path) {
      _tdmHostname = hostname;
      if (port != null) {
	  _tdmPort = port;
      }
      if (path != null) {
	  _apiEndpointPath = path;
      }
      _log.error("_tdmHostname=" + _tdmHostname);
      _restApiLibrary = new RestApiLibrary(_tdmHostname, _tdmPort, _apiEndpointPath);
  }

  private RestApiLibrary _restApiLibrary;

  public URL buildUrl(String baseObject, String... params) throws Exception {
    return _restApiLibrary.buildUrl(baseObject, params);
  }
    
  public void executeApiMethodWithNoResult(String baseObject, String... params) throws Exception {
    URL requestUrl = buildUrl(baseObject, params);

    if(!_restApiLibrary.executeApiMethodWithNoResult(requestUrl)) {
      throw new Exception("Error setting configuration value");
    }
  }
  
  public ArrayList<JsonObject> getItemsForRequest(String baseObject, String... params) throws Exception {		
    URL requestUrl = _restApiLibrary.buildUrl(baseObject, params);
    String responseJson = _restApiLibrary.getContentsOfUrlAsString(requestUrl);
    return _restApiLibrary.getJsonObjectsForString(responseJson);
  }

  /**
   * Convenience method. Note this assumes all values coming back from the service are strings.
   */
  public List<Map<String, String>> getItems(String baseObject, String... params) throws Exception {
    List<Map<String, String>> result = new ArrayList<Map<String, String>>();
    ArrayList<JsonObject> items = getItemsForRequest(baseObject, params);
    for(JsonObject item: items) {
      Map<String, String> m = new HashMap<String, String>();
      result.add(m);
      for (Map.Entry<String, JsonElement> entry: item.entrySet()) {
        m.put(entry.getKey(), entry.getValue().getAsString());
      }
    }
    return result;
  }

}
