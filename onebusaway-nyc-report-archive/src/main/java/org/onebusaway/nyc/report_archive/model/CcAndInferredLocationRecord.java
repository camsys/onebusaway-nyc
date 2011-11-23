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


import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

public class CcAndInferredLocationRecord implements Serializable {

    public CcAndInferredLocationRecord() { 
    }
    
    public CcAndInferredLocationRecord (ArchivedInferredLocationRecord inferred,
					CcLocationReportRecord realtime) {
	// realtime fields
	setVehicleAgencyId(realtime.getVehicleAgencyId());
	setVehicleAgencyDesignator(realtime.getVehicleAgencyDesignator());
	setTimeReported(realtime.getTimeReported());
	setTimeReceived(realtime.getTimeReceived());
	setOperatorId(realtime.getOperatorId());
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
	setServiceDate(inferred.getServiceDate());
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
	setScheduleDeviation(inferred.getScheduleDeviation());
    }

    // realtime fields
  private Integer vehicleAgencyId;
  private String vehicleAgencyDesignator;
  private Date timeReported;
  private Date timeReceived;
  private Integer operatorId;
  private String operatorIdDesignator;
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
  private Date serviceDate;
  private String inferredOperatorId;
  private String inferredRunId;
  private String inferredBlockId;
  private String inferredTripId;
  private String inferredRouteId;
  private String inferredDirectionId;
  private String inferredDestSignCode;
  private BigDecimal inferredLatitude;
  private BigDecimal inferredLongitude;
  private String inferredPhase;
  private String inferredStatus;
  private boolean inferenceIsFormal;
  private Double distanceAlongBlock;
  private Double distanceAlongTrip;
  private Integer scheduleDeviation;


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

  public String getVehicleAgencyDesignator() {
    return vehicleAgencyDesignator;
  }

  public void setVehicleAgencyDesignator(String vehicleAgencyDesignator) {
    this.vehicleAgencyDesignator = vehicleAgencyDesignator;
  }

  public Date getTimeReported() {
    return timeReported;
  }

  public void setTimeReported(Date timeReported) {
    this.timeReported = timeReported;
  }

  public Date getTimeReceived() {
    return timeReceived;
  }

  public void setTimeReceived(Date timeReceived) {
    this.timeReceived = timeReceived;
  }

  public Integer getOperatorId() {
    return operatorId;
  }

  public void setOperatorId(Integer operatorId) {
    this.operatorId = operatorId;
  }

  public String getOperatorIdDesignator() {
    return operatorIdDesignator;
  }

  public void setOperatorIdDesignator(String operatorIdDesignator) {
    this.operatorIdDesignator = operatorIdDesignator;
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

  public Date getServiceDate() {
    return serviceDate;
  }

  public void setServiceDate(Date serviceDate) {
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

  public String getInferredDestSignCode() {
    return inferredDestSignCode;
  }

  public void setInferredDestSignCode(String inferredDestSignCode) {
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

  public Integer getScheduleDeviation() {
    return scheduleDeviation;
  }

  public void setScheduleDeviation(Integer scheduleDeviation) {
    this.scheduleDeviation = scheduleDeviation;
  }

}