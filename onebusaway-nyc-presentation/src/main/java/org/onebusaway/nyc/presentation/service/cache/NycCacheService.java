package org.onebusaway.nyc.presentation.service.cache;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


import java.net.InetSocketAddress;
import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public abstract class NycCacheService<K, V> {

  private static final int DEFAULT_CACHE_TIMEOUT = 60;

  private static final int STATUS_INTERVAL_MINUTES = 1;
  protected static Logger _log = LoggerFactory.getLogger(NycCacheService.class);
  protected Cache<K, V> _cache;

  private ScheduledFuture<NycCacheService.StatusThread> _statusTask = null;

  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;

  String addr = "sessions-memcache:11211";
  MemcachedClient memcache;
  boolean useMemcached;

  protected abstract void refreshCache();

  // proxy to the actual hashing algorithm
  public abstract K hash(Object... factors);

  protected boolean _disabled;

  public synchronized void setDisabled(boolean disable) {
    this._disabled = disable;
  }

  public Cache<K, V> getCache() {
    return getCache(DEFAULT_CACHE_TIMEOUT, "GENERIC");
  }

  public Cache<K, V> getCache(int timeout, String type) {
    if (_cache == null) {
      _log.info("creating initial " + type + " cache with timeout " + timeout
          + "...");
      _cache = CacheBuilder.newBuilder().expireAfterWrite(timeout,
          TimeUnit.SECONDS).build();
      _log.info("done");
    }
    if (memcache==null)
    {
      try {
        memcache = new MemcachedClient(
            new BinaryConnectionFactory(),
            AddrUtil.getAddresses(addr));
      } 
      catch (Exception e) {
      }
    }
    if (_disabled)
      _cache.invalidateAll();
    return _cache;
  }

  @SuppressWarnings("unchecked")
  public V retrieve(K key) {
    if (_disabled)
      return null;
    if (useMemcached) {
      try {
        return (V) memcache.get(key.toString());
      } catch (Exception e) {
      }
    }
    useMemcached = false;
    return (getCache() != null ? getCache().getIfPresent(key) : null);
  }

  public void store(K key, V value) {
    store(key, value, DEFAULT_CACHE_TIMEOUT);
  }

  public void store(K key, V value, int timeout) {
    if (_disabled)
      return;
    if (useMemcached) {
      try {
        memcache.set(key.toString(), timeout, value);
        return;
      } catch (Exception e) {
      }
    }
    useMemcached = false;
    getCache().put(key, value);
  }

  public boolean containsKey(K key) {
    if (_disabled)
      return false;
    Cache<K, V> cache = getCache();
    if (useMemcached) {
      try {
        return memcache.get(key.toString()) != null;
      } catch (Exception e) {
      }
    }
    if (!cache.asMap().containsKey(key)){
      // only attempt to switch to memcached if there is a miss in local cache
      // to minimize memcached connection attempts, saving time per local cache usage
      useMemcached=(memcache==null?false:!memcache.getAvailableServers().isEmpty());
      return false;
    }
    return true;
  }

  public boolean hashContainsKey(Object... factors) {
    return containsKey(hash(factors));
  }

  public void hashStore(V value, Object... factors) {
    getCache().put(hash(factors), value);
  }

  @SuppressWarnings("unchecked")
  @PostConstruct
  private void startStatusTask() {
    if (_statusTask == null) {
      _statusTask = _taskScheduler.scheduleWithFixedDelay(new StatusThread(),
          STATUS_INTERVAL_MINUTES * 60 * 1000);
    }
  }

  public void logStatus() {
    _log.info(getCache().stats().toString() + "; disabled=" + _disabled
        + "; useMemcached=" + useMemcached
        /*
         * + "; interval=" +
         * _configurationService.getConfigurationValueAsInteger
         * (SIRI_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT)
         */
        + "; Local Size=" + _cache.size()
        + "; Memcached Size=" + (memcache==null?"[null]":memcache.getStats("sizes")));
  }

  private class StatusThread extends TimerTask {
    @Override
    public void run() {
      logStatus();
    }
  }
}