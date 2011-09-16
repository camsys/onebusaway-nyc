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
package org.onebusaway.nyc.transit_data_federation.model;

import org.onebusaway.gtfs.model.AgencyAndId;

public class VehicleLocationManagementRecord {

  private AgencyAndId vehicleId;

  private boolean enabled;

  private long lastUpdateTime;

  private long lastGpsUpdateTime;

  private double lastGpsLat = Double.NaN;

  private double lastGpsLon = Double.NaN;

  private String mostRecentObservedDestinationSignCode;

  private String inferredDestinationSignCode;

  public AgencyAndId getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(AgencyAndId vehicleId) {
    this.vehicleId = vehicleId;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public long getLastUpdateTime() {
    return lastUpdateTime;
  }

  public void setLastUpdateTime(long lastUpdateTime) {
    this.lastUpdateTime = lastUpdateTime;
  }

  public long getLastGpsUpdateTime() {
    return lastGpsUpdateTime;
  }

  public void setLastGpsUpdateTime(long lastGpsTime) {
    this.lastGpsUpdateTime = lastGpsTime;
  }

  public double getLastGpsLat() {
    return lastGpsLat;
  }

  public void setLastGpsLat(double lastGpsLat) {
    this.lastGpsLat = lastGpsLat;
  }

  public double getLastGpsLon() {
    return lastGpsLon;
  }

  public void setLastGpsLon(double lastGpsLon) {
    this.lastGpsLon = lastGpsLon;
  }

  public String getMostRecentObservedDestinationSignCode() {
    return mostRecentObservedDestinationSignCode;
  }

  public void setMostRecentObservedDestinationSignCode(
      String mostRecentObservedDestinationSignCode) {
    this.mostRecentObservedDestinationSignCode = mostRecentObservedDestinationSignCode;
  }

  public String getInferredDestinationSignCode() {
    return inferredDestinationSignCode;
  }

  public void setInferredDestinationSignCode(String inferredDestinationSignCode) {
    this.inferredDestinationSignCode = inferredDestinationSignCode;
  }
}
