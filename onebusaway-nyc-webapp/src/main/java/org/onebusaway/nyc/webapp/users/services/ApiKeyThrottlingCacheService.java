package org.onebusaway.nyc.webapp.users.services;

//written to enable alternate memcache implementations to enable testing using this interface
public interface ApiKeyThrottlingCacheService {
	  
	long incr(String key, int by, long defaul, int expiration) throws NullPointerException;
	boolean isCacheAvailable();
	String getCacheHost();
	void setCacheHost(String cacheHost);
	String getCachePort();
	void setCachePort(String cachePort);
	
}
