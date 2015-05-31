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

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.queue.DNSResolver;
import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.OutputQueueSenderService;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.context.ServletContextAware;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletContext;

/**
 * Sends inference output to the inference output queue.
 * 
 * @author sheldonabrown
 *
 */
public class OutputQueueSenderServiceImpl implements OutputQueueSenderService,
    ServletContextAware {

  private static Logger _log = LoggerFactory.getLogger(OutputQueueSenderServiceImpl.class);

  private static final long HEARTBEAT_INTERVAL = 1000l;

  private static final String HEARTBEAT_TOPIC = "heartbeat";

  private ExecutorService _executorService = null;

  private ExecutorService _heartbeatService = null;

  private final ArrayBlockingQueue<String> _outputBuffer = new ArrayBlockingQueue<String>(
      100);

  private final ArrayBlockingQueue<String> _heartbeatBuffer = new ArrayBlockingQueue<String>(
      10);

  private boolean _initialized = false;

  public boolean _isPrimaryInferenceInstance = true;

  public String _primaryHostname = null;

  private final ObjectMapper _mapper = new ObjectMapper();

  protected DNSResolver _outputQueueResolver = null;

  protected DNSResolver _primaryResolver = null;

  protected ZMQ.Context _context = null;

  protected ZMQ.Socket _socket = null;

  protected int _countInterval = 100;
  
  @Autowired
  private ConfigurationService _configurationService;

  @Autowired
  private ThreadPoolTaskScheduler _taskScheduler;

  @Override
  public void setServletContext(ServletContext servletContext) {
    // check for primaryHost name
    String hostname = null;
    if (servletContext != null) {
      hostname = servletContext.getInitParameter("primary.host.name");
      _log.info("servlet context provied primary.host.name=" + hostname);
    }
    if (hostname != null) {
      setPrimaryHostname(hostname);
    }
  }
  
  public void setCountInterval(int countInterval) {
    this._countInterval = countInterval;  
  }

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
	_log.error("in output run");

      while (!Thread.currentThread().isInterrupted()) {
        String r = null;
        try {
          r = _outputBuffer.poll(250, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
          _log.error("SendThread interrupted (queue change?)", ie);
          return;
        }

        if (r != null) {
          if (_isPrimaryInferenceInstance) {
            // _zmqSocket.send(_topicName, ZMQ.SNDMORE);
            // _zmqSocket.send(r.getBytes(), 0);
          }
        }

        final String h = _heartbeatBuffer.poll();
        if (h != null) {
          if (_isPrimaryInferenceInstance) {
            // _zmqSocket.send(HEARTBEAT_TOPIC.getBytes(), ZMQ.SNDMORE);
            // _zmqSocket.send(h.getBytes(), 0);
            _log.debug("heartbeat=" + h);
          }
        }

        if (r == null){
          // skip output message below if no queue content
          continue;
        }

        if (processedCount > _countInterval) {
          long timeInterval = (new Date().getTime() - markTimestamp.getTime());
          _log.warn("Inference output queue(primary="
              + _isPrimaryInferenceInstance + "): processed " + _countInterval + " messages in "
              + (timeInterval / 1000)
              + " seconds; (" + (1000.0 * processedCount/timeInterval) + ") records/second.  "
              + "current queue length is " + _outputBuffer.size());

          markTimestamp = new Date();
          processedCount = 0;
        }

        processedCount++;
      }
    }
  }

  private class HeartbeatThread implements Runnable {

    private final long _interval;

    public HeartbeatThread(long interval) {
      _interval = interval;
    }

    @Override
    public void run() {

      try {
        while (!Thread.currentThread().isInterrupted()) {
          final long markTimestamp = System.currentTimeMillis();
          if (_isPrimaryInferenceInstance) {
            final String msg = getHeartbeatMessage(getPrimaryHostname(),
                markTimestamp, _interval);
            _heartbeatBuffer.put(msg);
          }
          Thread.sleep(_interval);
        }
      } catch (final InterruptedException ie) {
        _log.error(ie.toString());
      }
    }

    private String getHeartbeatMessage(String hostname, long timestamp,
        long interval) {
      /*
       * remember that only one IE for each depot will be transmitting at a
       * time, so we can use the primaryhostname to identify this IE in the
       * heartbeat message.
       */
      final String msg = "{\"heartbeat\": {\"hostname\":\"%1$s\",\"heartbeat_timestamp\":%2$s,\"heartbeat_interval\":%3$s}}";
      return String.format(msg, getPrimaryHostname(), timestamp, interval);
    }
  }

  private class OutputQueueCheckThread extends TimerTask {

    @Override
    public void run() {
      try {
        if (_outputQueueResolver.hasAddressChanged()) {
          _log.warn("Resolver Changed");
          reinitializeQueue();
        }
      } catch (final Exception e) {
        _log.error(e.toString());
        _outputQueueResolver.reset();
      }
    }
  }

  private class PrimaryCheckThread extends TimerTask {

    @Override
    public void run() {
      try {
        final boolean primaryValue = _primaryResolver.isPrimary();
        if (primaryValue != _isPrimaryInferenceInstance) {
          _log.warn("Primary inference status changed to " + primaryValue);
          _isPrimaryInferenceInstance = primaryValue;
        }
      } catch (final Exception e) {
        _log.error(e.toString());
      }
    }
  }

  @Override
  public void enqueue(NycQueuedInferredLocationBean r) {
    try {
      final StringWriter sw = new StringWriter();
      final MappingJsonFactory jsonFactory = new MappingJsonFactory();
      final JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
      _mapper.writeValue(jsonGenerator, r);
      sw.close();

      _outputBuffer.put(sw.toString());
    } catch (final IOException e) {
      _log.info("Could not serialize inferred location record: "
          + e.getMessage());
    } catch (final InterruptedException e) {
      // discard if thread is interrupted or serialization fails
      return;
    }
  }

  @PostConstruct
  public void setup() {
    _outputQueueResolver = new DNSResolver(getQueueHost());
    final OutputQueueCheckThread outputQueueCheckThread = new OutputQueueCheckThread();
    // every 10 seconds
    _taskScheduler.scheduleWithFixedDelay(outputQueueCheckThread, 10 * 1000);

    if (getPrimaryHostname() != null && getPrimaryHostname().length() > 0) {
      _primaryResolver = new DNSResolver(getPrimaryHostname());
      _log.warn("Primary Inference instance configured to be "
          + getPrimaryHostname() + " on "
          + _primaryResolver.getLocalHostString());
      final PrimaryCheckThread primaryCheckThread = new PrimaryCheckThread();
      _taskScheduler.scheduleWithFixedDelay(primaryCheckThread, 10 * 1000);
    }
    _executorService = Executors.newFixedThreadPool(1);
    _heartbeatService = Executors.newFixedThreadPool(1);
    startListenerThread();
  }

  @PreDestroy
  public void destroy() {
    _executorService.shutdownNow();
    _heartbeatService.shutdownNow();
  }

  @Refreshable(dependsOn = {
      "inference-engine.outputQueueHost", "inference-engine.outputQueuePort",
      "inference-engine.outputQueueName"})
  public void startListenerThread() {
    if (_initialized == true) {
      _log.warn("Configuration service tried to reconfigure inference output queue service; this service is not reconfigurable once started.");
      return;
    }

    final String host = getQueueHost();
    final String queueName = getQueueName();
    final Integer port = getQueuePort();

    if (host == null || queueName == null || port == null) {
      _log.info("Inference output queue is not attached; output hostname was not available via configuration service.");
      return;
    }

    try {
      initializeQueue(host, queueName, port);
    } catch (final Exception any) {
      _outputQueueResolver.reset();
    }

  }

  protected void reinitializeQueue() {
    try {
      initializeQueue(getQueueHost(), getQueueName(), getQueuePort());
    } catch (final InterruptedException ie) {
      _log.error("reinitialize failed:", ie);
      _outputQueueResolver.reset();
      return;
    }
  }

  protected synchronized void initializeQueue(String host, String queueName,
      Integer port) throws InterruptedException {
    final String bind = "tcp://" + host + ":" + port;
    _log.warn("binding to " + bind);
    if (_context == null) {
      _context = ZMQ.context(1);
    }
    if (_socket != null) {
      _executorService.shutdownNow();
      _heartbeatService.shutdownNow();
      Thread.sleep(1 * 1000);
      _log.warn("_executorService.isTerminated="
          + _executorService.isTerminated());
      _socket.close();
      _executorService = Executors.newFixedThreadPool(1);
      _heartbeatService = Executors.newFixedThreadPool(1);
    }

    _log.info("binding to " + bind);
    _socket = _context.socket(ZMQ.PUB);
    _socket.connect(bind);
    _executorService.execute(new SendThread(_socket, queueName));
    _heartbeatService.execute(new HeartbeatThread(HEARTBEAT_INTERVAL));

    _log.info("Inference output queue is sending to " + bind);
    _initialized = true;
  }

  public String getQueueHost() {
    return _configurationService.getConfigurationValueAsString(
        "inference-engine.outputQueueHost", null);
  }

  public String getQueueName() {
    return _configurationService.getConfigurationValueAsString(
        "inference-engine.outputQueueName", null);
  }

  public Integer getQueuePort() {
    return _configurationService.getConfigurationValueAsInteger(
        "inference-engine.outputQueuePort", 5566);
  }

  @Override
  public void setIsPrimaryInferenceInstance(boolean isPrimary) {
    _isPrimaryInferenceInstance = isPrimary;
  }

  @Override
  public boolean getIsPrimaryInferenceInstance() {
    return _isPrimaryInferenceInstance;
  }

  @Override
  public void setPrimaryHostname(String hostname) {
    _primaryHostname = hostname;
  }

  @Override
  public String getPrimaryHostname() {
    return _primaryHostname;
  }

}
