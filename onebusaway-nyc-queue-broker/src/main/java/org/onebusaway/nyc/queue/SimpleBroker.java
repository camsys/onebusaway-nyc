package org.onebusaway.nyc.queue;

import org.zeromq.ZMQ;

/**
 * Simple Brokering impelmented in ZeroMQ.
 * Listen for anything on one socket, and send it back out on another socket.
 */
public class SimpleBroker {
    private static final int DEFAULT_IN_PORT = 5566;
    private static final int DEFAULT_OUT_PORT = 5567;
    private static final String DEFAULT_IN_TOPIC = "inference_queue";
    private static final String DEFAULT_OUT_TOPIC = "inference_queue";
  	private static final String HEARTBEAT_TOPIC = "heartbeat";
    private int inPort;
    private int outPort;
    private String inTopic;
    private String outTopic;

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
	
	if (args.length > 2) {
	    broker.setInTopic(args[2]);
	} else {
	    broker.setInTopic(DEFAULT_IN_TOPIC);
	}

	if (args.length > 3) {
	    broker.setOutTopic(args[3]);
	} else {
	    broker.setOutTopic(DEFAULT_OUT_TOPIC);
	}
	broker.run();
    }

    public SimpleBroker() {
	System.out.println("Starting up SimpleBroker");
    }

    public void run() {
	// Prepare our context and subscriber
	ZMQ.Context context = ZMQ.context(1); // 1 = number of threads

	ZMQ.Socket subscriber = context.socket(ZMQ.SUB);
	String inBind = "tcp://*:" + inPort;
	System.out.println("subscribing to queue \"" + inTopic + "\" at " + inBind);
	subscriber.bind(inBind);
	// subscribe to everything
	subscriber.subscribe(new byte[0]); // was inTopic.getBytes()
	
	ZMQ.Socket publisher = context.socket(ZMQ.PUB);
	String outBind = "tcp://*:" + outPort;
	System.out.println("publishing to queue \"" + outTopic + "\" at " + outBind);
	publisher.bind(outBind);

	while (true) {
	    byte[] address;
	    byte[] message;
	    address = subscriber.recv(0);
	    message = subscriber.recv(0);
			if (HEARTBEAT_TOPIC.equals(address)) {
					publisher.send(HEARTBEAT_TOPIC.getBytes(), ZMQ.SNDMORE);
				} else {
					publisher.send(outTopic.getBytes(), ZMQ.SNDMORE);
				}
	    publisher.send(message, 0);
	    System.out.println("got address=" + new String(address) + " and message=" + new String(message) + ", resent to " + outTopic);
	} // while forever

    }

    public void setInPort(int inPort) {
	this.inPort = inPort;
    }

    public void setOutPort(int outPort) {
	this.outPort = outPort;
    }

    public void setInTopic(String inTopic) {
	this.inTopic = inTopic;
    }

   public void setOutTopic(String outTopic) {
	this.outTopic = outTopic;
    }
}
