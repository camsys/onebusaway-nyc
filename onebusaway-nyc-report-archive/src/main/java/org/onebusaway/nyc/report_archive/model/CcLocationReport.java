package org.onebusaway.nyc.report_archive.model;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "obanyc_cclocationreport_archive")
@AccessType("field")
@Cache(usage = CacheConcurrencyStrategy.NONE)
public class CcLocationReport implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  @AccessType("property")
  private Integer id;

  @Column(nullable = false)
  private Integer requestId;
  @Column(nullable = false)
  private Integer vehicleId;
  @Column(nullable = false)
  private Integer vehicleAgencyId;
  @Column(nullable = false)
  private String vehicleAgencydesignator; // TODO length?
  @Column(nullable = false)
  private Integer statusInfo;
  @Column(nullable = false)
  private Date timeReported;
  @Column(nullable = false)
  private Integer latitude;
  @Column(nullable = false)
  private Integer longitude;
  @Column(nullable = false, columnDefinition = "DECIMAL(4,1)")
  private BigDecimal directionDeg;
  @Column(nullable = false)
  private Integer speed;
  private Integer dataQuality; // Nullable
  @Column(nullable = false)
  private String manufacturerData; // TODO length?
  @Column(nullable = false)
  private Integer operatorId;
  @Column(nullable = false)
  private String operatorIdDesignator; // TODO length?
  @Column(nullable = false)
  private Integer runId;
  @Column(nullable = false)
  private String runIdDesignator; // TODO Length?
  @Column(nullable = false)
  private Integer destSignCode;
  private String emergencyCode; // Nullable, TODO Length?
  @Column(nullable = false)
  private Integer routeId;
  @Column(nullable = false)
  private String routeIdDesignator; // TODO length?
  private String nmeaSentenceGPGGA; // Nullable, TODO length?
  private String nmeaSentenceGPRMC; // Nullable, TODO length?

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public Integer getRequestId() {
    return requestId;
  }

  public void setRequestId(Integer requestId) {
    this.requestId = requestId;
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

  public String getVehicleAgencydesignator() {
    return vehicleAgencydesignator;
  }

  public void setVehicleAgencydesignator(String vehicleAgencydesignator) {
    this.vehicleAgencydesignator = vehicleAgencydesignator;
  }

  public Integer getStatusInfo() {
    return statusInfo;
  }

  public void setStatusInfo(Integer statusInfo) {
    this.statusInfo = statusInfo;
  }

  public Date getTimeReported() {
    return timeReported;
  }

  public void setTimeReported(Date timeReported) {
    this.timeReported = timeReported;
  }

  public Integer getLatitude() {
    return latitude;
  }

  public void setLatitude(Integer latitude) {
    this.latitude = latitude;
  }

  public Integer getLongitude() {
    return longitude;
  }

  public void setLongitude(Integer longitude) {
    this.longitude = longitude;
  }

  public BigDecimal getDirectionDeg() {
    return directionDeg;
  }

  public void setDirectionDeg(BigDecimal directionDeg) {
    this.directionDeg = directionDeg;
  }

  public Integer getSpeed() {
    return speed;
  }

  public void setSpeed(Integer speed) {
    this.speed = speed;
  }

  public Integer getDataQuality() {
    return dataQuality;
  }

  public void setDataQuality(Integer dataQuality) {
    this.dataQuality = dataQuality;
  }

  public String getManufacturerData() {
    return manufacturerData;
  }

  public void setManufacturerData(String manufacturerData) {
    this.manufacturerData = manufacturerData;
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

  public Integer getRunId() {
    return runId;
  }

  public void setRunId(Integer runId) {
    this.runId = runId;
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

  public Integer getRouteId() {
    return routeId;
  }

  public void setRouteId(Integer routeId) {
    this.routeId = routeId;
  }

  public String getRouteIdDesignator() {
    return routeIdDesignator;
  }

  public void setRouteIdDesignator(String routeIdDesignator) {
    this.routeIdDesignator = routeIdDesignator;
  }

  public String getNmeaSentenceGPGGA() {
    return nmeaSentenceGPGGA;
  }

  public void setNmeaSentenceGPGGA(String nmeaSentenceGPGGA) {
    this.nmeaSentenceGPGGA = nmeaSentenceGPGGA;
  }

  public String getNmeaSentenceGPRMC() {
    return nmeaSentenceGPRMC;
  }

  public void setNmeaSentenceGPRMC(String nmeaSentenceGPRMC) {
    this.nmeaSentenceGPRMC = nmeaSentenceGPRMC;
  }
}
