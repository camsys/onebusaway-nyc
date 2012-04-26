package org.onebusaway.nyc.sms.impl;

import java.util.Map;
import java.util.concurrent.Future;

import net.spy.memcached.CASValue;
import net.spy.memcached.MemcachedClientIF;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MemcacheSessionManagerImpl extends SessionManagerImpl {

  static Logger _log = LoggerFactory.getLogger(MemcacheSessionManagerImpl.class);
  
  @Autowired
  private MemcacheClientFactory memcacheClientFactory;

  public MemcacheSessionManagerImpl() {
    super();
  }
  
  @Override
  public Map<String, Object> getContext(String key) {
    Map<String, Object> context = getContextFromCache(key);
    if (context != null) {
      _log.info("getContext for " + key + " not null, updating local cache");
      updateLocalCache(key, context);
    } else {
      _log.info("getContext for " + key + " is null, delegating to superclass");
      context = super.getContext(key);
    }
    return context;
  }

  @Override
  public boolean contextExistsFor(String sessionId) {
    boolean exists = contextExistsInCacheFor(sessionId) || super.contextExistsFor(sessionId);
    _log.info("contextExistsFor returning " + exists + " for sessionId " + sessionId);
    return exists;
  }
  
  @Override
  public void saveContext(String sessionId) {
    _log.info("Saving context for sessionId " + sessionId);
    MemcachedClientIF client;
    try {
      client = memcacheClientFactory.getCacheClient();
      Future<Boolean> cas = client.set(sessionId, _sessionTimeout, super.getContext(sessionId));
      _log.info("saveContext: response: " + cas.get());
    } catch (Exception e) {
      _log.error("Failed to save context: ", e);
    }
  }

  // Private methods
  
  @SuppressWarnings("unchecked")
  private Map<String, Object> getContextFromCache(String key) {
    MemcachedClientIF client;
    _log.info("getContextFromCache for " + key);
    try {
      client = memcacheClientFactory.getCacheClient();
      CASValue<Object> value = client.getAndTouch(key, _sessionTimeout);
      Map<String, Object> map = (Map<String, Object>) (value == null ? value : value.getValue());
      _log.info("getContextFromCache for " + key + " result is " + map);
      return map;
    } catch (Exception e) {
      _log.error("Failed to get context from cache: ", e);
    }
    return null;
  }

  private boolean contextExistsInCacheFor(String sessionId) {
    MemcachedClientIF client;
    try {
      client = memcacheClientFactory.getCacheClient();
      boolean exists = client.get(sessionId) != null;
      _log.info("contextExistsInCacheFor " + sessionId + " result is " + exists);
      return exists;
    } catch (Exception e) {
      _log.error("Failed to check contextExistsInCacheFor: ", e);
    }
    return false;
  }

  private void updateLocalCache(String key, Map<String, Object> context) {
    _log.info("updateLocalCache for key " + key);
    super.updateContext(key, context);
  }

}
