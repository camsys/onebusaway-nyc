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

/**
 * Hibernate entity for archiving NycVehicleManagementStatusBeans coming from
 * the inference engine queue.
 * 
 * @author smeeks
 * 
 */
package org.onebusaway.nyc.report_archive.model;

import org.onebusaway.nyc.transit_data.model.NycVehicleManagementStatusBean;
import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.math.BigDecimal;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "obanyc_vehiclemanagementstatus_archive")
@AccessType("field")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class NycVehicleManagementStatusRecord implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  @AccessType("property")
  private Integer id;

  @Column(nullable = true, name = "active_bundle_id")
  private String activeBundleId;

  @Column(nullable = false, name = "last_update_time")
  private Long lastUpdateTime;

  @Column(nullable = false, name = "last_location_update_time")
  private Long lastLocationUpdateTime;

  @Column(nullable = false, columnDefinition = "DECIMAL(9,6)", name = "last_observed_latitude")
  private BigDecimal lastObservedLatitude;

  @Column(nullable = false, columnDefinition = "DECIMAL(9,6)", name = "last_observed_longitude")
  private BigDecimal lastObservedLongitude;

  @Column(nullable = false, name = "most_recent_observed_destination_sign_code")
  private String mostRecentObservedDestinationSignCode;

  @Column(nullable = true, name = "last_inferred_destination_sign_code")
  private String lastInferredDestinationSignCode;

  @Column(nullable = true, name = "inference_engine_hostname")
  private String inferenceEngineHostname;

  @Column(nullable = false, name = "inference_is_enabled")
  private boolean inferenceIsEnabled;

  @Column(nullable = false, name = "inference_engine_is_primary")
  private boolean inferenceEngineIsPrimary;

  @Column(nullable = false, name = "inference_is_formal")
  private boolean inferenceIsFormal;

  @Column(nullable = false, name = "emergency_flag")
  private boolean emergencyFlag;

  @Column(nullable = false, name = "depot_id")
  private String depotId;

  @Column(nullable = true, name = "last_inferred_operator_id")
  private String lastInferredOperatorId;

  @Column(nullable = true, name = "inferred_run_id")
  private String inferredRunId;

  public NycVehicleManagementStatusRecord() {
  }

  public NycVehicleManagementStatusRecord(NycVehicleManagementStatusBean message) {
    super();
    setActiveBundleId(message.getActiveBundleId());
    setLastUpdateTime(message.getLastUpdateTime());
    setLastLocationUpdateTime(message.getLastLocationUpdateTime());
    setLastObservedLatitude(new BigDecimal(message.getLastObservedLatitude()));
    setLastObservedLongitude(new BigDecimal(message.getLastObservedLongitude()));
    setMostRecentObservedDestinationSignCode(message.getMostRecentObservedDestinationSignCode());
    setLastInferredDestinationSignCode(message.getLastInferredDestinationSignCode());
    setInferenceIsEnabled(message.isInferenceIsEnabled());
    setInferenceEngineIsPrimary(message.isInferenceEngineIsPrimary());
    setInferenceIsFormal(message.isInferenceIsFormal());
    setDepotId(message.getDepotId());
    setEmergencyFlag(message.isEmergencyFlag());
    setLastInferredOperatorId(message.getLastInferredOperatorId());
    setInferredRunId(message.getInferredRunId());
  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public String getActiveBundleId() {
    return activeBundleId;
  }

  public void setActiveBundleId(String activeBundleId) {
    this.activeBundleId = activeBundleId;
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

  public BigDecimal getLastObservedLatitude() {
    return lastObservedLatitude;
  }

  public void setLastObservedLatitude(BigDecimal latitude) {
    this.lastObservedLatitude = latitude;
  }

  public BigDecimal getLastObservedLongitude() {
    return lastObservedLongitude;
  }

  public void setLastObservedLongitude(BigDecimal longitude) {
    this.lastObservedLongitude = longitude;
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

  public void setLastInferredDestinationSignCode(
      String inferredDestinationSignCode) {
    this.lastInferredDestinationSignCode = inferredDestinationSignCode;
  }

  public String getInferenceEngineHostname() {
    return inferenceEngineHostname;
  }

  public void setInferenceEngineHostname(String inferenceEngineHostname) {
    this.inferenceEngineHostname = inferenceEngineHostname;
  }

  public boolean isInferenceIsEnabled() {
    return inferenceIsEnabled;
  }

  public void setInferenceIsEnabled(boolean enabled) {
    this.inferenceIsEnabled = enabled;
  }

  public boolean isInferenceIsFormal() {
    return inferenceIsFormal;
  }

  public void setInferenceIsFormal(boolean inferenceIsFormal) {
    this.inferenceIsFormal = inferenceIsFormal;
  }

  public boolean isInferenceEngineIsPrimary() {
    return inferenceEngineIsPrimary;
  }

  public void setInferenceEngineIsPrimary(boolean inferenceEngineIsPrimary) {
    this.inferenceEngineIsPrimary = inferenceEngineIsPrimary;
  }

  public void setDepotId(String depotId) {
    this.depotId = depotId;
  }

  public String getDepotId() {
    return depotId;
  }

  public boolean isEmergencyFlag() {
    return emergencyFlag;
  }

  public void setEmergencyFlag(boolean emergencyFlag) {
    this.emergencyFlag = emergencyFlag;
  }

  public void setLastInferredOperatorId(String operatorId) {
    this.lastInferredOperatorId = operatorId;
  }

  public String getLastInferredOperatorId() {
    return lastInferredOperatorId;
  }

  public void setInferredRunId(String inferredRunId) {
    this.inferredRunId = inferredRunId;
  }

  public String getInferredRunId() {
    return inferredRunId;
  }

}
