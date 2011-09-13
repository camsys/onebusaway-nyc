package org.onebusaway.nyc.transit_data_federation.services.tdm;

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
  public String getConfigurationElementAsString(
      String configurationItemKey,
      String defaultValue);

  /**
   * Get a value for the given configuration key as a float.
   * 
   * @param configurationItemKey The configuration item key.
   * @param defaultValue The value to return if a value for the configurationItemKey 
   * 		  is not found.
   */
  public Float getConfigurationElementAsFloat(
      String configurationItemKey,
      Float defaultValue);
  
}
