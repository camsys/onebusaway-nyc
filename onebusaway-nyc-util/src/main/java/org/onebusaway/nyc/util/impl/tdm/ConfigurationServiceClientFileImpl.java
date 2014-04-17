package org.onebusaway.nyc.util.impl.tdm;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class ConfigurationServiceClientFileImpl implements
		ConfigurationServiceClient {

	private static Logger _log = LoggerFactory
			.getLogger(ConfigurationServiceClientFileImpl.class);
	private HashMap<String, Object> _config = null;
	
	@Override
	public URL buildUrl(String baseObject, String... params) throws Exception {
		// TODO Auto-generated method stub
		_log.info("empty buildUrl");
		return null;
	}

	@Override
	public void setConfigItem(String baseObject, String component, String key,
			String value) throws Exception {
		// TODO Auto-generated method stub
		_log.info("empty setConfigItem");
	}

	@Override
	public String log(String baseObject, String component, Integer priority,
			String message) {
		// TODO Auto-generated method stub
		_log.info("empty log");
		return null;
	}

	@Override
	public List<JsonObject> getItemsForRequest(String baseObject,
			String... params) throws Exception {
		// TODO Auto-generated method stub
		_log.info("empty getItemsForRequest");
		return null;
	}

	@Override
	public List<Map<String, String>> getItems(String baseObject,
			String... params) throws Exception {
		_log.debug("getItems(" + baseObject + ", " + params + ")");
		return (List<Map<String, String>>) getConfig().get("config");
	}

	@Override
	public String getItem(String baseObject, String key) throws Exception {
		List<Map<String, String>> items = getItems("config", "list");
		if (items == null) return null;
		for (Map<String, String> component : items) {
			if (component.containsKey("key") && key.equals(component.get("key"))) {
				return component.get("value");
			}
		}
		return null;
	}

    private HashMap<String, Object> getConfig() {
		  if (_config != null) {
			  return _config;
		  }
		  
		    try {
		        String config = FileUtils.readFileToString(new File(
		            "/var/lib/obanyc/config.json"));
		        HashMap<String, Object> o = new ObjectMapper(new JsonFactory()).readValue(
		            config, new TypeReference<HashMap<String, Object>>() {
		            });
		        _config = o;
		      } catch (Exception e) {
		        _log.info("Failed to get an environment out of /var/lib/obanyc/config.json, continuing without it.");
		      }
		      return _config;

	}

}
