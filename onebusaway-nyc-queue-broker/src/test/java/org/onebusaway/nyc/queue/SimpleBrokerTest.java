package org.onebusaway.nyc.queue;

import org.zeromq.ZMQ;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class SimpleBrokerTest {

    @Test
    /*
     * to run this test, run simple broker in another window.
     * something like:
     * java -cp "jzmq-053c2d7.jar:onebusaway-nyc-queue-broker.jar" org.onebusaway.nyc.queue.SimpleBroker
     */
    public void testRun() throws Exception {
	final boolean SKIP = true;
	if (SKIP) return;

	// create content
	String content = "This is what i was expecting";

	// create our connections
	ZMQ.Context context = ZMQ.context(1);  // 1 = number of threads
	ZMQ.Socket publisher = context.socket(ZMQ.PUB); // publisher
	publisher.bind("tcp://*:5566");

	ZMQ.Socket subscriber = context.socket(ZMQ.SUB); // subscriber
	subscriber.connect("tcp://*:5567");
	subscriber.subscribe("output_queue".getBytes()); // listen to output queu
	
	ZMQ.Poller items = context.poller(1); // 1 = size (1 subscription)
	items.register(subscriber, ZMQ.Poller.POLLIN);

	// publish it to inference_queue
	publisher.send("inference_queue".getBytes(), ZMQ.SNDMORE); 
	publisher.send(content.getBytes(), 0);

	// listen for it on output_queue
	final int MAX_WAIT = 5;
	int i = 0;
	boolean found = false;
	while (!found && i < MAX_WAIT) {
	    byte[] message;
	    items.poll(1*1000*1000); // micro seconds
	    if (items.pollin(0)) { // 0 = index of subscription
		// verify its what we sent
		assertEquals("output_queue", new String(subscriber.recv(0)));
		assertEquals(content, new String(subscriber.recv(0)));
		found = true;
	    }

	    // send it again
	    publisher.send("inference_queue".getBytes(), ZMQ.SNDMORE);
	    publisher.send(content.getBytes(), 0);

	    i++;
	}
	assertTrue(i < MAX_WAIT);
    }
}