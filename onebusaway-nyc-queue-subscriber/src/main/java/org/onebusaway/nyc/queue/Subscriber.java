package org.onebusaway.nyc.queue;

import org.zeromq.ZMQ;

public abstract class Subscriber {
  public static final String HOST_KEY = "mq.host";
  public static final String PORT_KEY = "mq.port";
  private static final String DEFAULT_HOST = "queue.staging.obanyc.com";
  private static final int DEFAULT_PORT = 5563;
  private ZMQ.Context context;
  private ZMQ.Socket socket;

  abstract void process(String address, String contents);  

  public void run() {
    String bind = setup();
    System.out.println("listening on " + bind);
    while (true) {
      readAndProcess();
    }
  }

  public void readAndProcess() {
    // Read envelope with address
    String address = new String(getSocket().recv(0));
    // Read message contents
    String contents = new String(getSocket().recv(0));
    process(address, contents);
  }

  public String setup() {
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
    getSocket().connect(bind);
    getSocket().subscribe("bhs_queue".getBytes());
    return bind;
  }

  public ZMQ.Context getContext() {
    if (context == null) {
      context = ZMQ.context(1); 
    }
    return context;
  }

  public void setContext(ZMQ.Context context) {
    this.context = context;
  }

  public ZMQ.Socket getSocket() {
    if (socket == null) {
      socket = getContext().socket(ZMQ.SUB);
    }
    return socket;
  }

  public void setSocket(ZMQ.Socket socket) {
    this.socket = socket;
  }
}
