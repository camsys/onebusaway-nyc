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

package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import org.onebusaway.nyc.queue.DNSResolver;
import org.onebusaway.nyc.vehicle_tracking.services.queue.PartitionedInputQueueListener;

/**
 * A no-op implementation for simulations with the inference engine--
 * we want to receive no actual real time queue input in that case.
 * 
 */
public class DummyPartitionedInputQueueListenerTask extends InputQueueListenerTask
     implements PartitionedInputQueueListener {

  @Override
  public String getDepotPartitionKey() {
    return null;
  }

  protected DNSResolver _outputQueueResolver;

  @Override
  public void setDepotPartitionKey(String depotPartitionKey) {

  }

    @Override
    public void initializeQueue(String host, String queueName, Integer port) throws InterruptedException {

    }

    @Override
  public boolean processMessage(String address, byte[] buff) {
    return true;
  }

	public void startListenerThread() {
        if (_initialized == true) {
            _log.warn("Configuration service tried to reconfigure inference output queue service; this service is not reconfigurable once started.");
            return;
        }

        final String host = "localhost";
        final String queueName = "bhs_queue";
        final Integer port = 9092;
        //"localhost", "demo_java", 9092

        if (host == null || queueName == null || port == null) {
            _log.info("Inference output queue is not attached; output hostname was not available via configuration service.");
            return;
        }

        try {
            initializeQueue(host, queueName, port);
        } catch (Exception any) {
            System.out.println("exception = " + any + "");
            _outputQueueResolver.reset();
        }
  }

	public String getQueueHost() {
    return null;
  }

	public String getQueueName() {
    return null;
  }
	
	public String getQueueDisplayName() {
    return null;
  }

	public Integer getQueuePort() {
    return -1;
  }

    @Override
    public void startDNSCheckThread() {

    }


}
