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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.queue.model.RealtimeEnvelope;
import org.onebusaway.nyc.vehicle_tracking.services.inference.VehicleLocationInferenceService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.InputService;
import org.onebusaway.nyc.vehicle_tracking.services.queue.PartitionedInputQueueListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import tcip_final_3_0_5_1.CPTVehicleIden;
import tcip_final_3_0_5_1.CcLocationReport;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Listens to the real time queue for one vehicle and injects it into the inference process.
 * Configure the specific vehicle by editing vehicleId in SingleVehicleQueueInputServiceImpl.
 * 
 * @author jmaki
 */
public class SingleVehicleInputQueueListenerTask extends InputQueueListenerTask
    implements PartitionedInputQueueListener {

  @Autowired
  @Qualifier("singleVehicleInputService")
  @Override
  public void setInputService(InputService inputService){
	  _inputService = inputService;
  }

  @Override
  public String getQueueDisplayName() {
    return "SingleVehicleInputQueueListenerTask";
  }

  @Override
  public boolean processMessage(String address, byte[] buff) throws Exception {
    return _inputService.processMessage(address, buff);
  }

  @Override
  @PostConstruct
  public void setup() {
    super.setup();
  }

  @Override
  @PreDestroy
  public void destroy() {
    super.destroy();
  }

  @Override
  public String getDepotPartitionKey() {
    return null;
  }

  @Override
  public void setDepotPartitionKey(String depotPartitionKey) {
    // not partitioned by depot, nothing to do here.
  }

}
