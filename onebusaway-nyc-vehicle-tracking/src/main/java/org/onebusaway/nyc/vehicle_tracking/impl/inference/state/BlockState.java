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
import org.onebusaway.nyc.vehicle_tracking.impl.sort.BlockInstanceComparator;
import org.onebusaway.nyc.vehicle_tracking.impl.sort.ScheduledBlockLocationComparator;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import javax.annotation.Nullable;

public final class BlockState implements Comparable<BlockState> {

  /**
   * Our current block instance
   */
  private final RunTripEntry runTrip;

  private final BlockInstance blockInstance;

  private final ScheduledBlockLocation blockLocation;

  private final String destinationSignCode;

  public BlockState(BlockInstance blockInstance,
      ScheduledBlockLocation blockLocation, @Nullable
      RunTripEntry runTrip, String destinationSignCode) {
    Preconditions.checkNotNull(blockInstance, "blockInstance");
    Preconditions.checkNotNull(blockLocation, "blockLocation");
    Preconditions.checkNotNull(destinationSignCode, "destinationSignCode");

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
    AgencyAndId tripId = blockLocation.getActiveTrip() != null ?
        blockLocation.getActiveTrip().getTrip().getId() : null;;
    Boolean runTripMatches = (tripId != null && runTrip != null) ? tripId.equals(runTrip.getTripEntry().getId())
            : null;
    return Objects.toStringHelper("BlockState")
        .add("blockId", blockInstance.getBlock().getBlock().getId())
        .add("scheduledTime", blockLocation.getScheduledTime())
        .add("distanceAlongBlock", blockLocation.getDistanceAlongBlock())
        .add("tripId", tripId)
        .add("dsc", destinationSignCode)
        .add("runTripMatchesActiveTrip", runTripMatches)
        .toString();
  }

  public String getRunId() {
    // TODO agencyId?
    return runTrip == null ? null : runTrip.getRunId();
  }

  /**
   * This compareTo method is for definite ordering in CategoricalDist; such
   * ordering allows for reproducibility in testing.
   */
  @Override
  public int compareTo(BlockState rightBs) {

    if (this == rightBs)
      return 0;

    final int res = ComparisonChain.start().compare(this.destinationSignCode,
        rightBs.destinationSignCode).compare(this.blockInstance,
        rightBs.blockInstance, Ordering.from(BlockInstanceComparator.INSTANCE)).compare(
        this.blockLocation, rightBs.blockLocation,
        Ordering.from(ScheduledBlockLocationComparator.INSTANCE)).compare(
        this.runTrip, rightBs.runTrip, Ordering.natural().nullsLast()).result();
    return res;
  }

  private int _hash = 0;

  @Override
  public int hashCode() {
    if (_hash != 0)
      return _hash;

    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((blockInstance == null) ? 0 : blockInstance.hashCode());
    /*
     * don't want/need to change OBA api, so do this...
     */
    result = prime * result
        + ((blockLocation == null) ? 0 : blockLocation.getScheduledTime());
    result = prime
        * result
        + ((blockLocation == null) ? 0
            : ((blockLocation.getActiveTrip() == null) ? 0
                : blockLocation.getActiveTrip().getTrip().getId().hashCode()));
    // long temp = 0;
    // if (blockLocation != null)
    // temp = Double.doubleToLongBits(blockLocation.getDistanceAlongBlock());
    // result = (int) (prime * result + ((temp == 0) ? temp
    // : (int) (temp ^ (temp >>> 32))));

    result = prime * result
        + ((destinationSignCode == null) ? 0 : destinationSignCode.hashCode());
    result = prime * result + ((runTrip == null) ? 0 : runTrip.hashCode());

    _hash = result;
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

    final BlockState other = (BlockState) obj;

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
      // if (Double.compare(blockLocation.getDistanceAlongBlock(),
      // other.blockLocation.getDistanceAlongBlock()) != 0)
      // return false;
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
