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
package org.onebusaway.nyc.transit_data_federation.impl.queue;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;
import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zeromq.ZMQ;

public abstract class InferenceQueueListenerTask {

  private static Logger _log = LoggerFactory.getLogger(InferenceQueueListenerTask.class);

  private ExecutorService _executorService = null;

  private ObjectMapper _mapper = new ObjectMapper();

  @Autowired
  protected ConfigurationService _configurationService;

  protected boolean _initialized = false;	

  protected abstract void processResult(NycQueuedInferredLocationBean inferredResult, String contents);

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
          @SuppressWarnings("unused")
          String address = new String(_zmqSocket.recv(0));
          String contents = new String(_zmqSocket.recv(0));

          try {
            NycQueuedInferredLocationBean inferredResult = 
                _mapper.readValue(contents, NycQueuedInferredLocationBean.class);

            processResult(inferredResult, contents);

          } catch(IOException e) {
            _log.info("Could not de-serialize inferred location record: " + e.getMessage()); 
            continue;
          }

          Thread.yield();
        }

        if(processedCount > 50) {
          _log.info("Inference output, TDS input queue: processed 50 messages in " 
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
    // use JAXB annotations so that we pick up anything from the auto-generated XML classes
    // generated from XSDs
    AnnotationIntrospector jaxb = new JaxbAnnotationIntrospector();
    _mapper.getDeserializationConfig().setAnnotationIntrospector(jaxb);

    _executorService = Executors.newFixedThreadPool(1);
    startListenerThread();
  }

  @PreDestroy 
  public void destroy() {
    _executorService.shutdownNow();
  }

  @Refreshable(dependsOn = {"tds.inputQueueHost", "tds.inputQueuePort", "tds.inputQueueName"})
  public void startListenerThread() {
    if(_initialized == true) {
      _log.warn("Configuration service tried to reconfigure TDS input queue reader; this service is not reconfigurable once started.");
      return;
    }

    String host = _configurationService.getConfigurationValueAsString("tds.inputQueueHost", null);
    String queueName = _configurationService.getConfigurationValueAsString("tds.inputQueueName", null);
    Integer port = _configurationService.getConfigurationValueAsInteger("tds.inputQueuePort", 5567);

    if(host == null) {
      _log.info("TDS input queue is not attached; input hostname was not available via configuration service.");
      return;
    }
    
    _log.info("real-time archive listening on " + host + ":" + port + ", queue=" + queueName);
    initializeZmq(host, queueName, port);

    _initialized = true;
  }	

  // FIXME why is this split out like this?
  protected void initializeZmq(String host, String queueName, Integer port) {
    String bind = "tcp://" + host + ":" + port;

    ZMQ.Context context = ZMQ.context(1);

    ZMQ.Socket socket = context.socket(ZMQ.SUB);	    	
    ZMQ.Poller poller = context.poller(1);
    poller.register(socket, ZMQ.Poller.POLLIN);

    socket.connect(bind);
    socket.subscribe(queueName.getBytes());

    _executorService.execute(new ReadThread(socket, poller));

    _log.debug("TDS input queue is listening on " + bind);
  }
}
