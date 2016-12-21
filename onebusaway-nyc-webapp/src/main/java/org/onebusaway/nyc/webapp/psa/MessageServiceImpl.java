package org.onebusaway.nyc.webapp.psa;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class MessageServiceImpl implements MessageService {

  private static final Logger _log = LoggerFactory.getLogger(MessageServiceImpl.class);
  
  private static final String ENDPOINT = "/api/psa/random";
  
  private ConfigurationService _service;
  
  @Autowired
  public void setConfigurationService(ConfigurationService service) {
    _service = service;
  }
  
  @Override
  public String getMessage() {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpGet get = new HttpGet(getUrl());
    try {
      HttpResponse response = client.execute(get);
      return EntityUtils.toString(response.getEntity());
    } catch (IOException e) {
      _log.error("Error retrieving PSA: " + e.getMessage());
      return "";
    }
  }
  
  private String getUrl() {
    return _service.getConfigurationValueAsString("display.adminUrl", "http://admin.dev.obanyc.com")
        + ENDPOINT;
  }

}
