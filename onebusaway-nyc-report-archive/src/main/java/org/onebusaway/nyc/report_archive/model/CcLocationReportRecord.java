package org.onebusaway.nyc.report_archive.model;

import org.hibernate.annotations.AccessType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Index;

import org.joda.time.format.ISODateTimeFormat;

import tcip_final_3_0_5_1.CcLocationReport;

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
public class CcLocationReportRecord implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  @AccessType("property")
  private Integer id;

  @Column(nullable = false, name = "request_id")
  private Integer requestId;

  @Column(nullable = false, name = "vehicle_id")
  @Index(name = "vehicle_id")
  private Integer vehicleId;

  @Column(nullable = false, name = "vehicle_agency_id")
  private Integer vehicleAgencyId;
  
  @Column(nullable = false, name = "vehicle_agency_designator", length = 64)
  private String vehicleAgencydesignator;
  
  @Column(nullable = false, name = "status_info")
  private Integer statusInfo;
  
  @Column(nullable = false, name = "time_reported")
  @Index(name = "time_reported")
  private Date timeReported;
  
  @Column(nullable = false, name = "time_received")
  private Date timeReceived;
  
  @Column(nullable = false, columnDefinition = "DECIMAL(9,6)", name = "latitude")
  private BigDecimal latitude;
  
  @Column(nullable = false, columnDefinition = "DECIMAL(9,6)", name = "longitude")
  private BigDecimal longitude;
  
  @Column(nullable = false, columnDefinition = "DECIMAL(5,2)", name = "direction_deg")
  private BigDecimal directionDeg;
  
  @Column(nullable = false, columnDefinition = "DECIMAL(4,1)", name = "speed")
  private BigDecimal speed;
  
  @Column(name = "data_quality")
  private Integer dataQuality;
  
  @Column(nullable = false, name = "manufacturer_data", length = 64)
  private String manufacturerData;
  
  @Column(nullable = false, name = "operator_id")
  private Integer operatorId;
  
  @Column(nullable = false, name = "operator_id_designator", length = 16)
  private String operatorIdDesignator;
  
  @Column(nullable = false, name = "run_id")
  private Integer runId;
  
  @Column(nullable = false, name = "run_id_designator", length = 32)
  private String runIdDesignator;
  
  @Column(nullable = false, name = "dest_sign_code")
  private Integer destSignCode;
  
  @Column(name = "emergency_code", length = 1)
  private String emergencyCode;
  
  @Column(nullable = false, name = "route_id")
  private Integer routeId;
  
  @Column(nullable = false, name = "route_id_designator", length =16)
  private String routeIdDesignator;
  
  @Column(name = "nmea_sentence_gpgga", length = 160)
  private String nmeaSentenceGPGGA;
  
  @Column(name = "nmea_sentence_gprmc", length = 160)
  private String nmeaSentenceGPRMC;
  
  @Column(nullable = false, name = "raw_message", length = 1400)
  private String rawMessage;

  public CcLocationReportRecord() {
  }

  public CcLocationReportRecord(CcLocationReport message, String contents) {
    super();
    setRequestId((int) message.getRequestId());
    setDestSignCode(message.getDestSignCode().intValue());
    setDirectionDeg(message.getDirection().getDeg());
    setLatitude(convertMicrodegreesToDegrees(message.getLatitude()));
    setLongitude(convertMicrodegreesToDegrees(message.getLongitude()));
    setManufacturerData(message.getManufacturerData());
    setOperatorId((int) message.getOperatorID().getOperatorId());
    setOperatorIdDesignator(message.getOperatorID().getDesignator());
    setRequestId((int) message.getRequestId());
    setRouteId((int) message.getRouteID().getRouteId());
    setRouteIdDesignator(message.getRouteID().getRouteDesignator());
    setRunId((int) message.getRunID().getRunId());
    setRunIdDesignator(message.getRunID().getDesignator());
    setSpeed(convertSpeed(message.getSpeed()));
    setStatusInfo(message.getStatusInfo());
    setTimeReported(convertTime(message.getTimeReported()));
    setTimeReceived(new Date());
    setVehicleAgencydesignator(message.getVehicle().getAgencydesignator());
    setVehicleAgencyId(message.getVehicle().getAgencyId().intValue());
    setVehicleId((int) message.getVehicle().getVehicleId());
    setRawMessage(contents);
  }

  // Timestamp of the on-board device when this message is created, in standard XML timestamp format
  // "time-reported": "2011-06-22T10:58:10.0-00:00"
  private Date convertTime(String timeString) {
    return ISODateTimeFormat.dateTime().parseDateTime(timeString).toDate();
  }

  // Instantaneous speed.  Per SAE J1587 speed is in half mph increments with an offset of -15mph.
  // For example, 002.642 knots (from RMC) is 3.04 mph, so (15*2) + (3 * 2) = 36
  // Valid range is [0,255], which equates to [-15mph, +112.5mph]
  private BigDecimal convertSpeed(short speed) {
    return new BigDecimal((speed / 2.0) - 30);
  }

  private BigDecimal convertMicrodegreesToDegrees(int latlong) {
    return new BigDecimal(latlong * Math.pow(10.0, -6));
  }

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

  public BigDecimal getDirectionDeg() {
    return directionDeg;
  }

  public void setDirectionDeg(BigDecimal directionDeg) {
    this.directionDeg = directionDeg;
  }

  public BigDecimal getSpeed() {
    return speed;
  }

  public void setSpeed(BigDecimal speed) {
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

  public Date getTimeReceived() {
    return timeReceived;
  }

  public void setTimeReceived(Date timeReceived) {
    this.timeReceived = timeReceived;
  }

  public String getRawMessage() {
    return rawMessage;
  }

  public void setRawMessage(String rawMessage) {
    this.rawMessage = rawMessage;
  }
}
