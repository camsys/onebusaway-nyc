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

package org.onebusaway.nyc.queue_broker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import zmq.SocketBase;

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
  private static final int HWM_VALUE = 50000; // High Water Mark
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

    //Set to prevent broker memory from growing indefinitely if client falls behind
    publisher.setHWM(HWM_VALUE);

    String outBind = "tcp://*:" + outPort;

    logger.info("publishing to queue at " + outBind);
    publisher.bind(outBind);

    ZMQ.proxy(subscriber, publisher, null);

  }

  public void setInPort(int inPort) {
    this.inPort = inPort;
  }

  public void setOutPort(int outPort) {
    this.outPort = outPort;
  }

}
