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
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;

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

  /**
   *  This compareTo method is for definite ordering
   *  in CategoricalDist; such ordering allows for
   *  reproducibility in testing. 
   */
  @Override
  public int compareTo(BlockState rightBs) {
    
    if (this == rightBs)
      return 0;
    
    int distAlongComp = Double.compare(this.getBlockLocation().getDistanceAlongBlock(),
        rightBs.getBlockLocation().getDistanceAlongBlock());
    
    if (distAlongComp != 0)
      return distAlongComp;
    
    int blockInstComp = this.getBlockInstance().getBlock().getBlock().getId().compareTo(
        rightBs.getBlockInstance().getBlock().getBlock().getId());
    
    if (blockInstComp != 0)
      return blockInstComp;
    
    int tripIdComp = this.getBlockLocation().getActiveTrip().getTrip().getId().compareTo(
        rightBs.getBlockLocation().getActiveTrip().getTrip().getId());
    
    if (tripIdComp != 0)
      return tripIdComp;
    
    return 0;
  }

}
