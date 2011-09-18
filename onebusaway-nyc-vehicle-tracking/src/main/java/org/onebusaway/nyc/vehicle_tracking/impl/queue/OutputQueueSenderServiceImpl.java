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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zeromq.ZMQ;

public class OutputQueueSenderServiceImpl implements OutputQueueSenderService {

	private static Logger _log = LoggerFactory.getLogger(OutputQueueSenderServiceImpl.class);
		
	private ExecutorService _executorService = null;

	private ArrayBlockingQueue<String> _outputBuffer = new ArrayBlockingQueue<String>(100);
	
	private boolean initialized = false;

	@Autowired
	private ConfigurationService _configurationService;

	private class SendThread implements Runnable {

		int processedCount = 0;
		
		Date markTimestamp = new Date();
		
		private ZMQ.Socket _zmqSocket = null;

		private byte[] _topicName = null;
		
		public SendThread(ZMQ.Socket socket, String topicName) {
			_zmqSocket = socket;
			_topicName = topicName.getBytes();
		}

		@Override
		public void run() {
		    while(true) {		    	
	    		String r = _outputBuffer.poll();
	    		if(r == null)
	    			continue;
	    		
	    		_zmqSocket.send(_topicName, ZMQ.SNDMORE);
	    		_zmqSocket.send(r.getBytes(), 0);

		    	Thread.yield();

				if(processedCount > 50) {
					_log.info("Inference output queue: processed 50 messages in " 
							+ (new Date().getTime() - markTimestamp.getTime()) / 1000 + 
							" seconds; current queue length is " + _outputBuffer.size());
					
					markTimestamp = new Date();
					processedCount = 0;
				}
				
				processedCount++;		    	
	    	}
		}
	}	
	
	@Override
	public void enqueue(NycQueuedInferredLocationBean r) {
		try {
			ObjectMapper mapper = new ObjectMapper();		
		    StringWriter sw = new StringWriter();
		    MappingJsonFactory jsonFactory = new MappingJsonFactory();
		    JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
		    mapper.writeValue(jsonGenerator, r);
		    sw.close();			
			
			_outputBuffer.put(sw.toString());
		} catch(IOException e) {
			_log.info("Could not serialize inferred location record: " + e.getMessage()); 
		} catch(InterruptedException e) {
			// discard if thread is interrupted or serialization fails
			return;
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
	
	@Refreshable(dependsOn = {"inference-engine.outputQueueHost", 
			"inference-engine.outputQueuePort", "inference-engine.outputQueueName"})
	public void startListenerThread() {
		if(initialized == true) {
			_log.warn("Configuration service tried to reconfigure inference output queue service; this service is not reconfigurable once started.");
			return;
		}
		
		String host = _configurationService.getConfigurationValueAsString("inference-engine.outputQueueHost", null);
		String queueName = _configurationService.getConfigurationValueAsString("inference-engine.outputQueueName", null);
	    Integer port = _configurationService.getConfigurationValueAsInteger("inference-engine.outputQueuePort", 5566);

	    if(host == null || queueName == null || port == null) {
	    	_log.info("Inference output queue is not attached; output hostname was not available via configuration service.");
	    	return;
	    }

	    String bind = "tcp://" + host + ":" + port;

		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket socket = context.socket(ZMQ.PUB);	    	
	    
	    socket.connect(bind);

	    _executorService.execute(new SendThread(socket, queueName));

		_log.debug("Inference output queue is sending to " + bind);
		initialized = true;
	}

	@Override
	public void setIsPrimaryInferenceInstance(boolean isPrimary) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean getIsPrimaryInferenceInstance() {
		// TODO Auto-generated method stub
		return false;
	}	
}
