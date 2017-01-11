package org.onebusaway.nyc.util.impl.tdm;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseConfiguration {
	
	protected ConcurrentMap<String, String> _configurationKeyToValueMap = new ConcurrentHashMap<String,String>();
	
	private static Logger _log = LoggerFactory.getLogger(BaseConfiguration.class);
	
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

}
