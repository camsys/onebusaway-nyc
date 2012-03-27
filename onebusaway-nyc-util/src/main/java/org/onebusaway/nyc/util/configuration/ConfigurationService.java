package org.onebusaway.nyc.util.configuration;

/**
 * Service interface for getting configuration parameters from a centrally-distributed
 * configuration service.
 * 
 * @author jmaki
 */
public interface ConfigurationService {

  /**
   * Get a value for the given configuration key as a string.
   * 
   * @param configurationItemKey The configuration item key.
   * @param defaultValue The value to return if a value for the configurationItemKey 
   * 		  is not found.
   */
  public String getConfigurationValueAsString(String configurationItemKey,
      String defaultValue);

  public Float getConfigurationValueAsFloat(String configurationItemKey,
      Float defaultValue);
  
  public Integer getConfigurationValueAsInteger(String configurationItemKey,
	      Integer defaultValue);
  
  /**
   * Set a value for the given configuration key as a string.
   * 
   * @param configurationItemKey The configuration item key.
   * @param value The value to set the configuration param to.
   */  
  public void setConfigurationValue(String configurationItemKey, 
		  String value) throws Exception;
}
