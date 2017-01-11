package org.onebusaway.nyc.util.impl.tdm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

public class ManualConfigurationServiceImpl extends BaseConfiguration implements
		ConfigurationService {

	private TransitDataManagerApiLibrary _transitDataManagerApiLibrary = null;
	private String _host = null; // no path configured for TDM by default
	private Integer _port = null; // no port configured for TDM by default

	private static final String TDM_HOST_KEY = "tdm.host";
	private static final String TDM_PORT_KEY = "tdm.port";
	private static Logger _log = LoggerFactory
			.getLogger(ManualConfigurationServiceImpl.class);

	public ManualConfigurationServiceImpl() {
		if(getHost() == null){
			_log.error("TDM Host value not set. Please set -Dtdm.host=tdm and optionally -Dtdm.port=80 or equivalent.");
		}
		_transitDataManagerApiLibrary = new TransitDataManagerApiLibrary(
				getHost(), getPort(), null);
	}

	public void refreshConfiguration() throws Exception {
		List<JsonObject> configurationItems = _transitDataManagerApiLibrary
				.getItemsForRequest("config", "list");
		for (JsonObject configItem : configurationItems) {
			String configKey = configItem.get("key").getAsString();
			String configValue = configItem.get("value").getAsString();

			updateConfigurationMap(configKey, configValue);
		}
	}

	@Override
	public void setConfigurationValue(String component,
			String configurationItemKey, String value) throws Exception {
		if (StringUtils.isBlank(value) || value.equals("null")) {
			throw new Exception(
					"Configuration values cannot be null (or 'null' as a string!).");
		}

		if (StringUtils.isBlank(configurationItemKey)) {
			throw new Exception("Configuration item key cannot be null.");
		}

		String currentValue = _configurationKeyToValueMap
				.get(configurationItemKey);

		if (StringUtils.isNotBlank(currentValue) && currentValue.equals(value)) {
			return;
		}

		_transitDataManagerApiLibrary.setConfigItem("config", component,
				configurationItemKey, value);
		updateConfigurationMap(configurationItemKey, value);
	}

	@Override
	public Map<String, String> getConfiguration() {
		try {
			refreshConfiguration();
		} catch (Exception e) {
			_log.error("Error updating configuration from TDM: "
					+ e.getMessage());
			e.printStackTrace();
		}
		return _configurationKeyToValueMap;
	}

	private void updateConfigurationMap(String configKey, String configValue) {
		_configurationKeyToValueMap.put(configKey, configValue);
	}

	private String getHost() {
		String host = null;
		// option 1: read tdm.host from system env
		host = getHostFromSystemProperty();
		if (StringUtils.isNotBlank(host))
			return host;
		
		// option 2: spring injection
		if (StringUtils.isNotBlank(_host)) {
			return _host;
		}

		// option 3: let if fail -- the default is not to configure this
		return null;
	}

	private String getHostFromSystemProperty() {
		return System.getProperty(TDM_HOST_KEY);
	}

	private Integer getPort() {
		Integer port = null;
		// option 1: read tdm.port from system env
		port = getPortFromSystemProperty();
		if (port != null && port > 0)
			return port;
		
		// option 2: spring injection
		if (_port != null && port > 0) {
			return _port;
		}

		// option 3: let if fail -- the default is not to configure this
		return null;
	}

	private Integer getPortFromSystemProperty() {
		try {
			Integer port = Integer.valueOf(System.getProperty(TDM_PORT_KEY));
			return port;
		} catch (Exception e) {
			return null;
		}
	}

}
