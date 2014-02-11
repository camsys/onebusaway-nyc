package org.onebusaway.nyc.util.impl.tdm;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.util.configuration.ConfigurationServiceClient;
import org.onebusaway.nyc.util.impl.RestApiLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ConfigurationServiceClientTDMImpl implements
		ConfigurationServiceClient {

	private static Logger _log = LoggerFactory
			.getLogger(ConfigurationServiceClientTDMImpl.class);

	private String _tdmHostname = null;

	private Integer _tdmPort = 80;

	private String _apiEndpointPath = "/api/";

	/**
	 * Constructor injection necessary due to the usage of RestApiLibrary.
	 */
	public ConfigurationServiceClientTDMImpl(String hostname, Integer port,
			String path) {
		_tdmHostname = hostname;
		if (port != null) {
			_tdmPort = port;
		}

		if (path != null) {
			_apiEndpointPath = path;
		}

		_log.info("TDM hostname = " + _tdmHostname);

		if (!StringUtils.isBlank(_tdmHostname))
			_restApiLibrary = new RestApiLibrary(_tdmHostname, _tdmPort,
					_apiEndpointPath);
		else
			_log.warn("No TDM URL given!");
	}

	private RestApiLibrary _restApiLibrary;

	public URL buildUrl(String baseObject, String... params) throws Exception {
		return _restApiLibrary.buildUrl(baseObject, params);
	}

	public void setConfigItem(String baseObject, String component, String key,
			String value) throws Exception {
		String[] params = { component, key, "set" };
		if (_restApiLibrary == null)
			return;
		URL requestUrl = buildUrl(baseObject, params);
		_log.info("Requesting " + requestUrl);

		if (!_restApiLibrary.setContents(requestUrl, value)) {
			throw new Exception("Error setting configuration value");
		}
	}

	public String log(String baseObject, String component, Integer priority,
			String message) {
		return _restApiLibrary.log(baseObject, component, priority, message);
	}

	public List<JsonObject> getItemsForRequest(String baseObject,
			String... params) throws Exception {
		if (_restApiLibrary == null)
			return Collections.emptyList();
		URL requestUrl = _restApiLibrary.buildUrl(baseObject, params);
		_log.info("Requesting " + requestUrl);

		String responseJson = _restApiLibrary
				.getContentsOfUrlAsString(requestUrl);

		return _restApiLibrary.getJsonObjectsForString(responseJson);
	}

	/**
	 * Convenience method. Note this assumes all values coming back from the
	 * service are strings.
	 */
	public List<Map<String, String>> getItems(String baseObject,
			String... params) throws Exception {
		if (_restApiLibrary == null)
			return Collections.emptyList();
		List<Map<String, String>> result = new ArrayList<Map<String, String>>();
		List<JsonObject> items = getItemsForRequest(baseObject, params);
		for (JsonObject item : items) {
			Map<String, String> m = new HashMap<String, String>();
			result.add(m);
			for (Map.Entry<String, JsonElement> entry : item.entrySet()) {
				m.put(entry.getKey(), entry.getValue().getAsString());
			}
		}
		return result;
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

}
