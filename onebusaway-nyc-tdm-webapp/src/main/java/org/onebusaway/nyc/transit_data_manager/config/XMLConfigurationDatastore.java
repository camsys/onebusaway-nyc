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

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.ConfigItem;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.ConfigurationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class XMLConfigurationDatastore implements
    ConfigurationDatastoreInterface {

  private static Logger _log = LoggerFactory.getLogger(XMLConfigurationDatastore.class);

  private boolean isConfigLoaded = false;
  private File configFile;

  private ConfigurationStore configuration;

  public void setConfigFilePath(String configFilePath) throws Exception {
    configFile = new File(configFilePath);

    loadConfigFile(configFilePath);

    loadConfiguration();
  }

  public XMLConfigurationDatastore() {

  }

  @Override
  public synchronized List<ConfigItem> getCompleteSetConfigItems() {
    return configuration.getEntireConfiguration();
  }

  @Override
  public synchronized List<ConfigItem> getConfigItemsForComponent(
      String component) {
    List<ConfigItem> result = configuration.getConfigForComponent(component);

    return result;
  }

  @Override
  public synchronized ConfigItem getConfigItemByComponentKey(String component,
      String key) {
    return configuration.getConfigForComponentKey(component, key);
  }

  @Override
  public synchronized void setConfigItemByComponentKey(String component,
      String key, ConfigItem config) {

    _log.info("Saving value in configurationstore in setConfigItemByComponentKey.");

    configuration.setConfigForComponentKey(component, key, config);

    try {
      saveConfiguration();
    } catch (JAXBException e) {
      e.printStackTrace();
    }

    _log.info("Done in setConfigItemByComponentKey.");
  }

  @Override
  public synchronized ConfigItem deleteConfigItemByKey(String component, String key){
    ConfigItem deletedConfigItem = configuration.deleteConfigForComponentKey(component, key);

    if(deletedConfigItem != null) {
      try {
        saveConfiguration();
      } catch (JAXBException e) {
        e.printStackTrace();
      }
    }

    return deletedConfigItem;
  }

  @Override
  public synchronized boolean getHasComponent(String component) {
    return configuration.getComponentMap().keySet().contains(component);
  }

  @Override
  public synchronized boolean getComponentHasKey(String component, String key) {
    if (getHasComponent(component)) {
      return configuration.getComponentMap().get(component).getConfigMap().keySet().contains(
          key);
    } else {
      return false;
    }
  }

  private void loadConfiguration() throws JAXBException  {

    _log.info("For the TDM Config tool, loading XML Configuration from "
        + configFile.getPath());
    if (!isConfigLoaded) {

      // Check to see if the file is empty
      if (configFile.length() == 0) { // If the file is empty...
        configuration = new ConfigurationStore();
      } else { // File is not empty, must contain ConfigurationStore stuff.
        JAXBContext jc = JAXBContext.newInstance(ConfigurationStore.class);
        Unmarshaller u = jc.createUnmarshaller();

        configuration = (ConfigurationStore) u.unmarshal(configFile);
      }

      isConfigLoaded = true;
    }

    _log.info("XML Configuration loaded.");
  }

  private void saveConfiguration() throws JAXBException {
    _log.info("writing configuration to file=" + configFile);

    JAXBContext jc = JAXBContext.newInstance(ConfigurationStore.class);
    Marshaller m = jc.createMarshaller();

    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

    m.marshal(configuration, configFile);

    _log.debug("Done writing configuration to file.");
  }

  private void loadConfigFile(String configFilePath) throws Exception {
    if (configFile.exists()) {
      if (!configFile.canRead()) {
        throw new Exception("Can not read input file at " + configFilePath);
      }
      if (!configFile.canWrite()) {
        throw new Exception("Can not write to input file at " + configFilePath);
      }
    } else {
      configFile.createNewFile();
    }
  }

}
