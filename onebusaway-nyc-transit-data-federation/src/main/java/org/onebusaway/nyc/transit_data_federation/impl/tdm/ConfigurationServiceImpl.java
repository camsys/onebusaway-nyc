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

	private class UpdateThread extends TimerTask {
		@Override
		public void run() {			
			try {				
				synchronized(_configurationKeyToValueMap) {
					ArrayList<JsonObject> configurationItems = DataFetcherLibrary.getItemsForRequest("config", "list");

					for(JsonObject configItem : configurationItems) {
						String configKey = configItem.get("key").getAsString();
						String configValue = configItem.get("value").getAsString();
						
						String currentValue = _configurationKeyToValueMap.get(configKey);
						
						if(currentValue == null || !configValue.equals(currentValue)) {
							_configurationKeyToValueMap.put(configKey, configValue);
							
							_log.info("Invoking refresh method for config key " + configKey);
							_refreshService.refresh(configKey);
						}
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
}
