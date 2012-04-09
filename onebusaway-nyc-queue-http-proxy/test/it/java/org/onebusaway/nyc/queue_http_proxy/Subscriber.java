package org.onebusaway.nyc.queue_http_proxy;

import org.zeromq.ZMQ;

/**
 * Example usage of receiving a queue message.
 */
public class Subscriber {
    public static void main(String[] args) {
    // Prepare our context and subscriber
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket subscriber = context.socket(ZMQ.SUB);

    subscriber.connect("tcp://localhost:5563");
    subscriber.subscribe("bhs_queue".getBytes());
    while (true) {
      // Read envelope with address
      String address = new String(subscriber.recv(0));
      // optionally assert that address is what we expected
      // Read message contents
      String contents = new String(subscriber.recv(0));
      process(contents);
     }
  }

    private static void process(String content) {
	// do something
    }

}