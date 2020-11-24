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
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class NycGeocoderCacheServiceImpl extends NycCacheService<String, List<NycGeocoderResult>> {

  private static final int DEFAULT_CACHE_TIMEOUT = 3600;
  private static final String GEOCODER_CACHE_TIMEOUT_KEY = "cache.expiry.geocoder";

  @Autowired
  private ConfigurationService _configurationService;

  public synchronized Cache<String, List<NycGeocoderResult>> getCache() {
    return getCache(_configurationService.getConfigurationValueAsInteger(GEOCODER_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT), "GEOCODER", true);
  }

  @Override
  @Refreshable(dependsOn = {GEOCODER_CACHE_TIMEOUT_KEY})
  protected synchronized void refreshCache() {
    if (_cache == null) return;
    int timeout = _configurationService.getConfigurationValueAsInteger(GEOCODER_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT);
    _log.info("rebuilding GEOCODER cache with " + _cache.size() + " entries after refresh with timeout=" + timeout + "...");
    ConcurrentMap<String, List<NycGeocoderResult>> map = _cache.asMap();
    _cache = CacheBuilder.newBuilder()
        .expireAfterWrite(timeout, TimeUnit.SECONDS)
        .build();
    for (Entry<String, List<NycGeocoderResult>> entry : map.entrySet()) {
      _cache.put(entry.getKey(), entry.getValue());
    }
    _log.info("done");
  }

  private String hash(String query){
    return query;
  }

  @Override
  public String hash(Object... factors) {
    return hash((String) factors[0]);
  }

  public void store(String key, List<NycGeocoderResult> value) {
    int timeout = _configurationService.getConfigurationValueAsInteger(
        GEOCODER_CACHE_TIMEOUT_KEY, DEFAULT_CACHE_TIMEOUT);
    super.store(key, value, timeout);
  }

}