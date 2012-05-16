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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.nyc.transit_data_federation.impl.nyc.RunServiceImpl;
import org.onebusaway.nyc.transit_data_federation.services.nyc.RunService;
import org.onebusaway.nyc.vehicle_tracking.impl.sort.BlockInstanceComparator;
import org.onebusaway.nyc.vehicle_tracking.impl.sort.ScheduledBlockLocationComparator;
import org.onebusaway.transit_data_federation.services.blocks.BlockCalendarService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.impl.blocks.FrequencyComparator;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.FrequencyEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import java.io.Serializable;

import javax.annotation.Nullable;

public final class BlockState implements Comparable<BlockState>, Serializable {

  private static final long serialVersionUID = 113570899391047971L;

  /**
   * These transient fields need to be repopulated, after deserialization,
   * using the accompanying info.
   */
  transient private RunTripEntry runTrip;
  private final String rteRunId;

  transient private BlockInstance blockInstance;
  private final long biServiceDate;
  private final AgencyAndId biBlockId; 

  transient private ScheduledBlockLocation blockLocation;
  private final int sblScheduleTime;

  private final String destinationSignCode;


  public BlockState(BlockInstance blockInstance,
      ScheduledBlockLocation blockLocation, @Nullable
      RunTripEntry runTrip, String destinationSignCode) {
    Preconditions.checkNotNull(blockInstance, "blockInstance");
    Preconditions.checkNotNull(blockLocation, "blockLocation");
    Preconditions.checkNotNull(destinationSignCode, "destinationSignCode");

    this.blockInstance = blockInstance;
    this.biBlockId = blockInstance.getBlock().getBlock().getId();
    this.biServiceDate = blockInstance.getState().getServiceDate();
    
    this.blockLocation = blockLocation;
    this.sblScheduleTime = blockLocation.getScheduledTime();
    
    this.destinationSignCode = destinationSignCode;
    
    this.runTrip = runTrip;
    if (runTrip != null) {
      this.rteRunId = runTrip.getRunId();
    } else {
      this.rteRunId = null;
    }
  }

  static public RunService runService;
  
  public RunTripEntry getRunTripEntry() {
    if (runTrip == null) {
      long thisTime = biServiceDate + 1000*sblScheduleTime;
      runTrip = runService.getActiveRunTripEntryForRunAndTime(
          new AgencyAndId("MTA_NYCT", rteRunId), thisTime);
    }
    return runTrip;
  }

  static public BlockCalendarService blockCalendarService;
  
  public BlockInstance getBlockInstance() {
    if (blockInstance == null) {
      blockInstance = blockCalendarService.getBlockInstance(biBlockId,
          biServiceDate);
    }
    return blockInstance;
  }

  static public ScheduledBlockLocationService blockLocationService;
  
  public ScheduledBlockLocation getBlockLocation() {
    if (blockLocation == null) {
      blockLocation = blockLocationService.getScheduledBlockLocationFromScheduledTime(
          getBlockInstance().getBlock(), sblScheduleTime);
    }
    return blockLocation;
  }

  public String getDestinationSignCode() {
    return destinationSignCode;
  }

  @Override
  public String toString() {
    // TODO FIXME use the local values 
    AgencyAndId tripId = getBlockLocation().getActiveTrip() != null ?
        blockLocation.getActiveTrip().getTrip().getId() : null;
    Boolean runTripMatches = (tripId != null && getRunTripEntry() != null) ? tripId.equals(
        getRunTripEntry().getTripEntry().getId())
            : null;
    int mins = blockLocation.getScheduledTime()/60;
    return Objects.toStringHelper("BlockState")
        .add("blockId", getBlockInstance().getBlock().getBlock().getId())
        .add("scheduledTime", Integer.toString(mins/60) 
            + ":" + Integer.toString(mins % 60) 
            + " (" + Integer.toString(getBlockLocation().getScheduledTime()) + ")")
        .add("distanceAlongBlock", getBlockLocation().getDistanceAlongBlock())
        .add("tripId", tripId)
        .add("dsc", destinationSignCode)
        .add("runTripMatchesActiveTrip", runTripMatches)
        .toString();
  }

  public String getRunId() {
    return rteRunId;
  }

  /**
   * This compareTo method is for definite ordering in CategoricalDist; such
   * ordering allows for reproducibility in testing.
   */
  @Override
  public int compareTo(BlockState rightBs) {

    // TODO FIXME use the local values 
    if (this == rightBs)
      return 0;

    final int res = ComparisonChain.start().compare(this.destinationSignCode,
        rightBs.destinationSignCode).compare(this.getBlockInstance(),
        rightBs.getBlockInstance(), Ordering.from(BlockInstanceComparator.INSTANCE)).compare(
        this.getBlockLocation(), rightBs.getBlockLocation(),
        Ordering.from(ScheduledBlockLocationComparator.INSTANCE)).compare(
        this.getRunTripEntry(), rightBs.getRunTripEntry(), Ordering.natural().nullsLast())
        .result();
    return res;
  }

  @Override
  public int hashCode() {

    // TODO FIXME use the local values 
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((getBlockInstance() == null) ? 0 : blockInstance.hashCode());
    /*
     * don't want/need to change OBA api, so do this...
     */
    result = prime * result
        + ((getBlockLocation() == null) ? 0 : blockLocation.getScheduledTime());
    result = prime
        * result
        + ((blockLocation == null) ? 0
            : ((blockLocation.getActiveTrip() == null) ? 0
                : blockLocation.getActiveTrip().getTrip().getId().hashCode()));

    result = prime * result
        + ((destinationSignCode == null) ? 0 : destinationSignCode.hashCode());
    result = prime * result + ((getRunTripEntry() == null) ? 0 : getRunTripEntry().hashCode());

    return result;
  }

  @Override
  public boolean equals(Object obj) {
    // TODO FIXME use the local values 
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof BlockState))
      return false;

    final BlockState other = (BlockState) obj;

    if (getBlockInstance() == null) {
      if (other.getBlockInstance() != null)
        return false;
    } else if (!blockInstance.equals(other.getBlockInstance()))
      return false;

    if (getBlockLocation() == null) {
      if (other.getBlockLocation()!= null)
        return false;
    } else {
      if (other.getBlockLocation() == null)
        return false;

      if (blockLocation.getScheduledTime() != other.getBlockLocation().getScheduledTime())
        return false;
      if (!blockLocation.getActiveTrip().getTrip().getId().equals(
          other.blockLocation.getActiveTrip().getTrip().getId()))
        return false;
    }

    if (destinationSignCode == null) {
      if (other.destinationSignCode != null)
        return false;
    } else if (!destinationSignCode.equals(other.destinationSignCode))
      return false;

    if (getRunTripEntry() == null) {
      if (other.getRunTripEntry() != null)
        return false;
    } else if (!runTrip.equals(other.getRunTripEntry()))
      return false;
    return true;
  }

}
