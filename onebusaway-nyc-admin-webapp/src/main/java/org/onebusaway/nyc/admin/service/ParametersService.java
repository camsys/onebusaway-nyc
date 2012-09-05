package org.onebusaway.nyc.admin.service;

import java.util.Map;

import org.onebusaway.nyc.util.configuration.ConfigurationService;

/**
 * Saves admin parameter values on TDM server. Uses {@link ConfigurationService} to persist new 
 * parameter values on TDM.
 * @author abelsare
 *
 */
public interface ParametersService {

	/**
	 * Saves given parameter values to TDM server. 
	 * @param paramters parameters to save
	 * @return true if all parameters are saved sucessfully
	 */
	boolean saveParameters(Map<String, String> parameters);
	
	/**
	 * Returns all key value pairs of configuration stored on TDM server
	 * @return collection of configuration key value pairs
	 */
	Map<String, String> getParameters();
}
