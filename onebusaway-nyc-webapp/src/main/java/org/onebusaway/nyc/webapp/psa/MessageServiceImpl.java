package org.onebusaway.nyc.webapp.psa;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.util.model.PublicServiceAnnouncement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

public class MessageServiceImpl implements MessageService {

  private static final Logger _log = LoggerFactory.getLogger(MessageServiceImpl.class);
  
  private static final String ENDPOINT = "/api/psa";
  
  private ConfigurationService _service;

  private Map<Integer, PublicServiceAnnouncement> _psaCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS)
          .<Integer, PublicServiceAnnouncement>build().asMap();

  private int _nPsas = 0;

  @Autowired
  public void setConfigurationService(ConfigurationService service) {
    _service = service;
  }

  @Override
  public String getMessage() {
    PublicServiceAnnouncement psa;
    while (_nPsas == 0 || (psa = getRandomPsa()) == null)
      fillCache();
    return psa.getText();
  }

  private PublicServiceAnnouncement[] getPsas() {
    DefaultHttpClient client = new DefaultHttpClient();
    HttpGet get = new HttpGet(getUrl());
    try {
      HttpResponse response = client.execute(get);
      Gson gson = new Gson();
      InputStreamReader reader = new InputStreamReader(response.getEntity().getContent());
      return gson.fromJson(reader, PublicServiceAnnouncement[].class);
    } catch (IOException e) {
      _log.error("Error retrieving PSA: " + e.getMessage());
      return new PublicServiceAnnouncement[] {};
    }
  }

  @PostConstruct
  private void fillCache() {
    _log.info("Filling public service announcement cache");
    PublicServiceAnnouncement[] psas = getPsas();
    if (psas == null) {
      _log.info("API tier not available.");
      return;
    }
    for (int i = 0; i < psas.length; i++)
      _psaCache.put(i, psas[i]);
    _nPsas = psas.length;
  }

  private PublicServiceAnnouncement getRandomPsa() {
    int i = new Random().nextInt(_nPsas);
    return _psaCache.get(i);
  }

  private String getUrl() {
    return _service.getConfigurationValueAsString("display.apiUrl", "http://localhost:8080/onebusaway-nyc-api-webapp")
            + ENDPOINT;
  }
}
