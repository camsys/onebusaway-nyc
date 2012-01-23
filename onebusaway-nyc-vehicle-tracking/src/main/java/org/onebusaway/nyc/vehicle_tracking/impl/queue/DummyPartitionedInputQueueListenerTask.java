package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import org.onebusaway.nyc.vehicle_tracking.services.queue.PartitionedInputQueueListener;

/**
 * No-op implementation for inference engine
 */
public class DummyPartitionedInputQueueListenerTask 
  implements PartitionedInputQueueListener {

  @Override
  public String getDepotPartitionKey() {
    return null;
  }

  @Override
  public void setDepotPartitionKey(String depotPartitionKey) {    

  }
  
  @Override
  public boolean processMessage(String address, String contents) {
    return true;
  }
  
}
