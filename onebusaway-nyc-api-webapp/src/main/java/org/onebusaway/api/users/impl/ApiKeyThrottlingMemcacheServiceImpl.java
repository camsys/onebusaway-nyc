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

package org.onebusaway.api.users.impl;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;
import org.onebusaway.api.users.services.ApiKeyThrottlingCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import javax.annotation.PostConstruct;
import java.util.TimerTask;

public class ApiKeyThrottlingMemcacheServiceImpl implements
		ApiKeyThrottlingCacheService {

	private static Logger _log = LoggerFactory
			.getLogger(ApiKeyThrottlingMemcacheServiceImpl.class);
	
	@Autowired
	private ThreadPoolTaskScheduler _taskScheduler;
	
	private String cacheHost = "sessions-memcache";
	
	private String cachePort = "11211";
	
	private Boolean disabled = false;

	private static final int INTERVAL_SECONDS = 1000;

	private MemcachedClient _cache = null;

	@PostConstruct
	private void setup() {
		if(!disabled){
			initializeMemcache();
			startMemcacheCheck();
		}
	}

	private void initializeMemcache() {
		try {
			if (_cache == null) {
				String addr = cacheHost + ":" + cachePort;
				_cache = new MemcachedClient(new BinaryConnectionFactory(),
					AddrUtil.getAddresses(addr));
			}
		} catch (Exception e) {
			_log.error("Error occurred when establishing connection to memcache client : "
					+ cacheHost + ":" + cachePort , e);
			_cache = null;
		}
	}

	private void startMemcacheCheck() {
		if (_taskScheduler != null) {
			MemcacheCheckThread memcacheCheckThread = new MemcacheCheckThread();
			_taskScheduler.scheduleWithFixedDelay(memcacheCheckThread,
					INTERVAL_SECONDS * 300);
		}
	}

	public long incr(String key, int by, long defaul, int expiration) throws NullPointerException {
		return _cache.incr(key, by, defaul, expiration);
	}

	public boolean isCacheAvailable() {
		return _cache != null;
	}
	
	public String getCacheHost(){
		return cacheHost;
	}
	
	public void setCacheHost(String cacheHost){
		this.cacheHost = cacheHost;
	}
	
	public String getCachePort(){
		return cachePort;
	}

	public void setCachePort(String cachePort){
		this.cachePort = cachePort;
	}
	
	public Boolean getDisabled(){
		return disabled;
	}

	public void setDisabled(Boolean disabled){
		this.disabled = disabled;
	}

	private class MemcacheCheckThread extends TimerTask {
		@Override
		public void run() {
			if (!isCacheAvailable()) {
				_log.warn("Memcache is unavailable. Attempting to reinitialize...");
				initializeMemcache();
			}
		}
	}
}
