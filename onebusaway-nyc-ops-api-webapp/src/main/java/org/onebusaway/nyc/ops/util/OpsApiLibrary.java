package org.onebusaway.nyc.ops.util;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.report.impl.MinRecordsException;
import org.onebusaway.nyc.util.impl.RestApiLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class OpsApiLibrary {

  private static Logger _log = LoggerFactory.getLogger(OpsApiLibrary.class);

  private final int MAX_RETRIES = 3;
  
  private final int MIN_RECORDS = 3000; //TODO - This value should come from TDM Config
  
  private final int OPS_READ_TIMEOUT = 5 * 60 * 1000; // 5 Min Timeout
  
  private final int OPS_CONNECTION_TIMEOUT = 1 * 60 * 1000; // 1 Min Timeout
  
  private String _opsHostname = null;

  private Integer _opsPort = 80;
  
  private String _opsApiEndpointPath = "/api/record/";
  
  private String _archiveHostname = null;
  
  private Integer _archivePort = 80;
  
  private String _archiveApiEndpointPath = "/api/record/";
  
  private RestApiLibrary _restApiLibrary;

  /**
    * Constructor injection necessary due to the usage of RestApiLibrary.
    */
  public OpsApiLibrary(String opsHostname, Integer opsPort, String opsPath, 
		  String archiveHostname, Integer archivePort, String archivePath) {
	  // Ops Settings
	  _opsHostname = opsHostname;
      if (opsPort != null) {
        _opsPort = opsPort;
      }

      _log.info("Ops hostname = " + _opsHostname);
      
      if (opsPath != null) {
          _opsApiEndpointPath = opsPath;
      }
      
      // Archive Settings
      _archiveHostname = archiveHostname;
      if (archivePort != null) {
        _archivePort = archivePort;
      }
      
      _log.info("Archive hostname = " + _archiveHostname);

      if (archivePath != null) {
    	  _archiveApiEndpointPath = archivePath;
      }
      

      if (!StringUtils.isBlank(_opsHostname)){
        _restApiLibrary = new RestApiLibrary(_opsHostname, _opsPort, _opsApiEndpointPath);
        _restApiLibrary.setReadTimeout(OPS_READ_TIMEOUT);
      	_restApiLibrary.setConnectionTimeout(OPS_CONNECTION_TIMEOUT);
      }
      else
        _log.warn("No Ops URL given!");
  }


  public URL buildUrl(String baseObject, String... params) throws Exception {
    return _restApiLibrary.buildUrl(baseObject, params);
  }
    

  public String log(String baseObject, String component, Integer priority, String message) {
	  return _restApiLibrary.log(baseObject, component, priority, message);
  }
  
  public String getItemsForRequestAsString(String baseObject, String... params) throws Exception {
	  return getItemsForRequest(baseObject, params).toString();
  }
  
  public List<JsonObject> getItemsForRequest(String baseObject, String... params) throws Exception {		
    if (_restApiLibrary == null)
      return Collections.emptyList();
    try{ 
    	return getContents(baseObject, params);
    }
    catch (Exception e){
    	_log.error("Attempt to set intial state failed. Will attempt to get the state from Archiver instead.", e);
    	_restApiLibrary = new RestApiLibrary(_archiveHostname, _archivePort, _archiveApiEndpointPath);
    	_restApiLibrary.setReadTimeout(OPS_READ_TIMEOUT);
    	_restApiLibrary.setConnectionTimeout(OPS_CONNECTION_TIMEOUT);
    	return getContents(baseObject, params);
    }
  }
  
  private List<JsonObject> getContents(String baseObject, String... params) throws Exception{
	int connect_attempt = 1;
	URL requestUrl = _restApiLibrary.buildUrl(baseObject, params);
	_log.info("Requesting " + requestUrl);
	while(true){
		try{
			_log.info("Connecting to " + requestUrl.toString());
			String responseJson = _restApiLibrary.getContentsOfUrlAsString(requestUrl);
			List<JsonObject> contents = _restApiLibrary.getJsonObjectsForString(responseJson);
			if(contents.size() < MIN_RECORDS){
				throw new MinRecordsException("Received " + contents.size() + " records which is less than the expected minimum of " + MIN_RECORDS);
			}
			return contents;
		}
		catch (ConnectException ce){
			Thread.sleep(1000); //Wait 1 second between attempts
	    	_log.warn("re-connect attempt " + connect_attempt + " of " + MAX_RETRIES);
	    	if (connect_attempt++ == MAX_RETRIES) throw ce;
		}
		catch (SocketTimeoutException ste){
			throw ste;
	    }
		catch (IOException ioe){
			Thread.sleep(1000); //Wait 1 second between attempts
	    	_log.warn("re-connect attempt " + connect_attempt + " of " + MAX_RETRIES);
	    	if (connect_attempt++ == MAX_RETRIES) throw ioe;
		}
		catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
		
	}
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
