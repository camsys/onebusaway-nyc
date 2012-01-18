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

import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.transit_data_federation.bundle.tasks.transit_graph.FrequencyComparator;
import org.onebusaway.transit_data_federation.impl.otp.TripSequence;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.FrequencyEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;


import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Longs;

import org.apache.commons.lang.StringUtils;

import java.util.Comparator;


public final class BlockState implements Comparable<BlockState> {

  /**
   * Our current block instance
   */
  private final RunTripEntry runTrip;

  private final BlockInstance blockInstance;

  private final ScheduledBlockLocation blockLocation;

  private final String destinationSignCode;

  private Boolean isOpAssigned;

  private Boolean isRunReported;

  private Boolean isRunReportedAssignedMismatch;

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
    b.append(", isOpAssigned=").append(isOpAssigned);
    b.append(", isRunReported=").append(isRunReported);
    b.append(", isRunReportedAssignedMismatch=").append(
        isRunReportedAssignedMismatch);
    b.append("})");

    return b.toString();
  }

  public String getRunId() {
    // TODO agencyId?
    return runTrip == null ? null : runTrip.getRunId();
  }

  public Boolean getRunReported() {
    return isRunReported;
  }

  public void setRunReported(Boolean isRunReported) {
    this.isRunReported = isRunReported;
  }

  public Boolean getOpAssigned() {
    return isOpAssigned;
  }

  public void setOpAssigned(Boolean isUTSassigned) {
    this.isOpAssigned = isUTSassigned;
  }

  public Boolean isRunReportedAssignedMismatch() {
    return isRunReportedAssignedMismatch;
  }

  public void setRunReportedAssignedMismatch(Boolean isRunReportedUTSMismatch) {
    this.isRunReportedAssignedMismatch = isRunReportedUTSMismatch;
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
    
    return ComparisonChain.start()
      .compare(this.getBlockLocation().getScheduledTime(), 
          rightBs.getBlockLocation().getScheduledTime())
      .compare(this.destinationSignCode, rightBs.getDestinationSignCode())
      .compare(this.isOpAssigned, rightBs.getOpAssigned(), 
          Ordering.natural().nullsLast())
      .compare(this.isRunReported, rightBs.getRunReported(),
          Ordering.natural().nullsLast())
      .compare(this.isRunReportedAssignedMismatch, rightBs.getRunReported(),
          Ordering.natural().nullsLast())
      .compare(this.getRunId(), rightBs.getRunId(), 
          Ordering.natural().nullsLast()) 
      .compare(this.getBlockLocation().getActiveTrip().getTrip(), 
          rightBs.getBlockLocation().getActiveTrip().getTrip(),
          Ordering.from(_tripIdComparator).nullsLast())
      .compare(this.getBlockLocation().getDistanceAlongBlock(), 
          rightBs.getBlockLocation().getDistanceAlongBlock())
      .compare(this.getBlockInstance(), rightBs.getBlockInstance(), 
          Ordering.from(_blockInstanceComparator))
      .result();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((blockInstance == null) ? 0 : blockInstance.hashCode());
    result = prime * result
        + ((blockLocation == null) ? 0 : blockLocation.hashCode());
    result = prime * result
        + ((destinationSignCode == null) ? 0 : destinationSignCode.hashCode());
    result = prime * result
        + ((isOpAssigned == null) ? 0 : isOpAssigned.hashCode());
    result = prime * result
        + ((isRunReported == null) ? 0 : isRunReported.hashCode());
    result = prime
        * result
        + ((isRunReportedAssignedMismatch == null) ? 0
            : isRunReportedAssignedMismatch.hashCode());
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
    } else if (!blockLocation.equals(other.blockLocation))
      return false;
    if (destinationSignCode == null) {
      if (other.destinationSignCode != null)
        return false;
    } else if (!destinationSignCode.equals(other.destinationSignCode))
      return false;
    if (isOpAssigned == null) {
      if (other.isOpAssigned != null)
        return false;
    } else if (!isOpAssigned.equals(other.isOpAssigned))
      return false;
    if (isRunReported == null) {
      if (other.isRunReported != null)
        return false;
    } else if (!isRunReported.equals(other.isRunReported))
      return false;
    if (isRunReportedAssignedMismatch == null) {
      if (other.isRunReportedAssignedMismatch != null)
        return false;
    } else if (!isRunReportedAssignedMismatch.equals(other.isRunReportedAssignedMismatch))
      return false;
    // TODO should verify that these objects aren't duplicated, or implement
    // equals & hashCode
    if (runTrip == null) {
      if (other.runTrip != null)
        return false;
    } else if (!runTrip.equals(other.runTrip))
      return false;
    return true;
  }
  
  

}
