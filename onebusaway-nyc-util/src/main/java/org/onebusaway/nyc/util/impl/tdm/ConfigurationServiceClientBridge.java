package org.onebusaway.nyc.util.impl.tdm;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class ConfigurationServiceClientBridge implements ConfigurationServiceClient {

	private static Logger _log = LoggerFactory.getLogger(ConfigurationServiceClientBridge.class);
	
	private ConfigurationServiceClient _tdmClient;
	private ConfigurationServiceClient _fileClient;
	private HashMap<String, Object> _config = null;

	public ConfigurationServiceClientBridge() {
		// default no-op constructor
	}
	/**
	 * Constructor to support spring replacement of ConfigurationServiceTDMImpl 
	 */
	public ConfigurationServiceClientBridge(String hostname, Integer port,
			String path) {
		_tdmClient = new ConfigurationServiceClientTDMImpl(hostname, port, path);
	}

	@PostConstruct
	private void setup() {
		if (getConfig() != null) {
			_log.info("found config");
			if (getConfig().containsKey("oba") && ((HashMap<String, Object>) _config.get("oba")).containsKey("config")) {
				String localConfig = (String) ((HashMap<String, Object>) _config.get("oba")).get("config");
				if ("local".equals(localConfig)) {
					_log.info("using File Based Config");
					_fileClient = new ConfigurationServiceClientFileImpl();
				} else {
					_log.info("local config disabled");
				}
			} else {
				_log.info("local config not specified, ignoring");
			}
		} else {
			_log.info("using TDM Based Config");
		}
	}
	
	@Override
	public URL buildUrl(String baseObject, String... params) throws Exception {
		if (_fileClient != null) {
			return _fileClient.buildUrl(baseObject, params);
		}
		return _tdmClient.buildUrl(baseObject, params);
	}

	@Override
	public void setConfigItem(String baseObject, String component, String key,
			String value) throws Exception {
		if (_fileClient != null) {
			_fileClient.setConfigItem(baseObject, component, key, value);
		} else {
			_tdmClient.setConfigItem(baseObject, component, key, value);
		}
	}

	@Override
	public String log(String baseObject, String component, Integer priority,
			String message) {
		if (_fileClient != null) {
			return _fileClient.log(baseObject, component, priority, message);
		} 
		return _tdmClient.log(baseObject, component, priority, message);
	}

	@Override
	public List<JsonObject> getItemsForRequest(String baseObject,
			String... params) throws Exception {
		if (_fileClient != null) {
			return _fileClient.getItemsForRequest(baseObject, params);
		}
		return _tdmClient.getItemsForRequest(baseObject, params);
	}

	@Override
	public List<Map<String, String>> getItems(String baseObject,
			String... params) throws Exception {
		if (_fileClient != null) {
			return _fileClient.getItems(baseObject, params);
		}
		return _tdmClient.getItems(baseObject, params);
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
	  
	@Override
	public String getItem(String baseObject, String key) throws Exception {
		if (_fileClient != null) {
			return _fileClient.getItem(baseObject, key);
		}
		return _tdmClient.getItem(baseObject, key);
	}

}
