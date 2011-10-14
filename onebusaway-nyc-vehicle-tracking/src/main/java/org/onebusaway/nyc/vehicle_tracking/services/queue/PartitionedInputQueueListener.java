package org.onebusaway.nyc.vehicle_tracking.services.queue;

public interface PartitionedInputQueueListener {

  public String getDepotPartitionKey();

  public void setDepotPartitionKey(String depotPartitionKey);
  
  public void processMessage(String address, String contents);
  
}
