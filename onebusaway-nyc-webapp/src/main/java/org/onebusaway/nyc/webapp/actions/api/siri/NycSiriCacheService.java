package org.onebusaway.nyc.webapp.actions.api.siri;

import org.onebusaway.nyc.queue.QueueListenerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public abstract class NycSiriCacheService<K, V> {

  static Logger _log = LoggerFactory.getLogger(QueueListenerTask.class);
  protected Cache<K, V> _cache;  

  protected abstract void refreshCache();

  // proxy to the actual hashing algorithm 
  protected abstract K hash(Object...factors);

  protected Cache<K, V> getCache(){
    if (_cache == null) {
      int timeout = 15;
      _log.info("creating initial generic cache with timeout " + timeout + "...");
      _cache = CacheBuilder.newBuilder()
          .expireAfterWrite(timeout, TimeUnit.SECONDS)
          .build();
      _log.info("done");
    }
    return _cache;
  }

  protected V retrieve(K key){
    return getCache().getIfPresent(key);
  }

  protected void store(K key, V value) {
    getCache().put(key, value);
  }

  protected boolean containsKey(K key){
    return getCache().asMap().containsKey(key);
  }

  protected boolean hashContainsKey(Object...factors){
    return containsKey(hash(factors));
  }

  protected void hashStore(V value, Object...factors){
    getCache().put(hash(factors), value);
  }
}