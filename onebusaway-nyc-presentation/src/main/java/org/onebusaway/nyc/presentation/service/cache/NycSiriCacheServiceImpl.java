/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.presentation.service.cache;

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

public class NycSiriCacheServiceImpl extends NycCacheService<Integer, String> {

  private static final int DEFAULT_CACHE_TIMEOUT = 30;
  private static final String SIRI_CACHE_TIMEOUT_KEY = "cache.expiry.siri";

  @Autowired
  private ConfigurationService _configurationService;

  public synchronized Cache<Integer, String> getCache() {
    return getCache(_configurationService.getConfigurationValueAsInteger(SIRI_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT), "SIRI", true);
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
  
  private Integer _hash(int maximumOnwardCalls, List<String> agencies, String outputType, String version,
                        boolean showApc, boolean showRawApc){
	// Use properties of a TreeSet to obtain consistent ordering of like combinations of agencies
	TreeSet<String> set = new TreeSet<String>(agencies);
	return maximumOnwardCalls + set.toString().hashCode() + outputType.hashCode() + version.hashCode()
            + new Boolean(showApc).hashCode() + new Boolean(showRawApc).hashCode();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Integer hash(Object...factors){

    return _hash((Integer)factors[0], /*maximumOnwardCalls*/
            (List<String>)factors[1], /*agencies*/
            (String)factors[2], /*outputType*/
            (String)factors[3], /*version*/
            (Boolean)factors[4], /*showApc*/
            (Boolean)factors[5] /*showRawApc*/
              );
  }

  public void store(Integer key, String value) {
    int timeout = _configurationService.getConfigurationValueAsInteger(SIRI_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT);
    super.store(key, value, timeout);
  }
  
}