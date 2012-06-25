package org.onebusaway.nyc.sms.impl;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Future;

import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.OperationFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MemcacheSessionManagerImpl extends SessionManagerImpl {

  static Logger _log = LoggerFactory.getLogger(MemcacheSessionManagerImpl.class);
  
  @Autowired
  private MemcacheClientFactory memcacheClientFactory;

  private MemcachedClient _client;

  public MemcacheSessionManagerImpl() {
    super();
  }
  
  @Override
  public Map<String, Object> getContext(String key) {
    Map<String, Object> context = getContextFromCache(key);
    if (context != null) {
      _log.debug("getContext for " + key + " not null, updating local cache");
      updateLocalCache(key, context);
    } else {
      _log.debug("getContext for " + key + " is null, delegating to superclass");
      context = super.getContext(key);
    }
    return context;
  }

  @Override
  public boolean contextExistsFor(String sessionId) {
    boolean exists = contextExistsInCacheFor(sessionId) || super.contextExistsFor(sessionId);
    _log.debug("contextExistsFor returning " + exists + " for sessionId " + sessionId);
    return exists;
  }
  
  @Override
  public void saveContext(String sessionId) {
    _log.debug("Saving context for sessionId " + sessionId);
    try {
      Future<Boolean> cas = getClient().set(sessionId, _sessionTimeout, super.getOrCreateContextEntry(sessionId));
      _log.debug("saveContext: response: " + cas.get());
    } catch (Exception e) {
      _log.error("Failed to save context: ", e);
    }
  }
  
  @Override
  public void close() {
    super.close();
    closeClient();
  }

  // Private methods
  
  @SuppressWarnings("unchecked")
  private Map<String, Object> getContextFromCache(String key) {
    _log.debug("getContextFromCache for " + key);
    try {
      ContextEntry contextEntry = (ContextEntry) getClient().get(key);
      _log.debug("getContextFromCache value before null test is: " + contextEntry);
      Map<String, Object> map = (Map<String, Object>) (contextEntry == null ? contextEntry : contextEntry.getContext());
      _log.debug("getContextFromCache for " + key + " result is " + map);
      return map;
    } catch (Exception e) {
      _log.error("Failed to get context from cache: ", e);
    }
    return null;
  }

  private boolean contextExistsInCacheFor(String sessionId) {
    try {
      Object object = getClient().get(sessionId);
      boolean exists = object != null;
      _log.debug("contextExistsInCacheFor " + sessionId + " result is " + exists);
      return exists;
    } catch (Exception e) {
      _log.error("Failed to check contextExistsInCacheFor: ", e);
    }
    return false;
  }

  private void updateLocalCache(String key, Map<String, Object> context) {
    _log.debug("updateLocalCache for key " + key);
    super.updateContext(key, context);
  }

  public synchronized MemcachedClient getClient() throws IOException {
    if (_client == null)
      _client = memcacheClientFactory.getCacheClient();
    return _client;
  }

  public synchronized void setClient(MemcachedClient _client) {
    this._client = _client;
  }
  
  public synchronized void closeClient() {
    if (_client == null)
      return;
    _client.shutdown();
    _client = null;
  }

}
