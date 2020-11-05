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

package org.onebusaway.nyc.queue;

import com.google.transit.realtime.GtfsRealtime.*;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.zeromq.ZMQ;

public class TripUpdateSubscriber {
	private static final String HOST_KEY = "mq.host";
	private static final String PORT_KEY = "mq.port";
	private static final String TOPIC_KEY = "mq.topic";
	private static final String SINGLE_MODE_KEY = "tu.single";
	private static final String ROUTE_KEY = "tu.route";
	private static final String OUTPUT_DIR_KEY = "tu.output";
	private static final String DEFAULT_HOST = "queue.dev.obanyc.com";
	private static final int DEFAULT_PORT = 5569;
	private static final String DEFAULT_TOPIC = "time";
	private static final String DEFAULT_OUTPUT = ".";
	private static final String DEFAULT_SINGLE_MODE = "false";

	public static void main(String[] args) {

		String route = System.getProperty(ROUTE_KEY);
		String outputDir = System.getProperty(OUTPUT_DIR_KEY, DEFAULT_OUTPUT);
		boolean singleMode = Boolean.parseBoolean(System.getProperty(SINGLE_MODE_KEY, DEFAULT_SINGLE_MODE));

		System.out.println("listen for route " + route + ", writing to dir " + outputDir + ", singleMode=" + singleMode);

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
					TripUpdate tripUpdate = entity.getTripUpdate();
					AgencyAndId routeId = AgencyAndId.convertFromString(tripUpdate.getTrip().getRouteId());
					if (route == null || routeId.getId().equals(route)) {
						// name by trip
						AgencyAndId tripId = AgencyAndId.convertFromString(tripUpdate.getTrip().getTripId());
						long timestamp = message.getHeader().getTimestamp()/1000;
						String vehicle = AgencyAndId.convertFromString(tripUpdate.getVehicle().getId()).getId();
						String filename = outputDir + "/" + tripId.getId() + ".pb";
						System.out.println("timestamp=" + timestamp + " vehicleid=" + vehicle + " filename=" + filename);
						Subscriber.writeToFile(filename, contents);
						if (singleMode)
							break;
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
