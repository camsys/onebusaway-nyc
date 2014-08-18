package org.onebusaway.nyc.report_archive.result;

import java.math.BigDecimal;

/**
 * Represents a historical record in the system. This record is returned as query result for 
 * historical operational API web service
 * @author abelsare
 *
 */
public class HistoricalRecord {

	private String vehicleAgencyId;
	private String timeReported;
	private String timeReceived;
	private String operatorIdDesignator;
	private String routeIdDesignator;
	private String runIdDesignator;
	private Integer destSignCode;
	private String emergencyCode;
	private BigDecimal latitude;
	private BigDecimal longitude;
	private String nmeaSentenceGPRMC;
	private String nmeaSentenceGPGGA;
	private BigDecimal speed;
	private BigDecimal directionDeg;
	private Integer vehicleId;
	private String manufacturerData;
	private Integer requestId;
	private String depotId;
	private String serviceDate;
	private String inferredRunId;
	private String assignedRunId;
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
	private String previousScheduledStopId;
	private Double previousScheduledStopDistance;
	private Integer scheduleDeviation;
	/**
	 * @return the vehicleAgencyId
	 */
	public String getVehicleAgencyId() {
		return vehicleAgencyId;
	}
	/**
	 * @param vehicleAgencyId the vehicleAgencyId to set
	 */
	public void setVehicleAgencyId(String vehicleAgencyId) {
		this.vehicleAgencyId = vehicleAgencyId;
	}
	/**
	 * @return the operatorIdDesignator
	 */
	public String getOperatorIdDesignator() {
		return operatorIdDesignator;
	}
	/**
	 * @param operatorIdDesignator the operatorIdDesignator to set
	 */
	public void setOperatorIdDesignator(String operatorIdDesignator) {
		this.operatorIdDesignator = operatorIdDesignator;
	}
	/**
	 * @return the routeIdDesignator
	 */
	public String getRouteIdDesignator() {
		return routeIdDesignator;
	}
	/**
	 * @param routeIdDesignator the routeIdDesignator to set
	 */
	public void setRouteIdDesignator(String routeIdDesignator) {
		this.routeIdDesignator = routeIdDesignator;
	}
	/**
	 * @return the runIdDesignator
	 */
	public String getRunIdDesignator() {
		return runIdDesignator;
	}
	/**
	 * @param runIdDesignator the runIdDesignator to set
	 */
	public void setRunIdDesignator(String runIdDesignator) {
		this.runIdDesignator = runIdDesignator;
	}
	/**
	 * @return the destSignCode
	 */
	public Integer getDestSignCode() {
		return destSignCode;
	}
	/**
	 * @param destSignCode the destSignCode to set
	 */
	public void setDestSignCode(Integer destSignCode) {
		this.destSignCode = destSignCode;
	}
	/**
	 * @return the emergencyCode
	 */
	public String getEmergencyCode() {
		return emergencyCode;
	}
	/**
	 * @param emergencyCode the emergencyCode to set
	 */
	public void setEmergencyCode(String emergencyCode) {
		this.emergencyCode = emergencyCode;
	}
	/**
	 * @return the latitude
	 */
	public BigDecimal getLatitude() {
		return latitude;
	}
	/**
	 * @param latitude the latitude to set
	 */
	public void setLatitude(BigDecimal latitude) {
		this.latitude = latitude;
	}
	/**
	 * @return the longitude
	 */
	public BigDecimal getLongitude() {
		return longitude;
	}
	/**
	 * @param longitude the longitude to set
	 */
	public void setLongitude(BigDecimal longitude) {
		this.longitude = longitude;
	}
	/**
	 * @return the nmeaSentenceGPRMC
	 */
	public String getNmeaSentenceGPRMC() {
		return nmeaSentenceGPRMC;
	}
	/**
	 * @param nmeaSentenceGPRMC the nmeaSentenceGPRMC to set
	 */
	public void setNmeaSentenceGPRMC(String nmeaSentenceGPRMC) {
		this.nmeaSentenceGPRMC = nmeaSentenceGPRMC;
	}
	/**
	 * @return the nmeaSentenceGPGGA
	 */
	public String getNmeaSentenceGPGGA() {
		return nmeaSentenceGPGGA;
	}
	/**
	 * @param nmeaSentenceGPGGA the nmeaSentenceGPGGA to set
	 */
	public void setNmeaSentenceGPGGA(String nmeaSentenceGPGGA) {
		this.nmeaSentenceGPGGA = nmeaSentenceGPGGA;
	}
	/**
	 * @return the speed
	 */
	public BigDecimal getSpeed() {
		return speed;
	}
	/**
	 * @param speed the speed to set
	 */
	public void setSpeed(BigDecimal speed) {
		this.speed = speed;
	}
	/**
	 * @return the directionDeg
	 */
	public BigDecimal getDirectionDeg() {
		return directionDeg;
	}
	/**
	 * @param directionDeg the directionDeg to set
	 */
	public void setDirectionDeg(BigDecimal directionDeg) {
		this.directionDeg = directionDeg;
	}
	/**
	 * @return the vehicleId
	 */
	public Integer getVehicleId() {
		return vehicleId;
	}
	/**
	 * @param vehicleId the vehicleId to set
	 */
	public void setVehicleId(Integer vehicleId) {
		this.vehicleId = vehicleId;
	}
	/**
	 * @return the manufacturerData
	 */
	public String getManufacturerData() {
		return manufacturerData;
	}
	/**
	 * @param manufacturerData the manufacturerData to set
	 */
	public void setManufacturerData(String manufacturerData) {
		this.manufacturerData = manufacturerData;
	}
	/**
	 * @return the requestId
	 */
	public Integer getRequestId() {
		return requestId;
	}
	/**
	 * @param requestId the requestId to set
	 */
	public void setRequestId(Integer requestId) {
		this.requestId = requestId;
	}
	/**
	 * @return the depotId
	 */
	public String getDepotId() {
		return depotId;
	}
	/**
	 * @param depotId the depotId to set
	 */
	public void setDepotId(String depotId) {
		this.depotId = depotId;
	}
	/**
	 * @return the inferredRunId
	 */
	public String getInferredRunId() {
		return inferredRunId;
	}
	/**
	 * @param inferredRunId the inferredRunId to set
	 */
	public void setInferredRunId(String inferredRunId) {
		this.inferredRunId = inferredRunId;
	}
	/**
	 * @return the assignedRunId
	 */
	public String getAssignedRunId() {
		return assignedRunId;
	}
	/**
	 * @param assignedRunId the assignedRunId to set
	 */
	public void setAssignedRunId(String assignedRunId) {
		this.assignedRunId = assignedRunId;
	}
	/**
	 * @return the inferredBlockId
	 */
	public String getInferredBlockId() {
		return inferredBlockId;
	}
	/**
	 * @param inferredBlockId the inferredBlockId to set
	 */
	public void setInferredBlockId(String inferredBlockId) {
		this.inferredBlockId = inferredBlockId;
	}
	/**
	 * @return the inferredTripId
	 */
	public String getInferredTripId() {
		return inferredTripId;
	}
	/**
	 * @param inferredTripId the inferredTripId to set
	 */
	public void setInferredTripId(String inferredTripId) {
		this.inferredTripId = inferredTripId;
	}
	/**
	 * @return the inferredRouteId
	 */
	public String getInferredRouteId() {
		return inferredRouteId;
	}
	/**
	 * @param inferredRouteId the inferredRouteId to set
	 */
	public void setInferredRouteId(String inferredRouteId) {
		this.inferredRouteId = inferredRouteId;
	}
	/**
	 * @return the inferredDirectionId
	 */
	public String getInferredDirectionId() {
		return inferredDirectionId;
	}
	/**
	 * @param inferredDirectionId the inferredDirectionId to set
	 */
	public void setInferredDirectionId(String inferredDirectionId) {
		this.inferredDirectionId = inferredDirectionId;
	}
	/**
	 * @return the inferredDestSignCode
	 */
	public Integer getInferredDestSignCode() {
		return inferredDestSignCode;
	}
	/**
	 * @param inferredDestSignCode the inferredDestSignCode to set
	 */
	public void setInferredDestSignCode(Integer inferredDestSignCode) {
		this.inferredDestSignCode = inferredDestSignCode;
	}
	/**
	 * @return the inferredLatitude
	 */
	public BigDecimal getInferredLatitude() {
		return inferredLatitude;
	}
	/**
	 * @param inferredLatitude the inferredLatitude to set
	 */
	public void setInferredLatitude(BigDecimal inferredLatitude) {
		this.inferredLatitude = inferredLatitude;
	}
	/**
	 * @return the inferredLongitude
	 */
	public BigDecimal getInferredLongitude() {
		return inferredLongitude;
	}
	/**
	 * @param inferredLongitude the inferredLongitude to set
	 */
	public void setInferredLongitude(BigDecimal inferredLongitude) {
		this.inferredLongitude = inferredLongitude;
	}
	/**
	 * @return the inferredPhase
	 */
	public String getInferredPhase() {
		return inferredPhase;
	}
	/**
	 * @param inferredPhase the inferredPhase to set
	 */
	public void setInferredPhase(String inferredPhase) {
		this.inferredPhase = inferredPhase;
	}
	/**
	 * @return the inferredStatus
	 */
	public String getInferredStatus() {
		return inferredStatus;
	}
	/**
	 * @param inferredStatus the inferredStatus to set
	 */
	public void setInferredStatus(String inferredStatus) {
		this.inferredStatus = inferredStatus;
	}
	/**
	 * @return the inferenceIsFormal
	 */
	public boolean isInferenceIsFormal() {
		return inferenceIsFormal;
	}
	/**
	 * @param inferenceIsFormal the inferenceIsFormal to set
	 */
	public void setInferenceIsFormal(boolean inferenceIsFormal) {
		this.inferenceIsFormal = inferenceIsFormal;
	}
	/**
	 * @return the distanceAlongBlock
	 */
	public Double getDistanceAlongBlock() {
		return distanceAlongBlock;
	}
	/**
	 * @param distanceAlongBlock the distanceAlongBlock to set
	 */
	public void setDistanceAlongBlock(Double distanceAlongBlock) {
		this.distanceAlongBlock = distanceAlongBlock;
	}
	/**
	 * @return the distanceAlongTrip
	 */
	public Double getDistanceAlongTrip() {
		return distanceAlongTrip;
	}
	/**
	 * @param distanceAlongTrip the distanceAlongTrip to set
	 */
	public void setDistanceAlongTrip(Double distanceAlongTrip) {
		this.distanceAlongTrip = distanceAlongTrip;
	}
	/**
	 * @return the nextScheduledStopId
	 */
	public String getNextScheduledStopId() {
		return nextScheduledStopId;
	}
	/**
	 * @param nextScheduledStopId the nextScheduledStopId to set
	 */
	public void setNextScheduledStopId(String nextScheduledStopId) {
		this.nextScheduledStopId = nextScheduledStopId;
	}
	/**
	 * @return the nextScheduledStopDistance
	 */
	public Double getNextScheduledStopDistance() {
		return nextScheduledStopDistance;
	}
	/**
	 * @param nextScheduledStopDistance the nextScheduledStopDistance to set
	 */
	public void setNextScheduledStopDistance(Double nextScheduledStopDistance) {
		this.nextScheduledStopDistance = nextScheduledStopDistance;
	}
	public String getPreviousScheduledStopId() {
		return previousScheduledStopId;
	}
	public void setPreviousScheduledStopId(String previousScheduledStopId) {
		this.previousScheduledStopId = previousScheduledStopId;
	}
	public Double getPreviousScheduledStopDistance() {
		return previousScheduledStopDistance;
	}
	public void setPreviousScheduledStopDistance(
			Double previousScheduledStopDistance) {
		this.previousScheduledStopDistance = previousScheduledStopDistance;
	}
	/**
	 * @return the scheduleDeviation
	 */
	public Integer getScheduleDeviation() {
		return scheduleDeviation;
	}
	/**
	 * @param scheduleDeviation the scheduleDeviation to set
	 */
	public void setScheduleDeviation(Integer scheduleDeviation) {
		this.scheduleDeviation = scheduleDeviation;
	}
	/**
	 * @return the timeReported
	 */
	public String getTimeReported() {
		return timeReported;
	}
	/**
	 * @param timeReported the timeReported to set
	 */
	public void setTimeReported(String timeReported) {
		this.timeReported = timeReported;
	}
	/**
	 * @return the timeReceived
	 */
	public String getTimeReceived() {
		return timeReceived;
	}
	/**
	 * @param timeReceived the timeReceived to set
	 */
	public void setTimeReceived(String timeReceived) {
		this.timeReceived = timeReceived;
	}
	/**
	 * @return the serviceDate
	 */
	public String getServiceDate() {
		return serviceDate;
	}
	/**
	 * @param serviceDate the serviceDate to set
	 */
	public void setServiceDate(String serviceDate) {
		this.serviceDate = serviceDate;
	}
	
}
