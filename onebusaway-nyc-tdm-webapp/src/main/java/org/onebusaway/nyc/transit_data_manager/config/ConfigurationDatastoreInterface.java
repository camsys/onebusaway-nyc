/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
