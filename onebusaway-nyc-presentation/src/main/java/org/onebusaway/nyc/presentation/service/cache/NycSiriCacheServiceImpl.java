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

public class NycSiriCacheServiceImpl extends NycCacheService<Integer, String> {

  private static final int DEFAULT_CACHE_TIMEOUT = 60;
  private static final String SIRI_CACHE_TIMEOUT_KEY = "cache.expiry.siri";

  @Autowired
  private ConfigurationService _configurationService;

  public synchronized Cache<Integer, String> getCache() {
    return getCache(_configurationService.getConfigurationValueAsInteger(SIRI_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT), "SIRI");
  }

  @Override
  @Refreshable(dependsOn = {SIRI_CACHE_TIMEOUT_KEY})
  protected synchronized void refreshCache() {
    if (_cache == null) return;
    int timeout = _configurationService.getConfigurationValueAsInteger(SIRI_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT);
    _log.info("rebuilding siri cache with " + _cache.size() + " entries after refresh with timeout=" + timeout + "...");
    ConcurrentMap<Integer, String> map = _cache.asMap();
    _cache = CacheBuilder.newBuilder()
        .expireAfterWrite(timeout, TimeUnit.SECONDS)
        .build();
    for (Entry<Integer, String> entry : map.entrySet()) {
      _cache.put(entry.getKey(), entry.getValue());
    }
    _log.info("done");
  }

  private Integer hash(int maximumOnwardCalls, List<String> agencies, String outputType){
    // Use properties of a TreeSet to obtain consistent ordering of like combinations of agencies
    TreeSet<String> set = new TreeSet<String>(agencies);
    return maximumOnwardCalls + set.toString().hashCode() + outputType.hashCode();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Integer hash(Object...factors){
    return hash((Integer)factors[0], (List<String>)factors[1], (String)factors[2]);  
  }

  public void store(Integer key, String value) {
    int timeout = _configurationService.getConfigurationValueAsInteger(SIRI_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT);
    super.store(key, value, timeout);
  }
  
}