package org.onebusaway.nyc.gtfsrt.tds;

import org.onebusaway.nyc.util.configuration.ConfigurationService;

import java.util.Map;

public class MockConfigurationService implements ConfigurationService {
  @Override
  public String getConfigurationValueAsString(String configurationItemKey, String defaultValue) {
    return defaultValue;
  }

  @Override
  public Float getConfigurationValueAsFloat(String configurationItemKey, Float defaultValue) {
    return defaultValue;
  }

  @Override
  public Integer getConfigurationValueAsInteger(String configurationItemKey, Integer defaultValue) {
    return defaultValue;
  }

  @Override
  public void setConfigurationValue(String component, String configurationItemKey, String value) throws Exception {
    throw new IllegalArgumentException("not implemented");
  }

  @Override
  public Map<String, String> getConfiguration() {
    throw new IllegalArgumentException("not implemented");
  }
}
