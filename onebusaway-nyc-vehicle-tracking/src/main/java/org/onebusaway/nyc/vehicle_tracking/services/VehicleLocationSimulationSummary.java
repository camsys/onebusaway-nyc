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
package org.onebusaway.nyc.vehicle_tracking.services;

import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;

public class VehicleLocationSimulationSummary {

  private int id;

  private String vehicleId;

  private int numberOfRecordsProcessed;

  private int numberOfRecordsTotal;

  private NycTestLocationRecord mostRecentRecord;

  private boolean paused = false;

  /**
   * True when the particle filter has had a failure
   */
  private boolean particleFilterFailure;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public int getNumberOfRecordsProcessed() {
    return numberOfRecordsProcessed;
  }

  public void setNumberOfRecordsProcessed(int numberOfRecordsProcessed) {
    this.numberOfRecordsProcessed = numberOfRecordsProcessed;
  }

  public int getNumberOfRecordsTotal() {
    return numberOfRecordsTotal;
  }

  public void setNumberOfRecordsTotal(int numberOfRecordsTotal) {
    this.numberOfRecordsTotal = numberOfRecordsTotal;
  }

  public NycTestLocationRecord getMostRecentRecord() {
    return mostRecentRecord;
  }

  public void setMostRecentRecord(NycTestLocationRecord mostRecentRecord) {
    this.mostRecentRecord = mostRecentRecord;
  }

  public boolean isPaused() {
    return paused;
  }

  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  public boolean isParticleFilterFailure() {
    return particleFilterFailure;
  }

  public void setParticleFilterFailure(boolean particleFilterFailure) {
    this.particleFilterFailure = particleFilterFailure;
  }
}
