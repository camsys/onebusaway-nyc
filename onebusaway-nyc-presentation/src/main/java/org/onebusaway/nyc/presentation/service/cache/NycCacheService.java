package org.onebusaway.nyc.presentation.service.cache;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.TimerTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public abstract class NycCacheService<K, V> {

  private static final int DEFAULT_CACHE_TIMEOUT = 30;

  private static final int STATUS_INTERVAL_MINUTES = 1;
  protected static Logger _log = LoggerFactory.getLogger(NycCacheService.class);
  protected Cache<K, V> _cache;

  @SuppressWarnings("rawtypes")
  private ScheduledFuture<NycCacheService<K, V>.StatusThread> _statusTask = null;

  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;

  String type;
  String addr = "sessions-memcache:11211";
  MemcachedClient memcachedClient;
  boolean useMemcached;

  protected abstract void refreshCache();

  // proxy to the actual hashing algorithm
  public abstract K hash(Object... factors);

  protected boolean _disabled;

  public synchronized void setDisabled(boolean disable) {
    this._disabled = disable;
  }

  public abstract Cache<K, V> getCache();

  public Cache<K, V> getCache(int timeout, String type, boolean memcacheable) {
    this.type = type;
    if (_cache == null) {
      _log.info("creating initial " + type + " cache with timeout " + timeout
          + "...");
      _cache = CacheBuilder.newBuilder().expireAfterWrite(timeout,
          TimeUnit.SECONDS).build();
      _log.info("done");
    }
    if (memcacheable && memcachedClient == null) {
      try {
        memcachedClient = new MemcachedClient(
            new BinaryConnectionFactory(),
            AddrUtil.getAddresses(addr));
      } catch (Exception e) {
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
        return (V) memcachedClient.get(key.toString());
      } catch (Exception e) {
        toggleCache(false);
      }
    }
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
        memcachedClient.set(key.toString(), timeout, value);
        return;
      } catch (Exception e) {
        toggleCache(false);
      }
    }
    getCache().put(key, value);
  }

  public boolean containsKey(K key) {
    if (_disabled)
      return false;
    Cache<K, V> cache = getCache();
    if (useMemcached) {
      try {
        return memcachedClient.get(key.toString()) != null;
      } catch (Exception e) {
        toggleCache(false);
      }
    }
    if (!cache.asMap().containsKey(key)){
      // only attempt to switch to memcached if there is a miss in local cache
      // to minimize memcached connection attempts, saving time per local cache usage
      if (memcachedClient != null && !memcachedClient.getAvailableServers().isEmpty()){
        toggleCache(true);
      }
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
      _statusTask = (ScheduledFuture<StatusThread>) _taskScheduler.scheduleWithFixedDelay(new StatusThread(),
          STATUS_INTERVAL_MINUTES * 60 * 1000);
    }
  }

  public void logStatus() {
    if (type != null) {
      _log.info("name=" + type + ": disabled=" + _disabled
          + "; " + (useMemcached ? "Memcached" : "Local Cache")
          + "; " + (useMemcached ? memcachedClient.getStats("sizes") : getCache().stats().toString()));
    }
  }

  private class StatusThread extends TimerTask {
    @Override
    public void run() {
      logStatus();
    }
  }

  private void toggleCache(boolean useMemcached) {
    this.useMemcached = useMemcached;
    _log.info("Caching with " + (useMemcached ? "Memcached" : "Local Cache"));
  }
}