package org.onebusaway.nyc.webapp.actions.api.siri;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import uk.org.siri.siri.Siri;

public class NycSiriCacheServiceImpl extends NycSiriCacheService<Integer, Siri> {

	private static final int DEFAULT_CACHE_TIMEOUT = 2 * 60;
	private static final String SIRI_CACHE_TIMEOUT_KEY = "cache.expiry.siri";

	@Autowired
	private ConfigurationService _configurationService;

	public synchronized Cache<Integer, Siri> getCache() {
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
	public synchronized void refreshCache() {
		if (_cache == null) return; // nothing to do
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

	protected int hash(int maximumOnwardCalls, List<String> agencies){
		String[] agencyArray = (String[]) agencies.toArray();
		Arrays.sort(agencyArray);
		return maximumOnwardCalls + Arrays.toString(agencyArray).hashCode();
	}

	protected boolean hashContainsKey(int maximumOnwardCalls, List<String> agencies){
		return containsKey(hash(maximumOnwardCalls, agencies));
	}

	protected void hashStore(int maximumOnwardCalls, List<String> agencies, Siri request){
		getCache().put(hash(maximumOnwardCalls, agencies), request);
	}

}