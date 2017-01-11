package org.onebusaway.nyc.util.impl.tdm;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.google.gson.JsonObject;

public class ConfigurationServiceImpl extends BaseConfiguration implements ConfigurationService {

	private static Logger _log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

	@Autowired
	private ThreadPoolTaskScheduler _taskScheduler;

	private RefreshService _refreshService = null;

	private TransitDataManagerApiLibrary _transitDataManagerApiLibrary = null;

	private ConcurrentMap<String, String> _configurationKeyToValueMap = new ConcurrentHashMap<String,String>();

	@Autowired
	public void setRefreshService(RefreshService refreshService) {
		this._refreshService = refreshService;
	}

	@Autowired
	public void setTransitDataManagerApiLibrary(TransitDataManagerApiLibrary apiLibrary) {
		this._transitDataManagerApiLibrary = apiLibrary;
	}

	@Autowired
	public void setTaskScheduler(ThreadPoolTaskScheduler taskScheduler) {
		this._taskScheduler = taskScheduler;
	}

	private void updateConfigurationMap(String configKey, String configValue) {
		String currentValue = _configurationKeyToValueMap.get(configKey);
		_configurationKeyToValueMap.put(configKey, configValue);

		if(currentValue == null || !configValue.equals(currentValue)) {	
			_log.info("Invoking refresh method for config key " + configKey);

			_refreshService.refresh(configKey);
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
	  try {
      if (_configurationKeyToValueMap.size() == 0) {
        _log.warn("No configuration values are present!");
      } else {
        _log.debug("Have " + _configurationKeyToValueMap.size()
            + " configuration parameters.");
      }

      String value = _configurationKeyToValueMap.get(configurationItemKey);

      if (value == null) {
        return defaultValue;
      } else {
        return value;
      }
	  } catch (Exception any) {
	    _log.error("getConfigurationValue(" + configurationItemKey + ", " + defaultValue + ") exception:", any);
	    return defaultValue;
	  }
	}

	@Override
	public Float getConfigurationValueAsFloat(String configurationItemKey, Float defaultValue) {
		try {
			String defaultValueAsString = ((defaultValue != null) ? defaultValue.toString() : null);
			String valueAsString = getConfigurationValueAsString(configurationItemKey, defaultValueAsString);
			if (valueAsString == null) return defaultValue;
			return Float.parseFloat(valueAsString);					
		} catch(NumberFormatException ex) {
			return defaultValue;
		} catch (Exception any) {
		  _log.error("getConfigurationValueAsFloat(" + configurationItemKey + ", " + defaultValue + ") exception:", any);
		  return defaultValue;
		}
	}

	@Override
	public Integer getConfigurationValueAsInteger(String configurationItemKey, Integer defaultValue) {
		try {
			String defaultValueAsString = ((defaultValue != null) ? defaultValue.toString() : null);
			String valueAsString = getConfigurationValueAsString(configurationItemKey, defaultValueAsString);
			if (valueAsString == null) return defaultValue;
			return Integer.parseInt(valueAsString);					
		} catch(NumberFormatException ex) {
			return defaultValue;
		}
	}

	@Override
	public void setConfigurationValue(String component, String configurationItemKey, String value) 
			throws Exception {
		if(StringUtils.isBlank(value) || value.equals("null")) {
			throw new Exception("Configuration values cannot be null (or 'null' as a string!).");
		}

		if(StringUtils.isBlank(configurationItemKey)) {
			throw new Exception("Configuration item key cannot be null.");
		}

		String currentValue = _configurationKeyToValueMap.get(configurationItemKey);

		if(StringUtils.isNotBlank(currentValue) && currentValue.equals(value)) {
			return;
		}

		_transitDataManagerApiLibrary.setConfigItem("config", component, configurationItemKey, value);		  
		updateConfigurationMap(configurationItemKey, value);
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
