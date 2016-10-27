package org.onebusaway.nyc.util.impl.vtw;

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

public class PullOutApiLibrary {

  private static Logger _log = LoggerFactory.getLogger(PullOutApiLibrary.class);

  private String _vehiclePipoHostname = null;

  private Integer _vehiclePipoPort = 443;

  private String _apiEndpointPath = "/yardtrek-cron/";
  
  private Boolean _ssl = Boolean.TRUE;
  
  private RestApiLibrary _restApiLibrary;

  /**
    * Constructor injection necessary due to the usage of RestApiLibrary.
    */
  public PullOutApiLibrary(String hostname, Integer port, String path, Boolean ssl) {
      _vehiclePipoHostname = hostname;
      if (port != null) {
    	  _vehiclePipoPort = port;
      }

      if (StringUtils.isNotEmpty(path)) {
        _apiEndpointPath = path;
      }
      
      if(ssl != null){
    	  _ssl = ssl;
      }

      _log.info("Vehicle Pipo API hostname = " + _vehiclePipoHostname);

      if (!StringUtils.isBlank(_vehiclePipoHostname))
        _restApiLibrary = new RestApiLibrary(_vehiclePipoHostname, _vehiclePipoPort, _apiEndpointPath);
      else
        _log.warn("No TDM URL given!");
  }

  public String getContentsOfUrlAsString(String baseObject, String... params)
      throws Exception {
    if (_restApiLibrary == null)
      return null;
    
    URL requestUrl;
    
    if(_ssl = true)
    	requestUrl = _restApiLibrary.buildSSLUrl(baseObject, params);
    else
    	requestUrl = _restApiLibrary.buildUrl(baseObject, params);
    
    _log.debug("Requesting " + requestUrl);
    
    String response = _restApiLibrary.getContentsOfUrlAsString(requestUrl);
    return response;
  }
  
  public URL buildUrl(String baseObject, String... params) throws Exception {
    return _restApiLibrary.buildUrl(baseObject, params);
  }
  
  public URL buildSSLUrl(String baseObject, String... params) throws Exception {
    return _restApiLibrary.buildSSLUrl(baseObject, params);
  }
  
  public String log(String baseObject, String component, Integer priority, String message) {
	  return _restApiLibrary.log(baseObject, component, priority, message);
  }

}
