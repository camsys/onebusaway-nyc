package org.onebusaway.nyc.presentation.service;

public interface NycConfigurationService {
  
  public String getDefaultAgencyId();

  public ConfigurationBean getConfiguration();

  public void setConfiguration(ConfigurationBean configuration);
}
