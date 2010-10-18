package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;

public final class BlockState {

  /**
   * Our current block instance
   */
  private final BlockInstance blockInstance;

  private final ScheduledBlockLocation blockLocation;

  private final String destinationSignCode;

  public BlockState(BlockInstance blockInstance,
      ScheduledBlockLocation blockLocation, String destinationSignCode) {
    if (blockInstance == null)
      throw new IllegalArgumentException("blockInstance is null");
    if (blockLocation == null)
      throw new IllegalArgumentException("blockLocation is null");
    if (destinationSignCode == null)
      throw new IllegalArgumentException("dsc is null");
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

  @Override
  public String toString() {
    return blockInstance + " location=" + blockLocation + " dsc="
        + destinationSignCode;
  }
}
