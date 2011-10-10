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
package org.onebusaway.nyc.transit_data.model;

import java.io.Serializable;

/**
 * An over the wire management record that is sent by the inference engine and received 
 * by the TDF. These records are passed to the VehicleTrackingManagementService for processing.
 * 
 * @author jmaki
 *
 */
public class NycVehicleManagementStatusBean implements Serializable {

  private static final long serialVersionUID = 1L;

  private boolean inferenceIsEnabled;

  private Long lastUpdateTime;
	
  private Long lastLocationUpdateTime;

  private Double lastObservedLatitude;
	
  private Double lastObservedLongitude;
	
  private String mostRecentObservedDestinationSignCode;

  private String lastInferredDestinationSignCode;

  private String inferenceEngineHostname;
  
  private boolean inferenceEngineIsPrimary;
  
  private String activeBundleId;
  
  private boolean inferenceIsFormal;

  private String depotId;
  
  public boolean isInferenceIsEnabled() {
    return inferenceIsEnabled;
  }

  public void setInferenceIsEnabled(boolean enabled) {
    this.inferenceIsEnabled = enabled;
  }

  public long getLastUpdateTime() {
    return lastUpdateTime;
  }

  public void setLastUpdateTime(long lastUpdateTime) {
    this.lastUpdateTime = lastUpdateTime;
  }

  public long getLastLocationUpdateTime() {
    return lastLocationUpdateTime;
  }

  public void setLastLocationUpdateTime(long lastGpsTime) {
    this.lastLocationUpdateTime = lastGpsTime;
  }

  public double getLastObservedLatitude() {
    return lastObservedLatitude;
  }

  public void setLastObservedLatitude(double lastGpsLat) {
    this.lastObservedLatitude = lastGpsLat;
  }

  public double getLastObservedLongitude() {
    return lastObservedLongitude;
  }

  public void setLastObservedLongitude(double lastGpsLon) {
    this.lastObservedLongitude = lastGpsLon;
  }

  public String getMostRecentObservedDestinationSignCode() {
    return mostRecentObservedDestinationSignCode;
  }

  public void setMostRecentObservedDestinationSignCode(
      String mostRecentDestinationSignCode) {
    this.mostRecentObservedDestinationSignCode = mostRecentDestinationSignCode;
  }

  public String getLastInferredDestinationSignCode() {
    return lastInferredDestinationSignCode;
  }

  public void setLastInferredDestinationSignCode(String inferredDestinationSignCode) {
    this.lastInferredDestinationSignCode = inferredDestinationSignCode;
  }


  public boolean isInferenceIsFormal() {
	return inferenceIsFormal;
  }

  
  public void setInferenceIsFormal(boolean inferenceIsFormal) {
	this.inferenceIsFormal = inferenceIsFormal;
  }

  public String getInferenceEngineHostname() {
    return inferenceEngineHostname;
  }

  public void setInferenceEngineHostname(String inferenceEngineHostname) {
    this.inferenceEngineHostname = inferenceEngineHostname;
  }

  public boolean isInferenceEngineIsPrimary() {
    return inferenceEngineIsPrimary;
  }

  public void setInferenceEngineIsPrimary(boolean inferenceEngineIsPrimary) {
    this.inferenceEngineIsPrimary = inferenceEngineIsPrimary;
  }

  public String getActiveBundleId() {
    return activeBundleId;
  }

  public void setActiveBundleId(String activeBundleId) {
    this.activeBundleId = activeBundleId;
  }

  public void setDepotId(String depotId) {
    this.depotId = depotId;
  }

  public String getDepotId() {
    return depotId;
  }
}
