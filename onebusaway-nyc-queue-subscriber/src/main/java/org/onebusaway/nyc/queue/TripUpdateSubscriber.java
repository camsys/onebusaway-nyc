package org.onebusaway.nyc.queue;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.zeromq.ZMQ;

public class TripUpdateSubscriber {
	public static final String HOST_KEY = "mq.host";
	public static final String PORT_KEY = "mq.port";
	public static final String TOPIC_KEY = "mq.topic";
	private static final String DEFAULT_HOST = "queue.dev.obanyc.com";
	private static final int DEFAULT_PORT = 5569;
	private static final String DEFAULT_TOPIC = "time";

	public static void main(String[] args) {

		String route = args[0];
		String output = args[1];

		// Prepare our context and subscriber
		ZMQ.Context context = ZMQ.context(1);
		ZMQ.Socket subscriber = context.socket(ZMQ.SUB);

		String host = System.getProperty(HOST_KEY, DEFAULT_HOST);
		int port = defaultOrProperty(PORT_KEY, DEFAULT_PORT);
		String topic = System.getProperty(TOPIC_KEY, DEFAULT_TOPIC);

		String bind = "tcp://" + host + ":" + port;
		subscriber.connect(bind);
		subscriber.subscribe(topic.getBytes());
		System.out.println("TU subscriber listening on " + bind);

		while (true) {
			// Read envelope with address
			String address = new String(subscriber.recv(0));
			// Read message contents
			byte[] contents = subscriber.recv(0);
			try {
				FeedMessage message = FeedMessage.parseFrom(contents);
				FeedEntity entity = message.getEntity(0);
				if (entity.hasTripUpdate()) {
					AgencyAndId routeId = AgencyAndId.convertFromString(entity.getTripUpdate().getTrip().getRouteId());
					if (routeId.getId().equals(route)) {
						// name by trip
						AgencyAndId tripId = AgencyAndId.convertFromString(entity.getTripUpdate().getTrip().getTripId());
						String filename = output + "/" + tripId.getId() + ".pb";
						System.out.println("writing TU to " + filename);
						Subscriber.writeToFile(filename, contents);
					}
				}
				else {
					System.out.println("no trip update");
				}
			} catch(Exception e) {
				e.printStackTrace();
			}

		}
	}


	private static int defaultOrProperty(String prop, int def) {
		if (System.getProperty(prop) != null) {
			try {
				return Integer.parseInt(System.getProperty(prop));
			} catch (NumberFormatException nfe) {
				//
			}
		}
		return def;
	}



}
