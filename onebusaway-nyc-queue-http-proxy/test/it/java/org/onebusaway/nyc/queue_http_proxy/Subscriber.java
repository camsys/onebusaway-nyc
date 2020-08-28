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