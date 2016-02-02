/**
 * Copyright (C) 2016 Philip Matuskiewicz <philip.matuskiewicz@nyct.com>
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
 * 
 */

/***
 * Documentation for throttling:
 * 
 * This throttle service appends on 2 TDM configuration values and requires a memcache connection to run properly *for load balanced apps*
 * 
 * TDM Variables:
 * 
 * To enable Throttling... use tds.APIThrottlingEnabled = 1
 * curl -X POST --data-ascii '{"config":{"value":"1"}}' http://tdm.CHANGEME/api/config/tds/tds.APIThrottlingEnabled/set --header "Content-Type:application/json"
 * 
 * For throttling exceptions in api keys, insert a list of values in format 
 * "<API Key>,<Limit Exception, 0 for no limit>;<API Key>,<Limit Exception, 0 for no limit>;...;..."
 * curl -X POST --data-ascii '{"config":{"value":"CHANGEME"}}' http://tdm.CHANGEME/api/config/tds/tds.ApiThrottlingExceptions/set --header "Content-Type:application/json"
 * 
 * For a default calls per minute allowed, use this configuration, this is updated hourly by a running frontend 
 * 	----(This is due to lack of Refreshable annotation availability that would normally be available from within the TDS component)
 * curl -X POST --data-ascii '{"config":{"value":"60"}}' http://tdm.CHANGEME/api/config/tds/tds.AllowedApiCallsPerMinute/set --header "Content-Type:application/json"
 * 
 * 
 * Memcache:
 * A memcache daemon must be running... edit the dns resolver to redirect apithrottlememcache to the correct memcache node
 * Memcache on the node must be running on port 11211 which is the standard port
 * 
 */
package org.onebusaway.nyc.webapp.users.impl;

import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.webapp.users.services.ApiKeyThrottledService;
import org.onebusaway.nyc.webapp.users.services.ApiKeyThrottlingMemcacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;

@Component
public class ApiKeyThrottledServiceImpl implements ApiKeyThrottledService {

  private static Logger _log = LoggerFactory.getLogger(ApiKeyThrottledServiceImpl.class);
  
  @Autowired
  private ApiKeyThrottlingMemcacheService _memcache;
  
  @Autowired
  private ConfigurationService _config;
  
  private static final int ALLOWED_HITS_PER_MINUTE = 60;//60 by default or 1 per second
  private static final long UPDATE_INTERVAL = 3600000L;//60000 = 1 minute, 3600000L is 60 minutes or an hour
  
  private long _lastTDMUpdate = 0;
  
  private int _hitsAllowedPerMinute = 0;
  
  private int _TDMEnable = 0;
  
  //more than 100 exceptions would be unexpected, even for a large deployment.  Remember that bans are handled separately.
  private Map<String, Integer> _exceptions = new HashMap<String, Integer>(100);
  
  public void setServicesForTesting(ConfigurationService cs, ApiKeyThrottlingMemcacheService mc){
	  _memcache = mc;
	  _config = cs;
  }
  
  //Note refreshable won't help us here since the interface doesn't work over hessian for some reason
  private void refreshConfigFromTDM(){
    
	_lastTDMUpdate = System.currentTimeMillis();
	
	_TDMEnable = _config.getConfigurationValueAsInteger("tds.APIThrottlingEnabled", 0);
    
    _hitsAllowedPerMinute = _config.getConfigurationValueAsInteger("tds.AllowedApiCallsPerMinute", ALLOWED_HITS_PER_MINUTE);
    _log.info("Set allowed API hits allowed per minute to "+_hitsAllowedPerMinute);
    
    //ApiThrottlingExceptions will be semicolon separated in the format <API Key>,<Limit Exception, 0 for no limit>;...;...
    //e.g. {"value":"TEST,1;OBANYC,0"}
    
    HashMap<String, Integer> ehm = new HashMap<String, Integer>(100);
    
    String exceptionString = _config.getConfigurationValueAsString("tds.ApiThrottlingExceptions", "");
    String[] exceptions = exceptionString.split(";");
    if(exceptions.length > 0){
    	for(String exception : exceptions){
    		String[] tuple = exception.split(",");//in format <API Key>,<Limit Exception, 0 for no limit>
    		if(tuple.length == 2){
    			//add the exception!
    			int limit = 0;
    			try{
    				limit = Integer.parseInt(tuple[1]);
    			}catch(NumberFormatException nfe){
    				_log.error("Encountered a number format exception for api throttling key "+tuple[0]+" caused by: "+tuple[1]+". Won't add this exception.");
    				continue;//don't try adding this
    			}
    			ehm.put(tuple[0], limit);
    			_log.info("Added exception for api throttling key "+tuple[0]+" with limit (0 for none): "+limit);
    		}
    	}
    }
    
    if(ehm.size() > 0){
    	_exceptions.clear();
    	_exceptions.putAll(ehm);
    }
    
  }
  
  @Override
  public boolean isAllowed(String key) {
	  
    //we would only get here if the app is running, need to get the first setting from the TDM and refreshable won't work due to hessian connection to tdm
    if(_hitsAllowedPerMinute == 0 || (System.currentTimeMillis() - _lastTDMUpdate) > UPDATE_INTERVAL){
      refreshConfigFromTDM();
    }
    
	//catch all
	if(key == null || _memcache == null || _TDMEnable < 1)
	  return true;
    
    //is the user on the exception list?  if so, let's allow them if they're set to 0
    //banning is allowed in the general permissions page, therefore it isn't necessary to set that here
    int allowedExceptionHits = -1;
    if(_exceptions.containsKey(key)){
      allowedExceptionHits = _exceptions.get(key);
      if(allowedExceptionHits == 0){
        return true;
      }
    }
    
    //start all keys with "hctr_" to avoid collisions when using an existing cache
    key = "hctr_".concat(key);
    
    long value = -1;
    try{
      //Incr does NOT reset the expiration every time, therefore this will expire after 60 seconds, regardless of updates to it
      value = _memcache.incr(key, 1, 0, 60);//key, increment by, default if not there, expires in seconds
    }catch(Exception e){
      //Don't interrupt the request if we can't touch memcache right now... this could cause someone grief
      _log.info("There was an exception from the Memcache client, going to allow the api request through.");
      e.printStackTrace();
      return true;
    }
    
    //something went wrong with getting a good value from memcache, just allow the request through
    if(value < 0){
      _log.info("There was a logical exception from the Memcache client, going to allow the api request through (value="+value+").");
      return true;
    }
    
    //exceptional user, that has a different limit from the default
    if(allowedExceptionHits != -1){
      if(value < allowedExceptionHits){
        return true;
      }
      return false;
    }
    
    //general case
    if(value < _hitsAllowedPerMinute){
      return true;
    }
	  
	return false;
  }

}