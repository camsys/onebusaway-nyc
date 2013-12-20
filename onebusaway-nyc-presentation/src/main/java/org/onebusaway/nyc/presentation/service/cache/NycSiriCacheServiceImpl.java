package org.onebusaway.nyc.presentation.service.cache;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import uk.org.siri.siri.Siri;

public class NycSiriCacheServiceImpl extends NycCacheService<Integer, Siri> {

  private static final int DEFAULT_CACHE_TIMEOUT = 15;
  private static final String SIRI_CACHE_TIMEOUT_KEY = "cache.expiry.siri";

  @Autowired
  private ConfigurationService _configurationService;
  
  @Autowired
  private RealtimeService _realtimeService;
  
  private boolean _disabled;

  public synchronized void setDisabled(boolean disable) {
	  this._disabled = true;
  }
  
  public synchronized Cache<Integer, Siri> getCache() {
    if (!useMemcached() && _cache == null) {
      int timeout = _configurationService.getConfigurationValueAsInteger(SIRI_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT);
      _log.info("creating initial SIRI cache with timeout " + timeout + "...");
      _cache = CacheBuilder.newBuilder()
          .expireAfterWrite(timeout, TimeUnit.SECONDS)
          .build();
      _log.info("done");
    }
    if (_disabled) _cache.invalidateAll(); 
    return _cache;
  }

  @Override
  @Refreshable(dependsOn = {SIRI_CACHE_TIMEOUT_KEY})
  protected synchronized void refreshCache() {
    if (_cache == null) return;
    int timeout = _configurationService.getConfigurationValueAsInteger(SIRI_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT);
    _log.info("rebuilding siri cache with " + _cache.size() + " entries after refresh with timeout=" + timeout + "...");
    ConcurrentMap<Integer, Siri> map = _cache.asMap();
    _cache = CacheBuilder.newBuilder()
        .expireAfterWrite(timeout, TimeUnit.SECONDS)
        .build();
    for (Entry<Integer, Siri> entry : map.entrySet()) {
      _cache.put(entry.getKey(), entry.getValue());
    }
    _log.info("done");
  }

  // Hash Function: Sort the list of agencies via properties of a TreeSet
  private Integer hash(int maximumOnwardCalls, List<String> agencies){
    TreeSet<String> set = new TreeSet<String>(agencies);
    return maximumOnwardCalls + set.toString().hashCode();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Integer hash(Object...factors){
    return hash((Integer)factors[0], (List<String>)factors[1]);  
  }  
  
  public Siri retrieve(Integer key){
	  System.out.println("Retrieving! K:"+key);
      if (useMemcached){
    	  System.out.println("THE ITEM RETRIEVED FROM THE MEMCACHE WAS: "+memcache.get(key.toString()));
    	  return  ((SiriWrapper) memcache.get(key.toString()));
      }
      else{
    	  System.out.println("THE ITEM RETRIEVED FROM THE LOCAL CACHE WAS: "+memcache.get(key.toString()));
    	  return (getCache()!=null?getCache().getIfPresent(key):null);
      }
}

  public void store(Integer key, Siri value) {
      if (useMemcached){
    	  int timeout = _configurationService.getConfigurationValueAsInteger(SIRI_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT);
    	  System.out.println("Storing via MEMCACHE, K:"+key+" V:"+value);
    	  memcache.set(key.toString(), timeout, new SiriWrapper(value));
          return;
      }
      else{
    	  System.out.println("Storing via LOCAL CACHE, K:"+key+" V:"+value);
          getCache().put(key, value);
      }
  }
}