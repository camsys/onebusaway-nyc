package org.onebusaway.nyc.webapp.users.impl;

import java.util.TimerTask;

import javax.annotation.PostConstruct;

import org.onebusaway.nyc.webapp.users.services.ApiKeyThrottlingCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import net.spy.memcached.AddrUtil;
import net.spy.memcached.BinaryConnectionFactory;
import net.spy.memcached.MemcachedClient;

public class ApiKeyThrottlingMemcacheServiceImpl implements
		ApiKeyThrottlingCacheService {

	private static Logger _log = LoggerFactory
			.getLogger(ApiKeyThrottlingMemcacheServiceImpl.class);
	
	@Autowired
	private ThreadPoolTaskScheduler _taskScheduler;
	
	private String cacheHost = "sessions-memcache";
	
	private String cachePort = "11211";

	private static final int INTERVAL_SECONDS = 1000;

	private MemcachedClient _cache = null;

	@PostConstruct
	private void setup() {
		initializeMemcache();
		startMemcacheCheck();
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

	public long incr(String key, int by, long defaul, int expiration) {
		return _cache.incr(key, by, defaul, expiration);
	}

	public boolean isCacheAvailable() {
		return _cache == null;
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
