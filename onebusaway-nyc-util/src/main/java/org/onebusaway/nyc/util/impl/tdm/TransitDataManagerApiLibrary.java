/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.util.impl.tdm;

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

public class TransitDataManagerApiLibrary {

  private static Logger _log = LoggerFactory.getLogger(TransitDataManagerApiLibrary.class);

  private String _tdmHostname = null;

  private Integer _tdmPort = 80;

  private String _apiEndpointPath = "/api/";


  /**
    * Constructor injection necessary due to the usage of RestApiLibrary.
    */
  public TransitDataManagerApiLibrary(String hostname, Integer port, String path) {
    setupRestApiLibrary(hostname, port, path, null, null);
  }

  public TransitDataManagerApiLibrary(String hostname, Integer port, String path, Integer connectionTimeout, Integer readTimeout) {
    setupRestApiLibrary(hostname, port, path, connectionTimeout, readTimeout);
  }

  private void setupRestApiLibrary(String hostname, Integer port, String path, Integer connectionTimeout, Integer readTimeout) {
    _tdmHostname = hostname;
    if (port != null) {
      _tdmPort = port;
    }

    if (path != null) {
      _apiEndpointPath = path;
    }

    _log.info("TDM hostname = " + _tdmHostname);

    if (!StringUtils.isBlank(_tdmHostname)) {
      _restApiLibrary = new RestApiLibrary(_tdmHostname, _tdmPort, _apiEndpointPath);
      if(connectionTimeout != null) {
        _restApiLibrary.setConnectionTimeout(connectionTimeout);
      }
      if(readTimeout != null){
        _restApiLibrary.setConnectionTimeout(readTimeout);
      }
    }
    else {
      _log.warn("No TDM URL given!");
    }
  }

  private RestApiLibrary _restApiLibrary;

  public URL buildUrl(String baseObject, String... params) throws Exception {
    return _restApiLibrary.buildUrl(baseObject, params);
  }
    
  public void setConfigItem(String baseObject, String component, String key, String value) 
		  throws Exception {
	String[] params = {component, key, "set"};  
    if (_restApiLibrary == null)
      return;
    URL requestUrl = buildUrl(baseObject, params);
    _log.info("Requesting " + requestUrl);

    if(!_restApiLibrary.setContents(requestUrl, value)) {
      throw new Exception("Error setting configuration value");
    }
  }
  
  public String log(String baseObject, String component, Integer priority, String message) {
	  return _restApiLibrary.log(baseObject, component, priority, message);
  }
  
  public List<JsonObject> getItemsForRequest(String baseObject, String... params) throws Exception {    
    String responseJson = getContentsOfUrlAsString(baseObject, params);
    if (responseJson == null)
      return Collections.emptyList();

    return _restApiLibrary.getJsonObjectsForString(responseJson);
  }

  public List<JsonObject> getItemsForRequestNoCheck(String baseObject, String... params) throws Exception {    
    String responseJson = getContentsOfUrlAsString(baseObject, params);    
    if (responseJson == null)
      return Collections.emptyList();

    return _restApiLibrary.getJsonObjectsForStringNoCheck(responseJson);
  }

  private String getContentsOfUrlAsString(String baseObject, String... params)
      throws Exception {
    if (_restApiLibrary == null)
      return null;
    URL requestUrl = _restApiLibrary.buildUrl(baseObject, params);
    _log.info("Requesting " + requestUrl);

    String responseJson = _restApiLibrary.getContentsOfUrlAsString(requestUrl);
    return responseJson;
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
