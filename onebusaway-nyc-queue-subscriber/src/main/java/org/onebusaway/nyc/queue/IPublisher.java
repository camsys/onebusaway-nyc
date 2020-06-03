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

/**
 * Represents an interface to simply message queue publishing operations. This
 * is not attempting to be JMS. This is merely hiding the details of ZeroMQ for
 * easier testing.
 */
public interface IPublisher {

	public void open(String protocol, String host, int port);

	public void close();

	public void send(byte[] message);
}