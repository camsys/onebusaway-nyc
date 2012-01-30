package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import org.onebusaway.nyc.transit_data_federation.impl.RestApiLibrary;


import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransitDataManagerApiLibrary {

  private static Logger _log = LoggerFactory.getLogger(TransitDataManagerApiLibrary.class);

  private String _tdmHostname = null;

  private Integer _tdmPort = 80;

  private String _apiEndpointPath = "/api/";

  /**
    * Constructor injection necessary due to the usage of RestApiLibrary.
    */
  public TransitDataManagerApiLibrary(String hostname, Integer port, String path) {
      _tdmHostname = hostname;
      if (port != null) {
        _tdmPort = port;
      }

      if (path != null) {
        _apiEndpointPath = path;
      }

      _log.info("TDM hostname = " + _tdmHostname);

      if (!StringUtils.isBlank(_tdmHostname))
        _restApiLibrary = new RestApiLibrary(_tdmHostname, _tdmPort, _apiEndpointPath);
      else
        _log.warn("No TDM URL given!");
  }

  private RestApiLibrary _restApiLibrary;

  public URL buildUrl(String baseObject, String... params) throws Exception {
    return _restApiLibrary.buildUrl(baseObject, params);
  }
    
  public void executeApiMethodWithNoResult(String baseObject, String... params) throws Exception {
    if (_restApiLibrary == null)
      return;
    URL requestUrl = buildUrl(baseObject, params);
    _log.info("Requesting " + requestUrl);

    if(!_restApiLibrary.executeApiMethodWithNoResult(requestUrl)) {
      throw new Exception("Error setting configuration value");
    }
  }
  
  public List<JsonObject> getItemsForRequest(String baseObject, String... params) throws Exception {		
    if (_restApiLibrary == null)
      return Collections.emptyList();
    URL requestUrl = _restApiLibrary.buildUrl(baseObject, params);
    _log.info("Requesting " + requestUrl);

    String responseJson = _restApiLibrary.getContentsOfUrlAsString(requestUrl);    

    return _restApiLibrary.getJsonObjectsForString(responseJson);
  }

  /**
   * Convenience method. Note this assumes all values coming back from the service are strings.
   */
  public List<Map<String, String>> getItems(String baseObject, String... params) throws Exception {
    if (_restApiLibrary == null)
      return Collections.emptyList();
    List<Map<String, String>> result = new ArrayList<Map<String, String>>();
    List<JsonObject> items = getItemsForRequest(baseObject, params);
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
