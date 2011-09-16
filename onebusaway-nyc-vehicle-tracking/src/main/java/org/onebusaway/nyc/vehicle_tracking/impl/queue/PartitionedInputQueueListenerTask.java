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

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.services.tdm.ConfigurationService;
import org.onebusaway.nyc.transit_data_federation.services.tdm.VehicleAssignmentService;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationInferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zeromq.ZMQ;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;

public class PartitionedInputQueueListenerTask {

	private static Logger _log = LoggerFactory.getLogger(PartitionedInputQueueListenerTask.class);
	
	private static String depotPartitionKey = "JG";
		
	private ExecutorService _executorService = null;
	
	@Autowired
	private VehicleLocationInferenceService _vehicleLocationService;
	
	@Autowired 
	private VehicleAssignmentService _vehicleAssignmentService;
	
	@Autowired
	private ConfigurationService _configurationService;
		
	private boolean initialized = false;
	
	private void processMessage(CcLocationReport message) {
	    _vehicleLocationService.handleCcLocationReportRecord(message);
	}
	
	// listen for messages directed to us (in our partition) and process them
	private void filterMessages(String address, String contents) {
	
		ArrayList<AgencyAndId> vehicleList = 
				_vehicleAssignmentService.getAssignedVehicleIdsForDepot(depotPartitionKey);

		CcLocationReport message = null;
		try {
			ObjectMapper mapper = new ObjectMapper();		
		    AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector();
			mapper.getDeserializationConfig().setAnnotationIntrospector(jaxb);
			
			JsonNode wrappedMessage = mapper.readValue(contents, JsonNode.class);
			String ccLocationReportString = wrappedMessage.get("CcLocationReport").toString();
			
			message = mapper.readValue(ccLocationReportString, CcLocationReport.class);
		} catch(Exception e) {
			_log.warn("Received corrupted message from queue; discarding");
			return;
		}			

	    CPTVehicleIden vehicleIdent = message.getVehicle();
	    AgencyAndId vehicleId = 
				new AgencyAndId(vehicleIdent.getAgencydesignator(), vehicleIdent.getVehicleId() + "");

		if(!vehicleList.contains(vehicleId))
			return;

		processMessage(message);
	}	
	
	private class ReadThread implements Runnable {

		int processedCount = 0;
		
		Date markTimestamp = new Date();
		
		private ZMQ.Socket _zmqSocket = null;

		private ZMQ.Poller _zmqPoller = null;
		
		public ReadThread(ZMQ.Socket socket, ZMQ.Poller poller) {
			_zmqSocket = socket;
			_zmqPoller = poller;
		}

		@Override
		public void run() {
		    while(true) {
				_zmqPoller.poll();
				if(_zmqPoller.pollin(0)) {
					String address = new String(_zmqSocket.recv(0));
					String contents = new String(_zmqSocket.recv(0));

			    	filterMessages(address, contents);		    	

			    	Thread.yield();
				}
				
				if(processedCount > 50) {
					_log.info("Inference input queue: processed 50 messages in " 
							+ (new Date().getTime() - markTimestamp.getTime()) / 1000 + " seconds.");
					
					markTimestamp = new Date();
					processedCount = 0;
				}
				
				processedCount++;
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
	
	@Refreshable(dependsOn = {"inference-engine.inputQueueHost", 
			"inference-engine.inputQueuePort", "inference-engine.inputQueueName"})
	public void startListenerThread() {
		if(initialized == true) {
			_log.warn("Configuration service tried to reconfigure queue reader; this service is not reconfigurable once started.");
			return;
		}
		
		String host = _configurationService.getConfigurationValueAsString("inference-engine.inputQueueHost", null);
		String queueName = _configurationService.getConfigurationValueAsString("inference-engine.inputQueueName", null);
	    Integer port = _configurationService.getConfigurationValueAsInteger("inference-engine.inputQueuePort", 5563);

	    if(host == null) {
	    	_log.info("Inference input queue is not attached; input hostname was not available via configuration service.");
	    	return;
	    }

	    String bind = "tcp://" + host + ":" + port;

		ZMQ.Context context = ZMQ.context(1);

		ZMQ.Socket socket = context.socket(ZMQ.SUB);	    	
	    ZMQ.Poller poller = context.poller(2);
	    poller.register(socket, ZMQ.Poller.POLLIN);
	    
	    socket.connect(bind);
	    socket.subscribe(queueName.getBytes());

	    _executorService.execute(new ReadThread(socket, poller));

		_log.debug("Inference input queue is listening on " + bind);
		initialized = true;
	}	
}
