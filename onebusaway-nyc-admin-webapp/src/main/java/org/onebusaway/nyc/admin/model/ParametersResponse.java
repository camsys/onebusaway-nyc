package org.onebusaway.nyc.admin.model;

import java.util.Map;

/**
 * Response object with data for admin parameters UI
 * @author abelsare
 *
 */
public class ParametersResponse {

	private Map<String, String> configParameters;

	/**
	 * @return the configParameters
	 */
	public Map<String, String> getConfigParameters() {
		return configParameters;
	}

	/**
	 * @param configParameters the configParameters to set
	 */
	public void setConfigParameters(Map<String, String> configParameters) {
		this.configParameters = configParameters;
	}
	
	
}
