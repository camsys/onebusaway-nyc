package org.onebusaway.nyc.queue_broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQForwarder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Simple Brokering impelmented in ZeroMQ. Listen for anything on one socket,
 * and send it back out on another socket.
 */
public class SimpleBroker {
  Logger logger = LoggerFactory.getLogger(SimpleBroker.class);

  private static final int DEFAULT_IN_PORT = 5566;
  private static final int DEFAULT_OUT_PORT = 5567;
  private ExecutorService _executorService = null;
  private int inPort;
  private int outPort;

  public static void main(String[] args) {
    SimpleBroker broker = new SimpleBroker();
    if (args.length > 0) {
      broker.setInPort(Integer.parseInt(args[0]));
    } else {
      broker.setInPort(DEFAULT_IN_PORT);
    }
    if (args.length > 1) {
      broker.setOutPort(Integer.parseInt(args[1]));
    } else {
      broker.setOutPort(DEFAULT_OUT_PORT);
    }

    broker.run();
  }

  public SimpleBroker() {
    logger.info("Starting up SimpleBroker");
  }

  public void run() {
    // Prepare our context and subscriber
    ZMQ.Context context = ZMQ.context(1); // 1 = number of threads

    ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
    String inBind = "tcp://*:" + inPort;

    logger.info("subscribing to queue at " + inBind);
    subscriber.bind(inBind);
    // subscribe to everything
    subscriber.subscribe(new byte[0]); // was inTopic.getBytes()

    ZMQ.Socket publisher = context.socket(ZMQ.PUB);
    String outBind = "tcp://*:" + outPort;

    logger.info("publishing to queue at " + outBind);
    publisher.bind(outBind);

    _executorService = Executors.newFixedThreadPool(1);
    ZMQForwarder broker = new ZMQForwarder(context, subscriber, publisher);
    _executorService.execute(broker);

  }

  public void setInPort(int inPort) {
    this.inPort = inPort;
  }

  public void setOutPort(int outPort) {
    this.outPort = outPort;
  }

}
