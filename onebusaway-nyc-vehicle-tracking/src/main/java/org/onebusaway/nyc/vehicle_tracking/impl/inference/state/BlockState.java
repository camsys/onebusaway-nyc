/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.transit_data_federation.bundle.tasks.transit_graph.FrequencyComparator;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;


import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import java.util.Comparator;


public final class BlockState implements Comparable<BlockState> {

  
  /**
   * Our current block instance
   */
  private final RunTripEntry runTrip;

  private final BlockInstance blockInstance;

  private final ScheduledBlockLocation blockLocation;

  private final String destinationSignCode;
  
  public BlockState(BlockInstance blockInstance,
      ScheduledBlockLocation blockLocation, RunTripEntry runTrip,
      String destinationSignCode) {
    if (blockInstance == null)
      throw new IllegalArgumentException("blockInstance is null");
    if (blockLocation == null)
      throw new IllegalArgumentException("blockLocation is null");
    if (destinationSignCode == null)
      throw new IllegalArgumentException("dsc is null");
    // TODO should we every allow null runTrips here?
    // if (runTrip == null)
    // throw new IllegalArgumentException("runTrip is null");
    this.blockInstance = blockInstance;
    this.blockLocation = blockLocation;
    this.destinationSignCode = destinationSignCode;
    this.runTrip = runTrip;
  }

  public RunTripEntry getRunTripEntry() {
    return runTrip;
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
    StringBuilder b = new StringBuilder();
    b.append("BlockState(");
    b.append(blockInstance).append(",");
    b.append(blockLocation).append(",");
    b.append(runTrip).append(",");
    b.append(" {dsc=").append(destinationSignCode);
    b.append("})");

    return b.toString();
  }

  public String getRunId() {
    // TODO agencyId?
    return runTrip == null ? null : runTrip.getRunId();
  }

  
  static final private FrequencyComparator _frequencyComparator = new FrequencyComparator();
  
  private static class TripIdComparator implements Comparator<TripEntry> {
    @Override
    public int compare(TripEntry o1, TripEntry o2) {
      return o1.getId().compareTo(o2.getId());
    }
  }
  
  private static class BlockInstanceComparator implements Comparator<BlockInstance> {

    @Override
    public int compare(BlockInstance o1, BlockInstance o2) {
      if (o1 == o2)
        return 0;
      
      return ComparisonChain.start()
          .compare(o1.getBlock().getServiceIds(), o2.getBlock().getServiceIds())
          .compare(o1.getServiceDate(), o2.getServiceDate())
          .compare(o1.getFrequency(), o2.getFrequency(), 
            Ordering.from(_frequencyComparator).nullsLast())    
          .result();
    }
    
  }
  
  static final private TripIdComparator _tripIdComparator = new TripIdComparator(); 
  static final private BlockInstanceComparator _blockInstanceComparator = new BlockInstanceComparator();
  
  /**
   *  This compareTo method is for definite ordering
   *  in CategoricalDist; such ordering allows for
   *  reproducibility in testing. 
   */
  @Override
  public int compareTo(BlockState rightBs) {
    
    if (this == rightBs)
      return 0;
    
    int res = ComparisonChain.start()
      .compare(this.runTrip, rightBs.runTrip, 
          Ordering.natural().nullsLast()) 
      .compare(this.destinationSignCode, rightBs.getDestinationSignCode())
      .compare(this.getBlockLocation().getDistanceAlongBlock(), 
          rightBs.getBlockLocation().getDistanceAlongBlock())
      .compare(this.getBlockLocation().getActiveTrip().getTrip(), 
          rightBs.getBlockLocation().getActiveTrip().getTrip(),
          Ordering.from(_tripIdComparator).nullsLast())
      .compare(this.getBlockLocation().getScheduledTime(), 
          rightBs.getBlockLocation().getScheduledTime())
      .compare(this.getBlockInstance(), rightBs.getBlockInstance(), 
          Ordering.from(_blockInstanceComparator))
      .result();
    return res;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((blockInstance == null) ? 0 : blockInstance.hashCode());
    /*
     * don't want/need to change OBA api, so do this...
     */
    result = prime * result
        + ((blockLocation == null) ? 0 : blockLocation.getScheduledTime());
    result = prime * result
        + ((blockLocation == null) ? 0 : 
            ((blockLocation.getActiveTrip() == null) ? 0 : 
              blockLocation.getActiveTrip().getTrip().getId().hashCode()));
    long temp = 0;
    if (blockLocation != null)
      temp = Double.doubleToLongBits(blockLocation.getDistanceAlongBlock());
    result = (int) (prime * result + ((temp == 0) ? temp : (int) (temp ^ (temp >>> 32))));
    
    result = prime * result
        + ((destinationSignCode == null) ? 0 : destinationSignCode.hashCode());
    result = prime * result + ((runTrip == null) ? 0 : runTrip.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof BlockState))
      return false;
    
    BlockState other = (BlockState) obj;
    
    if (blockInstance == null) {
      if (other.blockInstance != null)
        return false;
    } else if (!blockInstance.equals(other.blockInstance))
      return false;
    
    if (blockLocation == null) {
      if (other.blockLocation != null)
        return false;
    } else {
      if (other.blockLocation == null)
        return false;
      
      if (blockLocation.getScheduledTime() != other.blockLocation.getScheduledTime())
        return false;
      if (!blockLocation.getActiveTrip().getTrip().getId().equals(
            other.blockLocation.getActiveTrip().getTrip().getId()))
        return false;
      if (Double.compare(blockLocation.getDistanceAlongBlock(), 
            other.blockLocation.getDistanceAlongBlock()) != 0)
        return false;
    }
    
    if (destinationSignCode == null) {
      if (other.destinationSignCode != null)
        return false;
    } else if (!destinationSignCode.equals(other.destinationSignCode))
      return false;
    
    if (runTrip == null) {
      if (other.runTrip != null)
        return false;
    } else if (!runTrip.equals(other.runTrip))
      return false;
    return true;
  }
  
  

}
