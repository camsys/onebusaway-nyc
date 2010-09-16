package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.transit_data_federation.services.realtime.BlockInstance;
import org.onebusaway.transit_data_federation.services.realtime.ScheduledBlockLocation;

public final class BlockState {

  /**
   * Our current block instance
   */
  private final BlockInstance blockInstance;
  
  private final ScheduledBlockLocation blockLocation;
  
  private final String destinationSignCode;

  public BlockState(BlockInstance blockInstance, ScheduledBlockLocation blockLocation, String destinationSignCode) {
    this.blockInstance = blockInstance;
    this.blockLocation = blockLocation;
    this.destinationSignCode = destinationSignCode;
  }

  public BlockInstance getBlockInstance() {
    return blockInstance;
  }

  public ScheduledBlockLocation getBlockLocation() {
    return blockLocation;
  }
  
  public String getDestinationSignCode() {
    return destinationSignCode;
  }
}
