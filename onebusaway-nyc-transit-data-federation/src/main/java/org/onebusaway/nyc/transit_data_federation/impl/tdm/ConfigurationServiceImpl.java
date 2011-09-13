package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.nyc.transit_data_federation.impl.tdm.model.ConfigurationItem;
import org.onebusaway.nyc.transit_data_federation.services.tdm.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConfigurationServiceImpl implements ConfigurationService {

	@Autowired
	private RefreshService _refreshServier;
	
	private static Logger _log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

	private Timer _updateTimer;

	private volatile HashMap<String, String> _configurationKeyToValueMap = new HashMap<String,String>();

	private class UpdateThread extends TimerTask {
		@Override
		public void run() {
			try {
				synchronized(_configurationKeyToValueMap) {
					@SuppressWarnings("unchecked")
					ArrayList<ConfigurationItem> configurationItems = 
						(ArrayList<ConfigurationItem>)DataFetcherLibrary.getItemsForRequest("config", "list");

					for(ConfigurationItem configItem : configurationItems) {
						_configurationKeyToValueMap.put(configItem.key, configItem.value);
						
					}
				}
			} catch(Exception e) {
				_log.error("Error updating configuration");
				e.printStackTrace();
			}
		}		
	}

	public ConfigurationServiceImpl() {
		_updateTimer = new Timer();
		_updateTimer.schedule(new UpdateThread(), 0, 5 * 1000);
	}
	
	@Override
	public String getConfigurationElementAsString(String configurationItemKey,
			String defaultValue) {

		String value = _configurationKeyToValueMap.get("configurationItemKey");
		
		if(value == null)
			return defaultValue;
		else 
			return value;
	}

	@Override
	public Float getConfigurationElementAsFloat(String configurationItemKey,
			Float defaultValue) {
		try {
			return Float.parseFloat(
					getConfigurationElementAsString(configurationItemKey, defaultValue.toString()));					
		} catch(NumberFormatException ex) {
			return defaultValue;
		}
	}

}
