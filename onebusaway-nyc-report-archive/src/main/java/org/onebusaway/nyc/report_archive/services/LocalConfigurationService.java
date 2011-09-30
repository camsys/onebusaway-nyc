package org.onebusaway.nyc.report_archive.services;

import org.onebusaway.nyc.transit_data.services.ConfigurationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalConfigurationService implements ConfigurationService {

  private static Logger _log = LoggerFactory.getLogger(LocalConfigurationService.class);

  @Override
  public Float getConfigurationValueAsFloat(String arg0, Float arg1) {
    throw new RuntimeException("Unknown config key requested: " + arg0);
  }

  @Override
  public Integer getConfigurationValueAsInteger(String arg0, Integer arg1) {
    if (arg0.equalsIgnoreCase("inference-engine.inputQueuePort")) {
      return 5563;
    }
    throw new RuntimeException("Unknown config key requested: " + arg0);
  }

  @Override
  public String getConfigurationValueAsString(String arg0, String arg1) {
    if (arg0.equalsIgnoreCase("inference-engine.inputQueueHost")) {
      return "queue.staging.obanyc.com";
    }
    if (arg0.equalsIgnoreCase("inference-engine.inputQueueName")) {
      return "bhs_queue";
    }
    if (arg0.equalsIgnoreCase("inference-engine.inputQueuePort")) {
      return "5563";
    }
    throw new RuntimeException("Unknown config key requested: " + arg0);
  }

  @Override
  public void setConfigurationValue(String arg0, String arg1) throws Exception {
    // TODO Auto-generated method stub
    _log.info("Called with " + arg0);
    throw new RuntimeException("Not implemented");
  }

}
