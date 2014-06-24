package org.onebusaway.nyc.report.util;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.util.impl.RestApiLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class OpsApiLibrary {

  private static Logger _log = LoggerFactory.getLogger(OpsApiLibrary.class);

  private String _opsHostname = null;

  private Integer _opsPort = 8080;

  private String _apiEndpointPath = "/api/record/";

  /**
    * Constructor injection necessary due to the usage of RestApiLibrary.
    */
  public OpsApiLibrary(String hostname, Integer port, String path) {
      _opsHostname = hostname;
      if (port != null) {
        _opsPort = port;
      }

      if (path != null) {
        _apiEndpointPath = path;
      }

      _log.info("Ops hostname = " + _opsHostname);

      if (!StringUtils.isBlank(_opsHostname))
        _restApiLibrary = new RestApiLibrary(_opsHostname, _opsPort, _apiEndpointPath);
      else
        _log.warn("No Ops URL given!");
  }

  private RestApiLibrary _restApiLibrary;

  public URL buildUrl(String baseObject, String... params) throws Exception {
    return _restApiLibrary.buildUrl(baseObject, params);
  }
    
  /*public void setConfigItem(String baseObject, String component, String key, String value) 
		  throws Exception {
	String[] params = {component, key, "set"};  
    if (_restApiLibrary == null)
      return;
    URL requestUrl = buildUrl(baseObject, params);
    _log.info("Requesting " + requestUrl);

    if(!_restApiLibrary.setContents(requestUrl, value)) {
      throw new Exception("Error setting configuration value");
    }
  }*/
  
  public String log(String baseObject, String component, Integer priority, String message) {
	  return _restApiLibrary.log(baseObject, component, priority, message);
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
