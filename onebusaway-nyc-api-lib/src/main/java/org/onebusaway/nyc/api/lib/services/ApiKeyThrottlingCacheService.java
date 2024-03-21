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

package org.onebusaway.nyc.api.lib.services;

//written to enable alternate memcache implementations to enable testing using this interface
public interface ApiKeyThrottlingCacheService {
	  
	long incr(String key, int by, long defaul, int expiration) throws NullPointerException;
	boolean isCacheAvailable();
	String getCacheHost();
	void setCacheHost(String cacheHost);
	String getCachePort();
	void setCachePort(String cachePort);
	
}
