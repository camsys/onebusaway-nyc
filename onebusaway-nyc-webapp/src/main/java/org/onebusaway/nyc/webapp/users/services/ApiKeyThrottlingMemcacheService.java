package org.onebusaway.nyc.webapp.users.services;

//written to enable alternate memcache implementations to enable testing using this interface
public interface ApiKeyThrottlingMemcacheService {
	  
	public long incr(String key, int by, long defaul, int expiration);
	
}
