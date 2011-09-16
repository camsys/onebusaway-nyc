package org.onebusaway.nyc.transit_data.services;

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
  public String getConfigurationValueAsString(
      String configurationItemKey,
      String defaultValue);

  public Float getConfigurationValueAsFloat(
      String configurationItemKey,
      Float defaultValue);
  
  public Integer getConfigurationValueAsInteger(
	      String configurationItemKey,
	      Integer defaultValue);
}
