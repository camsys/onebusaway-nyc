package org.onebusaway.nyc.presentation.service.cache;

import org.onebusaway.nyc.queue.QueueListenerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public abstract class NycCacheService<K, V> {

  protected static Logger _log = LoggerFactory.getLogger(QueueListenerTask.class);
  protected Cache<K, V> _cache;  
  protected boolean _disabled;

  public synchronized void setDisabled(boolean disable) {
    this._disabled = true;
  }

  protected abstract void refreshCache();

  // proxy to the actual hashing algorithm 
  public abstract K hash(Object...factors);

  public Cache<K, V> getCache(){
    if (_cache == null) {
      int timeout = 15;
      _log.info("creating initial GENERIC cache with timeout " + timeout + "...");
      _cache = CacheBuilder.newBuilder()
          .expireAfterWrite(timeout, TimeUnit.SECONDS)
          .build();
      _log.info("done");
    }
    return _cache;
  }

  public V retrieve(K key){
    if (_disabled)return null;
    return getCache().getIfPresent(key);
  }

  public void store(K key, V value) {
    if (_disabled) return;
    getCache().put(key, value);
  }

  public boolean containsKey(K key){
    if (_disabled) return false;
    return getCache().asMap().containsKey(key);
  }

  public boolean hashContainsKey(Object...factors){
    if (_disabled) return false;
    return containsKey(hash(factors));
  }

  public void hashStore(V value, Object...factors){
    if (_disabled) return;
    getCache().put(hash(factors), value);
  }
}