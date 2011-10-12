package org.onebusaway.nyc.transit_data_manager.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.joda.time.DateTime;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.ConfigItem;
import org.onebusaway.nyc.transit_data_manager.config.model.jaxb.ConfigurationStore;
import org.springframework.stereotype.Component;

@Component
public class XMLConfigurationDatastore implements
    ConfigurationDatastoreInterface {

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
  public synchronized List<ConfigItem> getConfigItemsForComponent(String component) {
    List<ConfigItem> result = configuration.getConfigForComponent(component);

    return result;
  }

  @Override
  public synchronized ConfigItem getConfigItemByComponentKey(String component, String key) {
    return configuration.getConfigForComponentKey(component, key);
  }

  @Override
  public synchronized void setConfigItemByComponentKey(String component, String key,
      ConfigItem config) {

    configuration.setConfigForComponentKey(component, key, config);

    try {
      saveConfiguration();
    } catch (JAXBException e) {
      e.printStackTrace();
    }

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

  private void loadConfiguration() throws IOException, JAXBException {

    if (!isConfigLoaded) {
      FileInputStream fis = null;
      try {
        fis = new FileInputStream(configFile);

        // Check to see if the file is empty
        if (fis.read() == -1) { // If the file is
                                // empty...
          configuration = new ConfigurationStore();
        } else { // File is not empty, must contain ConfigurationStore stuff.
          JAXBContext jc = JAXBContext.newInstance(ConfigurationStore.class);
          Unmarshaller u = jc.createUnmarshaller();

          configuration = (ConfigurationStore) u.unmarshal(configFile);
        }

        fis.close();
      } catch (IOException ioe) {
        if (fis != null) fis.close();
        
        throw new IOException("Could not create a FileInputStream with "
            + configFile + " in XMLConfigurationDatastore.loadConfiguration.",
            ioe);
      }

      isConfigLoaded = true;
    }

  }

  private void saveConfiguration() throws JAXBException {
    JAXBContext jc = JAXBContext.newInstance(ConfigurationStore.class);
    Marshaller m = jc.createMarshaller();

    m.marshal(configuration, configFile);
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
