package org.onebusaway.nyc.vehicle_tracking.services.queue;

public interface PartitionedInputQueueListener {

  public String getDepotPartitionKey();

  public void setDepotPartitionKey(String depotPartitionKey);

  /**
   * Attempt to process a TCIP JSON message.
   * 
   * @param address
   * @param contents
   * @return whether the message was processed successfully or not.
   */
  public boolean processMessage(String address, byte[] buff) throws Exception;

}
