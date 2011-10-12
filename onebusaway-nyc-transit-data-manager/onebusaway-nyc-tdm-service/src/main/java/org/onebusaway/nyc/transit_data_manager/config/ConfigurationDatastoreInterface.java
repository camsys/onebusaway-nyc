package org.onebusaway.nyc.transit_data_manager.config;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.ConfigItem;

/**
 * Interface created to hide the details of the datastore for the configuration
 * tool.
 * 
 * @author sclark
 * 
 */
public interface ConfigurationDatastoreInterface {

  /**
   * Get the entire set of configuration items
   * 
   * @return a list of ConfigItem containing every configuration option in this
   *         datastore.
   */
  public List<ConfigItem> getCompleteSetConfigItems();

  /**
   * Get the set of configuration items for a certain component.
   * 
   * @param component The component to get configuration for
   * @return a list of ConfigItem containing every configuration option for the
   *         input component.
   */
  public List<ConfigItem> getConfigItemsForComponent(String component);

  /**
   * Get a single ConfigItem for a component by key
   * 
   * @param component The component for which the key is saved
   * @param key The key value of the component
   * @return A ConfigItem containing the certain key.
   */
  public ConfigItem getConfigItemByComponentKey(String component, String key);

  /**
   * Set the configuration value for a certain key in a component.
   * 
   * @param component The component within which the configuration should be
   *          stored.
   * @param key the key of the component being stored
   * @param config the actual ConfigItem to store.
   */
  public void setConfigItemByComponentKey(String component, String key,
      ConfigItem config);
  
  /**
   * Returns true if the datastore has data for a certain component.
   * @param component
   * @return
   */
  public boolean getHasComponent (String component);

  /**
   * Returns true if the datastore has data for a certain key within a certain component.
   * @param component
   * @param key
   * @return
   */
  public boolean getComponentHasKey (String component, String key);
}
