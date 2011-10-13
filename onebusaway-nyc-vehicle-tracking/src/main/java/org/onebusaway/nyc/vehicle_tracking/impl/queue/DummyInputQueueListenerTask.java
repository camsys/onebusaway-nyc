package org.onebusaway.nyc.vehicle_tracking.impl.queue;

import java.util.ArrayList;


/**
 * No-op implementation for inference engine
 */
public class DummyInputQueueListenerTask {

  private String[] _depotPartitionKeys = null;

  public void processMessage(String address, String contents) {
      // no op
  }

  
}
