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

package org.onebusaway.nyc.api.lib;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.onebusaway.nyc.api.lib.impl.ApiKeyThrottledServiceImpl;
import org.onebusaway.nyc.api.lib.services.ApiKeyThrottlingCacheService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class ApiKeyThrottledTest {

  // getRouteResult tests
  
  @Test
  public void testBypassFunctionality() {
	ConfigurationServiceMockImpl configurationService = new ConfigurationServiceMockImpl();
    ApiKeyThrottledServiceImpl apiThrottleService = new ApiKeyThrottledServiceImpl();
	  
    //set disable
    try {
		configurationService.setConfigurationValue("tds", "tds.APIThrottlingEnabled", "0");
	} catch (Exception e) {
		assertTrue("The configuration service failed in the throttling test", false);
		e.printStackTrace();
	}
    
    //no memcache needed
    apiThrottleService.setServicesForTesting(configurationService, null);
    
    for(int i = 0; i<1000; i++){
    	assertTrue("The API call should be allowed here", apiThrottleService.isAllowed("BLAH"));
    }
	  
  }

  @Test
  public void testMemcacheDownAllowAll() {
	ConfigurationServiceMockImpl configurationService = new ConfigurationServiceMockImpl();
    ApiKeyThrottledServiceImpl apiThrottleService = new ApiKeyThrottledServiceImpl();
	  
    //set disable
    try {
		configurationService.setConfigurationValue("tds", "tds.APIThrottlingEnabled", "1");
		configurationService.setConfigurationValue("tds", "tds.AllowedApiCallsPerMinute", "5");
	} catch (Exception e) {
		assertTrue("The configuration service failed in the throttling test", false);
		e.printStackTrace();
	}
    
    //no memcache needed
    apiThrottleService.setServicesForTesting(configurationService, null);
    
    for(int i = 0; i<1000; i++){
    	assertTrue("The API call should be allowed here", apiThrottleService.isAllowed("TEST"));
    }
    
  }
  
  @Test
  public void testMemcacheUpAllowFive() {
	ConfigurationServiceMockImpl configurationService = new ConfigurationServiceMockImpl();
    ApiKeyThrottledServiceImpl apiThrottleService = new ApiKeyThrottledServiceImpl();
	  
    //set disable
    try {
		configurationService.setConfigurationValue("tds", "tds.APIThrottlingEnabled", "1");
		configurationService.setConfigurationValue("tds", "tds.AllowedApiCallsPerMinute", "5");
	} catch (Exception e) {
		assertTrue("The configuration service failed in the throttling test", false);
		e.printStackTrace();
	}
    
    //create memcache
    ApiKeyThrottlingCacheService m = new mockMemcacheService();
    apiThrottleService.setServicesForTesting(configurationService, m);
    
    for(int i = 0; i<5; i++){
    	assertTrue("The API call should be allowed here", apiThrottleService.isAllowed("TEST"));
    }
    
    assertFalse("The API call should be denied here", apiThrottleService.isAllowed("TEST"));
    
  }
  
  @Test
  public void testMemcacheUpAllowFiveForTwoKeys() {
	ConfigurationServiceMockImpl configurationService = new ConfigurationServiceMockImpl();
    ApiKeyThrottledServiceImpl apiThrottleService = new ApiKeyThrottledServiceImpl();
	  
    //set disable
    try {
		configurationService.setConfigurationValue("tds", "tds.APIThrottlingEnabled", "1");
		configurationService.setConfigurationValue("tds", "tds.AllowedApiCallsPerMinute", "5");
	} catch (Exception e) {
		assertTrue("The configuration service failed in the throttling test", false);
		e.printStackTrace();
	}
    
    //create memcache
    ApiKeyThrottlingCacheService m = new mockMemcacheService();
    apiThrottleService.setServicesForTesting(configurationService, m);
    
    for(int i = 0; i<5; i++){
    	assertTrue("The API call should be allowed here", apiThrottleService.isAllowed("TEST"));
    	assertTrue("The API call should be allowed here", apiThrottleService.isAllowed("OBANYC"));
    }
    
    assertFalse("The API call should be denied here", apiThrottleService.isAllowed("TEST"));
    assertFalse("The API call should be denied here", apiThrottleService.isAllowed("OBANYC"));
  }
  
  //mock configuration service that I can work with
  private class ConfigurationServiceMockImpl implements ConfigurationService{

	private Map<String, String> map = new HashMap<String, String>();
	  
	@Override
	public String getConfigurationValueAsString(String configurationItemKey, String defaultValue) {
		if(map.containsKey(configurationItemKey))
			return map.get(configurationItemKey);
		return defaultValue;
	}

	@Override
	public Float getConfigurationValueAsFloat(String configurationItemKey, Float defaultValue) {
		if(map.containsKey(configurationItemKey)){
			return Float.parseFloat(map.get(configurationItemKey));
		}
		return defaultValue;
	}

	@Override
	public Integer getConfigurationValueAsInteger(String configurationItemKey, Integer defaultValue) {
		if(map.containsKey(configurationItemKey)){
			return Integer.parseInt(map.get(configurationItemKey));
		}
		return defaultValue;
	}

  	@Override
  	public Boolean getConfigurationValueAsBoolean(String configurationItemKey, Boolean defaultValue) {
	  	if(map.containsKey(configurationItemKey)){
		  	return Boolean.parseBoolean(map.get(configurationItemKey));
	  	}
	  	return defaultValue;
  	}

	@Override
	public void setConfigurationValue(String component, String configurationItemKey, String value) throws Exception {
		map.put(configurationItemKey, value);
	}

	@Override
	public Map<String, String> getConfiguration() {
		return map;
	}
	  
  }

  private class mockMemcacheService implements ApiKeyThrottlingCacheService{

	private HashMap<String, Integer> _hm = new HashMap<String, Integer>();
	  
	@Override
	//expiration not implemented
	public long incr(String key, int by, long defaul, int expiration) {
		if(_hm.containsKey(key)){
			int val = _hm.get(key) + 1;
			_hm.put(key, val);
			return val;
		}
		_hm.put(key, (int)defaul);
		return defaul;
	}

	@Override
	public boolean isCacheAvailable() {
		return true;
	}

	@Override
	public String getCacheHost() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCacheHost(String cacheHost) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getCachePort() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCachePort(String cachePort) {
		// TODO Auto-generated method stub
		
	}
	  
  }
  
}
