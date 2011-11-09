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
 * Hibernate entity for archiving most recent NycQueuedInferredLocationBeans, 
 * NycVehicleManagementStatusBeans, and TDS data coming
 * from the inference engine queue.
 *
 * @author smeeks
 *
 */
package org.onebusaway.nyc.report_archive.model;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

import org.onebusaway.nyc.transit_data.model.NycQueuedInferredLocationBean;
import org.onebusaway.nyc.transit_data.model.NycVehicleManagementStatusBean;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "obanyc_inferredlocation_archive")
@AccessType("field")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class ArchivedInferredLocationRecord implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  @AccessType("property")
  private Integer id;

  // Fields from NycQueuedInferredLocationBean
  @Column(nullable = false, name = "record_timestamp")
  private Date recordTimestamp;

  @Column(nullable = false, name = "vehicle_id")
  private String vehicleId;

  @Column(nullable = false, name = "service_date")
  private Date serviceDate;
  
  @Column(nullable = true, name = "schedule_deviation")
  private Integer scheduleDeviation;
  
  @Column(nullable = true, name = "block_id")
  private String blockId;
  
  @Column(nullable = true, name = "trip_id")
  private String tripId;
  
  @Column(nullable = true, name = "distance_along_block")
  private Double distanceAlongBlock;
  
  @Column(nullable = true, name = "distance_along_trip")
  private Double distanceAlongTrip;
  
  @Column(nullable = true, columnDefinition = "DECIMAL(9,6)", name = "inferred_latitude")
  private BigDecimal inferredLatitude;
  
  @Column(nullable = true, columnDefinition = "DECIMAL(9,6)", name = "inferred_longitude")
  private BigDecimal inferredLongitude;
  
  @Column(nullable = false, columnDefinition = "DECIMAL(9,6)", name = "observed_latitude")
  private BigDecimal observedLatitude;
  
  @Column(nullable = false, columnDefinition = "DECIMAL(9,6)", name = "observed_longitude")
  private BigDecimal observedLongitude;
  
  @Column(nullable = false, name = "phase")
  private String phase;
  
  @Column(nullable = false, name = "status")
  private String status;
  
  @Column(nullable = false, name = "raw_message", length = 1400)
  private String rawMessage;

  @Column(nullable = true, name = "run_id")
  private String runId;

  @Column(nullable = true, name = "route_id")
  private String routeId;

  @Column(nullable = false, name = "bearing")
  private Double bearing;

  // Fields from NycVehicleManagementStatusBean
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
 
  // Fields from TDS
  // Stop ID of next scheduled stop
  @Column(nullable = true, name = "next_scheduled_stop_id")
  private String nextScheduledStopId;

  // Distance to next scheduled stop
  @Column(nullable = true, name = "next_scheduled_stop_distance")
  private Double nextScheduledStopDistance;

  public ArchivedInferredLocationRecord() {
  }

  public ArchivedInferredLocationRecord(NycQueuedInferredLocationBean message, String contents) {
    super();

    Double possibleNaN;

    setRecordTimestamp(new Date(message.getRecordTimestamp()));
    setVehicleId(message.getVehicleId());
    setServiceDate(new Date(message.getServiceDate()));
    setScheduleDeviation(message.getScheduleDeviation());

    possibleNaN = message.getDistanceAlongBlock();
    if (Double.isNaN(possibleNaN))
      setDistanceAlongBlock(null);
    else
      setDistanceAlongBlock(possibleNaN);
    
    setDistanceAlongTrip(message.getDistanceAlongTrip());

    if (Double.isNaN(message.getInferredLatitude()))
      setInferredLatitude(null);
    else
      setInferredLatitude(new BigDecimal(message.getInferredLatitude()));
    if (Double.isNaN(message.getInferredLongitude()))
      setInferredLongitude(null);
    else
      setInferredLongitude(new BigDecimal(message.getInferredLongitude()));
    setObservedLatitude(new BigDecimal(message.getObservedLatitude()));
    setObservedLongitude(new BigDecimal(message.getObservedLongitude()));
    setPhase(message.getPhase());
    setStatus(message.getStatus());
    setRunId(message.getRunId());
    setRouteId(message.getRouteId());
    setBearing(message.getBearing());
    setRawMessage(contents);

    NycVehicleManagementStatusBean managementBean = message.getManagementRecord();

    setActiveBundleId(managementBean.getActiveBundleId());
    setLastUpdateTime(managementBean.getLastUpdateTime());
    setLastLocationUpdateTime(managementBean.getLastLocationUpdateTime());
    setLastObservedLatitude(new BigDecimal(managementBean.getLastObservedLatitude()));
    setLastObservedLongitude(new BigDecimal(managementBean.getLastObservedLongitude()));
    setMostRecentObservedDestinationSignCode(managementBean.getMostRecentObservedDestinationSignCode());
    setLastInferredDestinationSignCode(managementBean.getLastInferredDestinationSignCode());
    setInferenceEngineHostname(managementBean.getInferenceEngineHostname());
    setInferenceIsEnabled(managementBean.isInferenceIsEnabled());
    setInferenceEngineIsPrimary(managementBean.isInferenceEngineIsPrimary());
    setInferenceIsFormal(managementBean.isInferenceIsFormal());
    setDepotId(managementBean.getDepotId());
    setEmergencyFlag(managementBean.isEmergencyFlag());
    setLastInferredOperatorId(managementBean.getLastInferredOperatorId());
    setInferredRunId(managementBean.getInferredRunId());

    // TODO: Query to get fields from TDS.

  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getRecordTimestamp() {
    return recordTimestamp;
  }

  public void setRecordTimestamp(Date recordTimestamp) {
    this.recordTimestamp = recordTimestamp;
  }

  public String getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(String vehicleId) {
    this.vehicleId = vehicleId;
  }

  public Date getServiceDate() {
    return serviceDate;
  }

  public void setServiceDate(Date serviceDate) {
    this.serviceDate = serviceDate;
  }

  public Integer getScheduleDeviation() {
    return scheduleDeviation;
  }

  public void setScheduleDeviation(Integer scheduleDeviation) {
    this.scheduleDeviation = scheduleDeviation;
  }

  public String getBlockId() {
    return blockId;
  }

  public void setBlockId(String blockId) {
    this.blockId = blockId;
  }

  public String getTripId() {
    return tripId;
  }

  public void setTripId(String tripId) {
    this.tripId = tripId;
  }

  public Double getDistanceAlongBlock() {
    return distanceAlongBlock;
  }

  public void setDistanceAlongBlock(Double distance) {
    this.distanceAlongBlock = distance;
  }

  public Double getDistanceAlongTrip() {
    return distanceAlongTrip;
  }

  public void setDistanceAlongTrip(Double distance) {
    this.distanceAlongTrip = distance;
  }

  public BigDecimal getInferredLatitude() {
    return inferredLatitude;
  }

  public void setInferredLatitude(BigDecimal latitude) {
    this.inferredLatitude = latitude;
  }

  public BigDecimal getInferredLongitude() {
    return inferredLongitude;
  }

  public void setInferredLongitude(BigDecimal longitude) {
    this.inferredLongitude = longitude;
  }

  public BigDecimal getObservedLatitude() {
    return observedLatitude;
  }

  public void setObservedLatitude(BigDecimal latitude) {
    this.observedLatitude = latitude;
  }

  public BigDecimal getObservedLongitude() {
    return observedLongitude;
  }

  public void setObservedLongitude(BigDecimal longitude) {
    this.observedLongitude = longitude;
  }

  public String getPhase() {
    return phase;
  }

  public void setPhase(String phase) {
    this.phase = phase;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public void setRunId(String runId) {
    this.runId = runId;
  }
  
  public String getRunId() {
    return runId;
  }

  public void setRouteId(String routeId) {
    this.routeId = routeId;
  }
  
  public String getRouteId() {
    return routeId;
  }

  public void setBearing(double bearing) {
    this.bearing = bearing;
  }
  
  public double getBearing() {
    return bearing;
  }

  public String getRawMessage() {
    return rawMessage;
  }

  public void setRawMessage(String rawMessage) {
    this.rawMessage = rawMessage;
  }

   // properties from NycVehicleManagementStatusBean
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

  public void setLastInferredDestinationSignCode(String inferredDestinationSignCode) {
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

  // Properties from TDS

  public void setNextScheduledStopId(String nextScheduleStopId) {
      this.nextScheduledStopId = nextScheduledStopId;
   }

  public String getNextScheduledStopId() {
      return nextScheduledStopId;
   }

  public void setNextScheduledStopDistance(Double distance) {
      this.nextScheduledStopDistance = distance;
  }

  public Double getNextScheduledStopDistance() {
      return nextScheduledStopDistance;
  }

}
