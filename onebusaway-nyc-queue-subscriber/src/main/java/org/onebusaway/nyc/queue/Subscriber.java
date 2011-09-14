package org.onebusaway.nyc.queue;

import org.zeromq.ZMQ;

public class Subscriber {
    public static final String HOST_KEY = "mq.host";
    public static final String PORT_KEY = "mq.port";
    private static final String DEFAULT_HOST = "mq.dev.obanyc.org";
    private static final int DEFAULT_PORT = 5563;

    public static void main(String[] args) {

    // Prepare our context and subscriber
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket subscriber = context.socket(ZMQ.SUB);

    String host = DEFAULT_HOST;
    if (System.getProperty(HOST_KEY) != null) {
	host = System.getProperty(HOST_KEY);
    }
    int port = DEFAULT_PORT;
    if (System.getProperty(PORT_KEY) != null) {
	try {
	    port = Integer.parseInt(System.getProperty(PORT_KEY));
	} catch (NumberFormatException nfe) {
	    port = DEFAULT_PORT;
	}
    }

    String bind = "tcp://" + host + ":" + port;
    subscriber.connect(bind);
    subscriber.subscribe("bhs_queue".getBytes());
    System.out.println("listening on " + bind);
    while (true) {
      // Read envelope with address
      String address = new String(subscriber.recv(0));
      // Read message contents
      String contents = new String(subscriber.recv(0));
      process(address, contents);
     }
  }
    private static void process(String address, String contents) {
      System.out.println(address + " : " + contents);
    }
}
