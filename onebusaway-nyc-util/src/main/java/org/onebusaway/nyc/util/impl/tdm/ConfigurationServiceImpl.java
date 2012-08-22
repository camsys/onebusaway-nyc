package org.onebusaway.nyc.util.impl.tdm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.google.gson.JsonObject;

public class ConfigurationServiceImpl implements ConfigurationService {

	private static Logger _log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

	@Autowired
	private ThreadPoolTaskScheduler _taskScheduler;

	private RefreshService _refreshService = null;

	private TransitDataManagerApiLibrary _transitDataManagerApiLibrary = null;

	private volatile Map<String, String> _configurationKeyToValueMap = new HashMap<String,String>();

	@Autowired
	public void setRefreshService(RefreshService refreshService) {
		this._refreshService = refreshService;
	}

	@Autowired
	public void setTransitDataManagerApiLibrary(TransitDataManagerApiLibrary apiLibrary) {
		this._transitDataManagerApiLibrary = apiLibrary;
	}

	private void updateConfigurationMap(String configKey, String configValue) {
		synchronized(_configurationKeyToValueMap) {
			String currentValue = _configurationKeyToValueMap.get(configKey);
			_configurationKeyToValueMap.put(configKey, configValue);

			if(currentValue == null || !configValue.equals(currentValue)) {	
				_log.info("Invoking refresh method for config key " + configKey);

				_refreshService.refresh(configKey);
			}
		}
	}

	public void refreshConfiguration() throws Exception {
		List<JsonObject> configurationItems = 
				_transitDataManagerApiLibrary.getItemsForRequest("config", "list");

		for(JsonObject configItem : configurationItems) {
			String configKey = configItem.get("key").getAsString();
			String configValue = configItem.get("value").getAsString();           

			updateConfigurationMap(configKey, configValue);
		} 
	}

	private class UpdateThread implements Runnable {
		@Override
		public void run() {			
			try {				
				refreshConfiguration();
			} catch(Exception e) {
				_log.error("Error updating configuration from TDM: " + e.getMessage());
				e.printStackTrace();
			}
		}		
	}

	@SuppressWarnings("unused")
	@PostConstruct
	private void startUpdateProcess() {
		_taskScheduler.scheduleWithFixedDelay(new UpdateThread(), 5 * 60 * 1000); // 5m
	}

	@Override
	public String getConfigurationValueAsString(String configurationItemKey,
			String defaultValue) {

		synchronized(_configurationKeyToValueMap) {
			if(_configurationKeyToValueMap.size() == 0) {
				_log.warn("No configuration values are present!");
			} else {        
				_log.debug("Have " + _configurationKeyToValueMap.size() + " configuration parameters.");
			}

			String value = _configurationKeyToValueMap.get(configurationItemKey);

			if(value == null) {
				return defaultValue;
			} else { 
				return value;
			}
		}
	}

	@Override
	public Float getConfigurationValueAsFloat(String configurationItemKey, Float defaultValue) {
		try {
			String defaultValueAsString = ((defaultValue != null) ? defaultValue.toString() : null);

			return Float.parseFloat(getConfigurationValueAsString(configurationItemKey, defaultValueAsString));					
		} catch(NumberFormatException ex) {
			return defaultValue;
		}
	}

	@Override
	public Integer getConfigurationValueAsInteger(String configurationItemKey, Integer defaultValue) {
		try {
			String defaultValueAsString = ((defaultValue != null) ? defaultValue.toString() : null);

			return Integer.parseInt(getConfigurationValueAsString(configurationItemKey, defaultValueAsString));					
		} catch(NumberFormatException ex) {
			return defaultValue;
		}
	}

	@Override
	public void setConfigurationValue(String component, String configurationItemKey, String value) 
			throws Exception {
		if(value == null || value.equals("null")) {
			throw new Exception("Configuration values cannot be null (or 'null' as a string!).");
		}

		if(configurationItemKey == null) {
			throw new Exception("Configuration item key cannot be null.");
		}

		synchronized(_configurationKeyToValueMap) {
			String currentValue = _configurationKeyToValueMap.get(configurationItemKey);

			if(currentValue != null && currentValue.equals(value)) {
				return;
			}

			_transitDataManagerApiLibrary.executeApiMethodWithNoResult("config", "set", component, 
					configurationItemKey, value);		  
			updateConfigurationMap(configurationItemKey, value);
		}		 
	}

	@Override
	public Map<String, String> getConfiguration() {
		try {				
			refreshConfiguration();
		} catch(Exception e) {
			_log.error("Error updating configuration from TDM: " + e.getMessage());
			e.printStackTrace();
		}
		return _configurationKeyToValueMap;
	}
}
