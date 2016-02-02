package org.onebusaway.nyc.webapp.users.impl;

import org.onebusaway.nyc.webapp.users.services.ApiKeyThrottlingMemcacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import net.spy.memcached.MemcachedClient;

//interface to Memcache implementation from spymemcached
@Component
public class ApiKeyThrottlingMemcacheServiceImpl implements ApiKeyThrottlingMemcacheService{

	  @Autowired(required=false)
	  @Qualifier("memcachedClientThrottled")
	  private MemcachedClient _client = null;
	
	  public long incr(String key, int by, long defaul, int expiration){
		  return _client.incr(key, by, defaul, expiration);
	  }
	  
}
