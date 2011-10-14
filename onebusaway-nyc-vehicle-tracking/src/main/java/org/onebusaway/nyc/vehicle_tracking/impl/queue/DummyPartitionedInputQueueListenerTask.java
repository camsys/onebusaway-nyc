package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import org.onebusaway.nyc.vehicle_tracking.services.queue.PartitionedInputQueueListener;

/**
 * No-op implementation for inference engine
 */
public class DummyPartitionedInputQueueListenerTask 
  implements PartitionedInputQueueListener {

  public String getDepotPartitionKey() {
    return null;
  }

  public void setDepotPartitionKey(String depotPartitionKey) {    

  }
  
  public void processMessage(String address, String contents) {
  
  }
  
}
