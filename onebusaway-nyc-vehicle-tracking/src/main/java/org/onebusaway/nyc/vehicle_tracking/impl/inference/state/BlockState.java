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

public final class BlockState {

  /**
   * Our current block instance
   */
  private final RunTripEntry runTrip;
  
  private final BlockInstance blockInstance;

  private final ScheduledBlockLocation blockLocation;

  private final String destinationSignCode;

  private boolean isUTSassigned;

  private boolean isRunReported;
  
  private boolean isRunReportedUTSMismatch;

  public BlockState(BlockInstance blockInstance,
      ScheduledBlockLocation blockLocation, RunTripEntry runTrip, String destinationSignCode) {
    if (blockInstance == null)
      throw new IllegalArgumentException("blockInstance is null");
    if (blockLocation == null)
      throw new IllegalArgumentException("blockLocation is null");
    if (destinationSignCode == null)
      throw new IllegalArgumentException("dsc is null");
    // TODO should we every allow null runTrips here?
//    if (runTrip == null)
//      throw new IllegalArgumentException("runTrip is null");
    this.blockInstance = blockInstance;
    this.blockLocation = blockLocation;
    this.destinationSignCode = destinationSignCode;
    this.isUTSassigned = false;
    this.isRunReported = false;
    this.isRunReportedUTSMismatch = false;
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
    return "[" + blockInstance + ", location=" + blockLocation + ", dsc="
        + destinationSignCode + ", runTrip=" + runTrip 
        + ", isUTSassigned=" + isUTSassigned + ", isRunReported=" 
        + isRunReported + ", isRunReportedUTSMismatch=" + isRunReportedUTSMismatch
        + "]";
  }

  public String getRunId() {
    // TODO agencyId?
    return runTrip == null ? null:runTrip.getRun();
  }

  public boolean isRunReported() {
    return isRunReported;
  }

  public void setRunReported(boolean isRunReported) {
    this.isRunReported = isRunReported;
  }

  public boolean isUTSassigned() {
    return isUTSassigned;
  }

  public void setUTSassigned(boolean isUTSassigned) {
    this.isUTSassigned = isUTSassigned;
  }

  public boolean isRunReportedUTSMismatch() {
    return isRunReportedUTSMismatch;
  }

  public void setRunReportedUTSMismatch(boolean isRunReportedUTSMismatch) {
    this.isRunReportedUTSMismatch = isRunReportedUTSMismatch;
  }
}
