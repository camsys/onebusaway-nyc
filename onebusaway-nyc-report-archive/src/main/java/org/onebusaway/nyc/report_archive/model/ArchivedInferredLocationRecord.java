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
@Table(name = "obanyc_inferredlocation")
@AccessType("field")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class ArchivedInferredLocationRecord implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  @AccessType("property")
  private Integer id;

  // Fields from NycQueuedInferredLocationBean
  @Column(nullable = false, name = "time_reported")
  private Date timeReported;

  @Column(nullable = false, name = "vehicle_id")
  @Index(name = "vehicle_id")
  private Integer vehicleId;

  @Column(nullable = false, name = "agency_id", length = 64)
  private String agencyId;

  @Column(nullable = false, name = "time_received")
  private Date timeReceived;

  @Column(nullable = false, name = "service_date")
  private Date serviceDate;
  
  @Column(nullable = true, name = "schedule_deviation")
  private Integer scheduleDeviation;
  
  @Column(nullable = true, name = "inferred_block_id")
  private String inferredBlockId;
  
  @Column(nullable = true, name = "inferred_trip_id")
  private String inferredTripId;
  
  @Column(nullable = true, name = "distance_along_block")
  private Double distanceAlongBlock;
  
  @Column(nullable = true, name = "distance_along_trip")
  private Double distanceAlongTrip;
  
  @Column(nullable = true, columnDefinition = "DECIMAL(9,6)", name = "inferred_latitude")
  private BigDecimal inferredLatitude;
  
  @Column(nullable = true, columnDefinition = "DECIMAL(9,6)", name = "inferred_longitude")
  private BigDecimal inferredLongitude;
  
  @Column(nullable = false, name = "inferred_phase")
  private String inferredPhase;
  
  @Column(nullable = false, name = "inferred_status")
  private String inferredStatus;
  
  @Column(nullable = true, name = "run_id")
  private String runId;

  @Column(nullable = true, name = "inferred_route_id")
  private String inferredRouteId;

  @Column(nullable = true, name = "inferred_direction_id")
  private String inferredDirectionId;
   

  // Fields from NycVehicleManagementStatusBean
  @Column(nullable = false, name = "last_update_time")
  private Long lastUpdateTime;
  
  @Column(nullable = false, name = "last_location_update_time")
  private Long lastLocationUpdateTime;

  @Column(nullable = true, name = "inferred_dest_sign_code")
  private String inferredDestSignCode;
  
  @Column(nullable = false, name = "inference_is_formal")
  private boolean inferenceIsFormal;

  @Column(nullable = false, name = "emergency_flag")
  private boolean emergencyFlag;

  @Column(nullable = false, name = "depot_id")
  private String depotId;

  @Column(nullable = true, name = "inferred_operator_id")
  private String inferredOperatorId;

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

    setTimeReported(new Date(message.getRecordTimestamp()));

    // Split vehicle id string to vehicle integer and agency designator string
    String id = message.getVehicleId();
    int index = id.indexOf('_');
    String agency = id.substring(0, index);
    int vehicleId = Integer.parseInt(id.substring(index + 1));

    setVehicleId(vehicleId);
    setAgencyId(agency);

    setTimeReceived(new Date());

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
    setInferredPhase(message.getPhase());
    setInferredStatus(message.getStatus());
    setRunId(message.getRunId());
    setInferredRouteId(message.getRouteId());
    // TODO: set inferredDirectionId

    NycVehicleManagementStatusBean managementBean = message.getManagementRecord();

    setLastUpdateTime(managementBean.getLastUpdateTime());
    setLastLocationUpdateTime(managementBean.getLastLocationUpdateTime());
    setInferredDestSignCode(managementBean.getLastInferredDestinationSignCode());
    setInferenceIsFormal(managementBean.isInferenceIsFormal());
    setDepotId(managementBean.getDepotId());
    setEmergencyFlag(managementBean.isEmergencyFlag());
    setInferredOperatorId(managementBean.getLastInferredOperatorId());
    setInferredRunId(managementBean.getInferredRunId());

    // TDS fields are inserted by listener task.

  }

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Date getTimeReported() {
    return timeReported;
  }

  public void setTimeReported(Date timeReported) {
    this.timeReported = timeReported;
  }

  public Integer getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(Integer vehicleId) {
    this.vehicleId = vehicleId;
  }

  public String getAgencyId() {
    return agencyId;
  }

  public void setAgencyId(String agencyId) {
    this.agencyId = agencyId;
  }

  public Date getTimeReceived() {
    return timeReceived;
  }

  public void setTimeReceived(Date timeReceived) {
    this.timeReceived = timeReceived;
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

  public String getInferredBlockId() {
    return inferredBlockId;
  }

  public void setInferredBlockId(String inferredBlockId) {
    this.inferredBlockId = inferredBlockId;
  }

  public String getInferredTripId() {
    return inferredTripId;
  }

  public void setInferredTripId(String inferredTripId) {
    this.inferredTripId = inferredTripId;
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

  public String getInferredPhase() {
    return inferredPhase;
  }

  public void setInferredPhase(String inferredPhase) {
    this.inferredPhase = inferredPhase;
  }

  public String getInferredStatus() {
    return inferredStatus;
  }

  public void setInferredStatus(String inferredStatus) {
    this.inferredStatus = inferredStatus;
  }

  public void setRunId(String runId) {
    this.runId = runId;
  }
  
  public String getRunId() {
    return runId;
  }

  public void setInferredRouteId(String inferredRouteId) {
    this.inferredRouteId = inferredRouteId;
  }
  
  public String getInferredRouteId() {
    return inferredRouteId;
  }

  public void setInferredDirectionId(String inferredDirectionId) {
    this.inferredDirectionId = inferredDirectionId;
  }
  
  public String getInferredDirectionId() {
    return inferredDirectionId;
  }

   // properties from NycVehicleManagementStatusBean
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

  public String getInferredDestSignCode() {
    return inferredDestSignCode;
  }

  public void setInferredDestSignCode(String inferredDestSignCode) {
    this.inferredDestSignCode = inferredDestSignCode;
  }

  public boolean isInferenceIsFormal() {
	return inferenceIsFormal;
  }

  public void setInferenceIsFormal(boolean inferenceIsFormal) {
	this.inferenceIsFormal = inferenceIsFormal;
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

  public void setInferredOperatorId(String operatorId) {
    this.inferredOperatorId = operatorId;
  }

  public String getInferredOperatorId() {
    return inferredOperatorId;
  }

  public void setInferredRunId(String inferredRunId) {
    this.inferredRunId = inferredRunId;
  }

  public String getInferredRunId() {
    return inferredRunId;
  }

  // Properties from TDS

  public void setNextScheduledStopId(String nextScheduledStopId) {
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
