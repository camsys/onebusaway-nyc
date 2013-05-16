package org.onebusaway.nyc.report_archive.queue;

import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.nyc.report_archive.services.DummyPredictionOutputQueueSenderService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.zeromq.ZMQ;

import com.google.transit.realtime.GtfsRealtime.FeedMessage;

public class DummyPredictionOutputQueueSenderServiceImpl implements DummyPredictionOutputQueueSenderService {

  private static Logger _log = LoggerFactory.getLogger(DummyPredictionOutputQueueSenderServiceImpl.class);
  
  private final ArrayBlockingQueue<FeedMessage> _outputBuffer = new ArrayBlockingQueue<FeedMessage>(
      100);
  
  private ExecutorService _executorService = null;
  
  protected ZMQ.Context _context = null;

  protected ZMQ.Socket _socket = null;
  
  private boolean _initialized = false;
  
  @Autowired
  private ConfigurationService _configurationService;
  
  @Override
  public void enqueue(FeedMessage message) {
    try {
      _outputBuffer.put(message);
    } catch (final InterruptedException e) {
      // discard if thread is interrupted or serialization fails
      return;
    }
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
      while (!Thread.currentThread().isInterrupted()) {
        FeedMessage r = null;
        try {
          r = _outputBuffer.poll(250, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ie) {
          _log.error("SendThread interrupted", ie);
          return;
        }

        if (r != null) {
            _zmqSocket.send(_topicName, ZMQ.SNDMORE);
            _zmqSocket.send(r.toByteArray(), 0);
        }



        if (r == null){
          // skip output message below if no queue content
          continue;
        }

        if (processedCount > 50) {
          _log.warn("Dummy Prediction output queue: processed 50 messages in "
              + (new Date().getTime() - markTimestamp.getTime()) / 1000
              + " seconds; current queue length is " + _outputBuffer.size());

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
  
  @Refreshable(dependsOn = {
      "tds.timePredictionQueueHost", "tds.timePredictionQueueInputPort",
      "tds.timePredictionQueueName"})
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
      _log.info("host=" + host + ", queueName=" + queueName + ", port=" + port);
      return;
    }

    try {
      initializeQueue(host, queueName, port);
    } catch (final Exception any) {
      _log.error("initQueue failed", any);
    }

  }
  
  protected void reinitializeQueue() {
    try {
      initializeQueue(getQueueHost(), getQueueName(), getQueuePort());
    } catch (final InterruptedException ie) {
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
      Thread.sleep(1 * 1000);
      _log.warn("_executorService.isTerminated="
          + _executorService.isTerminated());
      _socket.close();
      _executorService = Executors.newFixedThreadPool(1);
    }

    _socket = _context.socket(ZMQ.PUB);
    _socket.connect(bind);
    _executorService.execute(new SendThread(_socket, queueName));

    _log.debug("Inference output queue is sending to " + bind);
    _initialized = true;
  }

  public String getQueueHost() {
    return _configurationService.getConfigurationValueAsString(
        "tds.timePredictionQueueHost", null);
  }

  public String getQueueName() {
    return _configurationService.getConfigurationValueAsString(
        "tds.timePredictionQueueName", "time");
  }

  public Integer getQueuePort() {
    return _configurationService.getConfigurationValueAsInteger(
        "tds.timePredictionQueueInputPort", 5568);
  }

}
