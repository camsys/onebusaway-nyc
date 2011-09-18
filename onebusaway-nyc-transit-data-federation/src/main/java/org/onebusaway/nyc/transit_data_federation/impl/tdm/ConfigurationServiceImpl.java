package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;

import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.gson.JsonObject;

public class ConfigurationServiceImpl implements ConfigurationService {

	private static Logger _log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

	@Autowired
	private RefreshService _refreshService;
		
	private Timer _updateTimer = null;

	private volatile HashMap<String, String> _configurationKeyToValueMap = new HashMap<String,String>();

	private void updateConfigurationMap(String configKey, String configValue) {
		String currentValue = _configurationKeyToValueMap.get(configKey);
		
		_configurationKeyToValueMap.put(configKey, configValue);

		if(currentValue == null || !configValue.equals(currentValue)) {	
			_log.info("Invoking refresh method for config key " + configKey);
			_refreshService.refresh(configKey);
		}
	}
	
	private class UpdateThread extends TimerTask {
		@Override
		public void run() {			
			try {				
				synchronized(_configurationKeyToValueMap) {
					ArrayList<JsonObject> configurationItems = TransitDataManagerApiLibrary.getItemsForRequest("config", "list");

					for(JsonObject configItem : configurationItems) {
						String configKey = configItem.get("key").getAsString();
						String configValue = configItem.get("value").getAsString();						

						updateConfigurationMap(configKey, configValue);
					}	
				}
			} catch(Exception e) {
				_log.error("Error updating configuration from TDM: " + e.getMessage());
				e.printStackTrace();
			}
		}		
	}
		
	@SuppressWarnings("unused")
	@PostConstruct
	private void startUpdateProcess() {
		_updateTimer = new Timer();
		_updateTimer.schedule(new UpdateThread(), 0, 30 * 1000); // 30s	
	}
	
	@Override
	public String getConfigurationValueAsString(String configurationItemKey,
			String defaultValue) {

		String value = _configurationKeyToValueMap.get(configurationItemKey);
		
		if(value == null)
			return defaultValue;
		else 
			return value;
	}

	@Override
	public Float getConfigurationValueAsFloat(String configurationItemKey,
			Float defaultValue) {
		try {
			String defaultValueAsString = ((defaultValue != null) ? defaultValue.toString() : null);
			
			return Float.parseFloat(
					getConfigurationValueAsString(configurationItemKey, defaultValueAsString));					
		} catch(NumberFormatException ex) {
			return defaultValue;
		}
	}

	@Override
	public Integer getConfigurationValueAsInteger(String configurationItemKey,
			Integer defaultValue) {
		try {
			String defaultValueAsString = ((defaultValue != null) ? defaultValue.toString() : null);
			
			return Integer.parseInt(
					getConfigurationValueAsString(configurationItemKey, defaultValueAsString));					
		} catch(NumberFormatException ex) {
			return defaultValue;
		}
	}

	@Override
	public synchronized void setConfigurationValue(String configurationItemKey, String value) throws Exception {
		if(value == null || value.equals("null")) {
			throw new Exception("Configuration values cannot be null (or 'null' as a string!).");
		}
		
		String currentValue = _configurationKeyToValueMap.get(configurationItemKey);
		if(configurationItemKey != null && currentValue.equals(value))
			return;
		
		TransitDataManagerApiLibrary.executeApiMethodWithNoResult("config", "set", configurationItemKey, value);
		updateConfigurationMap(configurationItemKey, value);
	}
}
