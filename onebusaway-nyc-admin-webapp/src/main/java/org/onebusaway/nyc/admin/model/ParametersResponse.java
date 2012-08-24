package org.onebusaway.nyc.admin.model;

import java.util.Map;

/**
 * Response object with data for admin parameters UI
 * @author abelsare
 *
 */
public class ParametersResponse {

	private Map<String, String> configParameters;
	private boolean saveSuccess;

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

	/**
	 * Returns true if all parameters are saved successfully
	 * @return the saveSuccess
	 */
	public boolean isSaveSuccess() {
		return saveSuccess;
	}

	/**
	 * Set to true if all parameters are saved successfully
	 * @param saveSuccess the saveSuccess to set
	 */
	public void setSaveSuccess(boolean saveSuccess) {
		this.saveSuccess = saveSuccess;
	}
	
	
}
