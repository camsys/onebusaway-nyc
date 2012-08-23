package org.onebusaway.nyc.admin.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.nyc.admin.service.ParametersService;
import org.onebusaway.nyc.admin.util.ConfigurationKeyTranslator;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Default implementation of {@link ParametersService}
 * @author abelsare
 *
 */
public class ParametersServiceImpl implements ParametersService {

	private ConfigurationService configurationService;
	private ConfigurationKeyTranslator keyTranslator;
	
	@Override
	public boolean saveParameters(List<String> parameters) {
		return false;
	}

	@Override
	public Map<String, String> getParameters() {
		keyTranslator = new ConfigurationKeyTranslator();
		Map<String, String> displayParameters = new HashMap<String, String>(); 
		
		//Get config values from TDM
		Map<String, String> configParameters = configurationService.getConfiguration();
		
		for(Map.Entry<String, String> entry : configParameters.entrySet()) {
			String configKey = entry.getKey();
			String configValue = entry.getValue();
			//Translate config key to its UI counterpart
			String uiKey = keyTranslator.getTranslatedKey(configKey);
			displayParameters.put(uiKey, configValue);
		}
		
		return displayParameters;
	}

	/**
	 * Injects configuration service
	 * @param configurationService the configurationService to set
	 */
	@Autowired
	public void setConfigurationService(ConfigurationService configurationService) {
		this.configurationService = configurationService;
	}


}
