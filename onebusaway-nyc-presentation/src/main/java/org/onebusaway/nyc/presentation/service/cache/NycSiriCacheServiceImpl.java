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
  
  public synchronized Cache<Integer, Siri> getCache() {
	return getCache(_configurationService.getConfigurationValueAsInteger(SIRI_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT), "SIRI");
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

  private Integer hash(int maximumOnwardCalls, List<String> agencies){
	// Use properties of a TreeSet to obtain consistent ordering of like combinations of agencies
    TreeSet<String> set = new TreeSet<String>(agencies);
    return maximumOnwardCalls + set.toString().hashCode();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Integer hash(Object...factors){
    return hash((Integer)factors[0], (List<String>)factors[1]);  
  }

  public void store(Integer key, Siri value) {
    if (useMemcached){
	  try{
	    int timeout = _configurationService.getConfigurationValueAsInteger(SIRI_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT);
	    memcache.set(key.toString(), timeout, new SiriWrapper(value, _realtimeService));
	    return;
      }
      catch(Exception e){
        e.printStackTrace();
      }
    }
    useMemcached=false;
    getCache().put(key, value);
  }
}