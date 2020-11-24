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

  @Override
  public void setDepotPartitionKey(String depotPartitionKey) {

  }

  @Override
  public boolean processMessage(String address, byte[] buff) {
    return true;
  }

	public void startListenerThread() {
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


}
