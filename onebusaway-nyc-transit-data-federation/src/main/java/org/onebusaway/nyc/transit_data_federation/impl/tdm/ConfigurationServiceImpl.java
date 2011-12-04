package org.onebusaway.nyc.transit_data_federation.impl.tdm;

import org.onebusaway.container.refresh.RefreshService;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;

import com.google.gson.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.ArrayList;
import java.util.HashMap;

import javax.annotation.PostConstruct;

public class ConfigurationServiceImpl implements ConfigurationService {

	private static Logger _log = LoggerFactory.getLogger(ConfigurationServiceImpl.class);

  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;

	private RefreshService _refreshService = null;
		
  private TransitDataManagerApiLibrary _transitDataManagerApiLibrary = null;

	private volatile HashMap<String, String> _configurationKeyToValueMap = new HashMap<String,String>();
	

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
	  ArrayList<JsonObject> configurationItems = 
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
	  _taskScheduler.scheduleWithFixedDelay(new UpdateThread(), 30 * 1000); // 30s
	}
	
	@Override
	public String getConfigurationValueAsString(String configurationItemKey,
			String defaultValue) {

    synchronized(_configurationKeyToValueMap) {
      String value = _configurationKeyToValueMap.get(configurationItemKey);
		
      _log.debug("Have " + _configurationKeyToValueMap.size() + " configuration parameters.");
      
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
	public void setConfigurationValue(String configurationItemKey, String value) throws Exception {
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
		
		  _transitDataManagerApiLibrary.executeApiMethodWithNoResult("config", "set", configurationItemKey, value);		  
		  updateConfigurationMap(configurationItemKey, value);
		}		 
	}
}
