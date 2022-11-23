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

package org.onebusaway.nyc.transit_data_federation.impl.queue;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.queue.QueueListenerTask;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;

public abstract class TimeQueueListenerTask extends QueueListenerTask {

		public enum Status {
			ENABLED, // read from queue
			TESTING, // don't read from queue, but allow cache to be read.
			DISABLED; // totally disabled
		};

  protected abstract void processResult(FeedMessage message);

	@Override
	public boolean processMessage(String address, byte[] buff) {
		try {
			if (address == null || buff == null || !address.equals(getQueueName()) || !useTimePredictionsIfAvailable()) {
				return false;
			}
			
			processResult(FeedMessage.parseFrom(buff));
			return true;
		} catch (Exception e) {
		  _log.warn("exception:", e);
			_log.warn("Received corrupted message from queue; discarding: " + e.getMessage());
			return false;
		}
	}
	
	@Override
	public String getQueueHost() {
		return _configurationService.getConfigurationValueAsString("tds.timePredictionQueueHost", null);
	}

	@Override
	public String getQueueName() {
		return _configurationService.getConfigurationValueAsString("tds.timePredictionQueueName", "time");
	}

	@Override
	public Integer getQueuePort() {
		return _configurationService.getConfigurationValueAsInteger("tds.timePredictionQueueOutputPort", 5569);
	}
	
  @Override
  public String getQueueDisplayName() {
    return "timePrediction";
  }
  
  private Status status = Status.ENABLED;
  
  public Status getStatus(){
	  return status;
  }
  
 	public void setStatus(Status status) {
  	this.status = status;
		}
  
  @Refreshable(dependsOn = { "display.useTimePredictions" })
  public Boolean useTimePredictionsIfAvailable() {
    if (!Status.ENABLED.equals(status)) return false;
    return Boolean.parseBoolean(_configurationService.getConfigurationValueAsString("display.useTimePredictions", "false"));
  }
  
	@Refreshable(dependsOn = { "tds.timePredictionQueueHost", "tds.timePredictionQueuePort", "tds.timePredictionQueueName" })
	public void startListenerThread() {
		if (_initialized == true) {
			_log.warn("Configuration service tried to reconfigure prediction input queue reader; this service is not reconfigurable once started.");
			return;
		}

		if (!useTimePredictionsIfAvailable()) {
		  _log.error("time predictions disabled -- exiting");
		  return;
		}
		
		String host = getQueueHost();
		String queueName = getQueueName();
		Integer port = getQueuePort();

		if (host == null) {
			_log.info("Prediction input queue is not attached; input hostname was not available via configuration service.");
			return;
		}

		_log.info("time prediction input queue listening on " + host + ":" + port + ", queue=" + queueName);

		try {
			initializeQueue(host, queueName, port);
		} catch (InterruptedException ie) {
			return;
		}

		_initialized = true;
	}

}
