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
 * Model to wrap CcLocationReportRecord and ArchivedInferredLocationRecord
 * to present as a unified object via json.
 * @author sheldonabrown
 *
 */
package org.onebusaway.nyc.report_archive.model;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.ISODateTimeFormat;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class CcAndInferredLocationRecord implements Serializable {

    public CcAndInferredLocationRecord() { 
    }    
    public CcAndInferredLocationRecord (ArchivedInferredLocationRecord inferred,
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
    }

    // realtime fields
  private String uuid;
  private Integer vehicleAgencyId;
  private String timeReported;
  private String timeReceived;
  private String archiveTimeReceived;
  private String operatorIdDesignator;
  private String routeIdDesignator;
  private String runIdDesignator;
  private Integer destSignCode;
  private String emergencyCode;
  private BigDecimal latitude;
  private BigDecimal longitude;
  private BigDecimal speed;
  private BigDecimal directionDeg;
    // inferred fields
  private String agencyId;
  private Integer vehicleId;
  private String depotId;
  private String serviceDate;
  private String inferredOperatorId;
  private String inferredRunId;
  private String inferredBlockId;
  private String inferredTripId;
  private String inferredRouteId;
  private String inferredDirectionId;
  private Integer inferredDestSignCode;
  private BigDecimal inferredLatitude;
  private BigDecimal inferredLongitude;
  private String inferredPhase;
  private String inferredStatus;
  private boolean inferenceIsFormal;
  private Double distanceAlongBlock;
  private Double distanceAlongTrip;
  private String nextScheduledStopId;
  private Double nextScheduledStopDistance;
  private Integer scheduleDeviation;

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