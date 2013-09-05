package org.onebusaway.nyc.webapp.actions.api.siri;

import java.util.List;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import uk.org.siri.siri.Siri;

public class NycSiriCacheServiceImpl extends NycSiriCacheService<Integer, Siri> {

  private static final int DEFAULT_CACHE_TIMEOUT = 15;
  private static final String SIRI_CACHE_TIMEOUT_KEY = "cache.expiry.siri";

  @Autowired
  private ConfigurationService _configurationService;

  protected synchronized Cache<Integer, Siri> getCache() {
    if (_cache == null) {
      int timeout = _configurationService.getConfigurationValueAsInteger(SIRI_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT);
      _log.info("creating initial siri cache with timeout " + timeout + "...");
      _cache = CacheBuilder.newBuilder()
          .expireAfterWrite(timeout, TimeUnit.SECONDS)
          .build();
      _log.info("done");
    }
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

  protected Integer hash(int maximumOnwardCalls, List<String> agencies){
    TreeSet<String> set = new TreeSet<String>(agencies);
    return maximumOnwardCalls + set.toString().hashCode();
  }

  @SuppressWarnings("unchecked")
  @Override
  protected Integer hash(Object...factors){
    return hash((Integer)factors[0], (List<String>)factors[1]);
  }

}