package org.onebusaway.nyc.presentation.service;

public interface ConfigurationService {
  
  public String getDefaultAgencyId();

  public ConfigurationBean getConfiguration();

  public void setConfiguration(ConfigurationBean configuration);
}
