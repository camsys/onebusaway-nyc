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

package org.onebusaway.nyc.vehicle_tracking.impl.monitoring;

import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.onebusaway.nyc.vehicle_tracking.services.monitoring.CloudWatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.amazonaws.services.cloudwatch.model.StandardUnit;
import com.google.common.util.concurrent.AtomicDouble;

public class NullBlockStateRouteMap {
	  
	  @Autowired
	  private CloudWatchService _cloudWatchService;
	
	  private final ConcurrentMap<String, AtomicDouble> _nullBlockStateRoutes = new ConcurrentHashMap<String, AtomicDouble>(10000);
	  
	  protected static final Logger _log = LoggerFactory
				.getLogger(NullBlockStateRouteMap.class);
	  
	  @PostConstruct
	  public void setup(){
	  	//startMonitoringThread();
	  }
	  
	  public void increment(String dsc){
		  _nullBlockStateRoutes.putIfAbsent(dsc, new AtomicDouble());
		  _nullBlockStateRoutes.get(dsc).addAndGet(1); 
	  }
	  
	  public void clear(String dsc){
		  _nullBlockStateRoutes.get(dsc).getAndSet(0);
	  }
	  
	  public void startMonitoringThread() {
		  ScheduledExecutorService executor =
				    Executors.newSingleThreadScheduledExecutor();
		  executor.scheduleAtFixedRate(new MonitoringThread(), 0, 60, TimeUnit.SECONDS);
	  }
	  
	  private class MonitoringThread extends TimerTask {
		@Override
		public void run() {
			_log.info("Posting null block state dscs to cloud watch");
			try{
				for(String dsc: _nullBlockStateRoutes.keySet()){
					Double nullBlockStatesCount = _nullBlockStateRoutes.get(dsc).doubleValue();
					if(nullBlockStatesCount != null){
						_cloudWatchService.publishMetricWithDim("MissingBlockState", StandardUnit.None, nullBlockStatesCount, "DSC", dsc);
						clear(dsc);
					}
				}
			}catch(Exception e){
				_log.error("Error posting null block state dscs to cloud watch", e);
			}
		}
	  }
}
