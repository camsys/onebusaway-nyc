package org.onebusaway.nyc.sms.impl;

import java.io.IOException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import net.spy.memcached.CASValue;
import net.spy.memcached.MemcachedClientIF;

public class MemcacheSessionManagerImpl extends SessionManagerImpl {

  static Logger _log = LoggerFactory.getLogger(MemcacheSessionManagerImpl.class);
  private int _expiryTime;
  
  @Autowired
  private MemcacheClientFactory memcacheClientFactory;

  public MemcacheSessionManagerImpl() {
    super();
  }
  
  @Override
  public Map<String, Object> getContext(String key) {
    Map<String, Object> context = getContextFromCache(key);
    if (context != null)
      updateLocalCache(key, context);
    else
      context = super.getContext(key);
    return context;
  }

  @Override
  public boolean contextExistsFor(String sessionId) {
    return contextExistsInCacheFor(sessionId) || super.contextExistsFor(sessionId);
  }
  
  @Override
  public void saveContext(String sessionId) {
    MemcachedClientIF client;
    try {
      client = memcacheClientFactory.getCacheClient();
      client.cas(sessionId, _sessionTimeout, super.getContext(sessionId));
    } catch (IOException e) {
      _log.error("Failed to save context: ", e);
    }
  }

  // Private methods
  
  @SuppressWarnings("unchecked")
  private Map<String, Object> getContextFromCache(String key) {
    MemcachedClientIF client;
    try {
      client = memcacheClientFactory.getCacheClient();
      CASValue<Object> value = client.getAndTouch(key, _sessionTimeout);
      return (Map<String, Object>) (value == null ? value : value.getValue());
    } catch (IOException e) {
      _log.error("Failed to get context from cache: ", e);
    }
    return null;
  }

  private boolean contextExistsInCacheFor(String sessionId) {
    MemcachedClientIF client;
    try {
      client = memcacheClientFactory.getCacheClient();
      return client.get(sessionId) != null;
    } catch (IOException e) {
      _log.error("Failed to get context from cache: ", e);
    }
    return false;
  }

  private void updateLocalCache(String key, Map<String, Object> context) {
    super.updateContext(key, context);
  }

}
