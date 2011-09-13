/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.transit_data_federation.services.tdm.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zeromq.ZMQ;

@Component
public class PartitionedInputQueueListener {

	private static Logger _log = LoggerFactory.getLogger(PartitionedInputQueueListener.class);
	
	private ExecutorService _executorService = null;
	
	@Autowired
	private ConfigurationService _configurationService;
		
	private boolean alreadyInitialized = false;
	
	private static void processMessage(String address, String contents) {
		System.out.println("MSG RECEIVED!");
	}	
	
	private class ReadThread implements Runnable {

		private ZMQ.Socket _zmqSocket = null;

		private ZMQ.Poller _zmqPoller = null;
		
		public ReadThread(ZMQ.Socket socket, ZMQ.Poller poller) {
			_zmqSocket = socket;
			_zmqPoller = poller;
		}

		@Override
		public void run() {
		    while (true) {
				_zmqPoller.poll();
				if(_zmqPoller.pollin(0)) {
					String address = new String(_zmqSocket.recv(0));
					String contents = new String(_zmqSocket.recv(0));

			    	processMessage(address, contents);		    	
				}
		    }	    
		}
	}
	
	@PostConstruct
	public void setup() {
		_executorService = Executors.newFixedThreadPool(1);
		startListenerThread();
	}
	
	@PreDestroy 
	public void destroy() {
		_executorService.shutdownNow();
	}
	
	@Refreshable(dependsOn = {"inference-engine.inputQueueHost", "inference-engine.inputQueuePort"})
	public void startListenerThread() {
		if(alreadyInitialized == true) {
			_log.warn("Configuration service tried to reconfigure queue read pool; this service is not reconfigurable.");
			return;
		}
		
		alreadyInitialized = true;
		
		String host = _configurationService.getConfigurationValueAsString("inference-engine.inputQueueHost", null);
	    Integer port = _configurationService.getConfigurationValueAsInteger("inference-engine.inputQueuePort", 5563);

	    if(host == null) {
	    	_log.info("Input queue is not attached; input hostname was not available via configuration service.");
	    	return;
	    }

	    String bind = "tcp://" + host + ":" + port;

		ZMQ.Context context = ZMQ.context(1);

		ZMQ.Socket socket = context.socket(ZMQ.SUB);	    	
	    ZMQ.Poller poller = context.poller(2);
	    poller.register(socket, ZMQ.Poller.POLLIN);
	    
	    socket.connect(bind);
	    socket.subscribe("bhs_queue".getBytes());

	    _executorService.execute(new ReadThread(socket, poller));

		_log.debug("Input queue is listening on " + bind);
	}	
}
