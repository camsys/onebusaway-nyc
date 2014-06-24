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
 * Model to wrap CcLocationReportRecord and ArchivedInferredLocationRecord to
 * present as a unified object via json.
 * 
 * @author sheldonabrown
 * 
 */
package org.onebusaway.nyc.report.model;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "obanyc_last_known_vehicle")
@AccessType("field")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class CcAndInferredLocationRecord implements Serializable {

  private static final long serialVersionUID = 1L;

  public CcAndInferredLocationRecord() {
  }

  public CcAndInferredLocationRecord(ArchivedInferredLocationRecord inferred,
      CcLocationReportRecord realtime) {
    // realtime fields
    setUUID(realtime.getUUID());
    setRunIdDesignator(realtime.getRunIdDesignator());
    setRouteIdDesignator(realtime.getRouteIdDesignator());
    setVehicleAgencyId(realtime.getVehicleAgencyId());
    setTimeReported(toISODate(realtime.getTimeReported()));
    setTimeReceived(toISODate(realtime.getTimeReceived()));
    setArchiveTimeReceived(toISODate(realtime.getArchiveTimeReceived()));
    setOperatorIdDesignator(realtime.getOperatorIdDesignator());
    setDestSignCode(realtime.getDestSignCode());
    setEmergencyCode(realtime.getEmergencyCode());
    setLatitude(realtime.getLatitude());
    setLongitude(realtime.getLongitude());
    setSpeed(realtime.getSpeed());
    setDirectionDeg(realtime.getDirectionDeg());
    
    // inferred fields
    setAgencyId(inferred.getAgencyId());
    setVehicleId(inferred.getVehicleId());
    setDepotId(inferred.getDepotId());
    setServiceDate(toSimpleDate(inferred.getServiceDate()));
    setInferredOperatorId(inferred.getInferredOperatorId());
    setInferredRunId(inferred.getInferredRunId());
    setInferredBlockId(inferred.getInferredBlockId());
    setInferredTripId(inferred.getInferredTripId());
    setInferredRouteId(inferred.getInferredRouteId());
    setInferredDirectionId(inferred.getInferredDirectionId());
    setInferredDestSignCode(inferred.getInferredDestSignCode());
    setInferredLatitude(inferred.getInferredLatitude());
    setInferredLongitude(inferred.getInferredLongitude());
    setInferredPhase(inferred.getInferredPhase());
    setInferredStatus(inferred.getInferredStatus());
    setInferenceIsFormal(inferred.isInferenceIsFormal());
    setDistanceAlongBlock(inferred.getDistanceAlongBlock());
    setDistanceAlongTrip(inferred.getDistanceAlongTrip());
    setNextScheduledStopId(inferred.getNextScheduledStopId());
    setNextScheduledStopDistance(inferred.getNextScheduledStopDistance());
    setScheduleDeviation(inferred.getScheduleDeviation());
    setAssignedRunId(inferred.getAssignedRunId());

  }

  // realtime fields
  @Index(name = "UUID")
  @Column(nullable = false, name = "UUID", length = 36)
  private String uuid;

  @Column(nullable = false, name = "vehicle_agency_id")
  private Integer vehicleAgencyId;

  @Column(nullable = false, name = "time_reported")
  @Index(name = "time_reported")
  private String timeReported;

  // this is the system time received -- when the queue first saw it
  @Column(nullable = false, name = "time_received")
  @Index(name = "time_received")
  private String timeReceived;

  @Column(nullable = false, name = "archive_time_received")
  @Index(name = "archive_time_received")
  private String archiveTimeReceived;

  @Column(nullable = false, name = "operator_id_designator", length = 16)
  private String operatorIdDesignator;

  @Column(nullable = false, name = "route_id_designator", length = 16)
  private String routeIdDesignator;

  @Column(nullable = false, name = "run_id_designator", length = 32)
  private String runIdDesignator;

  @Column(nullable = false, name = "dest_sign_code")
  private Integer destSignCode;

  @Column(name = "emergency_code", length = 1)
  private String emergencyCode;

  @Column(nullable = false, columnDefinition = "DECIMAL(9,6)", name = "latitude")
  private BigDecimal latitude;

  @Column(nullable = false, columnDefinition = "DECIMAL(9,6)", name = "longitude")
  private BigDecimal longitude;

  @Column(nullable = false, columnDefinition = "DECIMAL(4,1)", name = "speed")
  private BigDecimal speed;

  @Column(nullable = false, columnDefinition = "DECIMAL(5,2)", name = "direction_deg")
  private BigDecimal directionDeg;

  // inferred fields

  @Column(nullable = false, name = "agency_id", length = 64)
  private String agencyId;

  @Id
  @Column(nullable = false, name = "vehicle_id")
  private Integer vehicleId;

  @Column(nullable = true, name = "depot_id", length = 16)
  private String depotId;

  @Column(nullable = false, name = "service_date")
  private String serviceDate;

  @Column(nullable = true, name = "inferred_operator_id", length = 16)
  private String inferredOperatorId;

  @Column(nullable = true, name = "inferred_run_id", length = 16)
  private String inferredRunId;

  @Column(nullable = true, name = "inferred_block_id", length = 64)
  private String inferredBlockId;

  @Column(nullable = true, name = "inferred_trip_id", length = 64)
  private String inferredTripId;

  @Column(nullable = true, name = "inferred_route_id", length = 32)
  private String inferredRouteId;

  @Column(nullable = true, name = "inferred_direction_id", length = 1)
  private String inferredDirectionId;

  @Column(nullable = true, name = "inferred_dest_sign_code")
  private Integer inferredDestSignCode;

  @Column(nullable = true, columnDefinition = "DECIMAL(9,6)", name = "inferred_latitude")
  private BigDecimal inferredLatitude;

  @Column(nullable = true, columnDefinition = "DECIMAL(9,6)", name = "inferred_longitude")
  private BigDecimal inferredLongitude;

  @Column(nullable = false, name = "inferred_phase", length = 32)
  private String inferredPhase;

  @Column(nullable = false, name = "inferred_status", length = 32)
  private String inferredStatus;

  @Column(nullable = false, name = "inference_is_formal")
  private boolean inferenceIsFormal;

  @Column(nullable = true, name = "distance_along_block")
  private Double distanceAlongBlock;

  @Column(nullable = true, name = "distance_along_trip")
  private Double distanceAlongTrip;

  @Column(nullable = true, name = "next_scheduled_stop_id", length = 32)
  private String nextScheduledStopId;

  @Column(nullable = true, name = "next_scheduled_stop_distance")
  private Double nextScheduledStopDistance;

  @Column(nullable = true, name = "schedule_deviation")
  private Integer scheduleDeviation;

  @Column(nullable = true, name = "assigned_run_id", length = 16)
  private String assignedRunId = null;

  public String getUUID() {
    return uuid;
  }

  public void setUUID(String uuid) {
    this.uuid = uuid;
  }

  public Integer getVehicleId() {
    return vehicleId;
  }

  public void setVehicleId(Integer vehicleId) {
    this.vehicleId = vehicleId;
  }

  public Integer getVehicleAgencyId() {
    return vehicleAgencyId;
  }

  public void setVehicleAgencyId(Integer vehicleAgencyId) {
    this.vehicleAgencyId = vehicleAgencyId;
  }

  public String getTimeReported() {
    return timeReported;
  }

  public void setTimeReported(String timeReported) {
    this.timeReported = timeReported;
  }

  public String getTimeReceived() {
    return timeReceived;
  }

  public void setTimeReceived(String timeReceived) {
    this.timeReceived = timeReceived;
  }

  public String getArchiveTimeReceived() {
    return archiveTimeReceived;
  }

  public void setArchiveTimeReceived(String archiveTimeReceived) {
    this.archiveTimeReceived = archiveTimeReceived;
  }

  public String getOperatorIdDesignator() {
    return operatorIdDesignator;
  }

  public void setOperatorIdDesignator(String operatorIdDesignator) {
    this.operatorIdDesignator = operatorIdDesignator;
  }

  public String getRouteIdDesignator() {
    return routeIdDesignator;
  }

  public void setRouteIdDesignator(String routeIdDesignator) {
    this.routeIdDesignator = routeIdDesignator;
  }

  public String getRunIdDesignator() {
    return runIdDesignator;
  }

  public void setRunIdDesignator(String runIdDesignator) {
    this.runIdDesignator = runIdDesignator;
  }

  public Integer getDestSignCode() {
    return destSignCode;
  }

  public void setDestSignCode(Integer destSignCode) {
    this.destSignCode = destSignCode;
  }

  public String getEmergencyCode() {
    return emergencyCode;
  }

  public void setEmergencyCode(String emergencyCode) {
    this.emergencyCode = emergencyCode;
  }

  public BigDecimal getLatitude() {
    return latitude;
  }

  public void setLatitude(BigDecimal latitude) {
    this.latitude = latitude;
  }

  public BigDecimal getLongitude() {
    return longitude;
  }

  public void setLongitude(BigDecimal longitude) {
    this.longitude = longitude;
  }

  public BigDecimal getSpeed() {
    return speed;
  }

  public void setSpeed(BigDecimal speed) {
    this.speed = speed;
  }

  public BigDecimal getDirectionDeg() {
    return directionDeg;
  }

  public void setDirectionDeg(BigDecimal directionDeg) {
    this.directionDeg = directionDeg;
  }

  // inference getters/setter
  public String getAgencyId() {
    return agencyId;
  }

  public void setAgencyId(String agencyId) {
    this.agencyId = agencyId;
  }

  public void setDepotId(String depotId) {
    this.depotId = depotId;
  }

  public String getDepotId() {
    return depotId;
  }

  public String getServiceDate() {
    return serviceDate;
  }

  public void setServiceDate(String serviceDate) {
    this.serviceDate = serviceDate;
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

  public Integer getInferredDestSignCode() {
    return inferredDestSignCode;
  }

  public void setInferredDestSignCode(Integer inferredDestSignCode) {
    this.inferredDestSignCode = inferredDestSignCode;
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

  public boolean isInferenceIsFormal() {
    return inferenceIsFormal;
  }

  public void setInferenceIsFormal(boolean inferenceIsFormal) {
    this.inferenceIsFormal = inferenceIsFormal;
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

  public String getNextScheduledStopId() {
    return nextScheduledStopId;
  }

  public void setNextScheduledStopId(String stopId) {
    this.nextScheduledStopId = stopId;
  }

  public Double getNextScheduledStopDistance() {
    return nextScheduledStopDistance;
  }

  public void setNextScheduledStopDistance(Double stopDistance) {
    this.nextScheduledStopDistance = stopDistance;
  }

  public Integer getScheduleDeviation() {
    return scheduleDeviation;
  }

  public void setScheduleDeviation(Integer scheduleDeviation) {
    this.scheduleDeviation = scheduleDeviation;
  }

  public void setAssignedRunId(String assignedRunId) {
    this.assignedRunId = assignedRunId;
  }

  public String getAssignedRunId() {
    return assignedRunId;
  }

  private String toISODate(Date date) {
    if (date != null) {
      return ISODateTimeFormat.dateTime().print(new DateTime(date));
    }
    return null;
  }

  private String toSimpleDate(Date date) {
    if (date != null) {
      return DateTimeFormat.forPattern("yyyy-MM-dd").print(new DateTime(date));
    }
    return null;
  }
}