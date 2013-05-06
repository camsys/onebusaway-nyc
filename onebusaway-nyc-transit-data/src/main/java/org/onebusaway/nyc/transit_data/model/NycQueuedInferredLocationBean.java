package org.onebusaway.nyc.transit_data.model;

import java.io.Serializable;

/**
 * An "over the wire", queued inferred location result--gets passed between the inference
 * engine and the TDF/TDS running on all front-end notes, plus the archiver and other inference
 * data consumers.
 * 
 * @author jmaki
 *
 */
public class NycQueuedInferredLocationBean implements Serializable {

	private static final long serialVersionUID = 1L;

	// the timestamp applied to the record when received by the inference engine
	private Long recordTimestamp;

	private String vehicleId;

	// service date of trip/block
	private Long serviceDate;

	private Integer scheduleDeviation;

	private String blockId;

	private String tripId;

	private Double distanceAlongBlock;

	private Double distanceAlongTrip;

	// snapped lat/long of vehicle to route shape
	private Double inferredLatitude;

	private Double inferredLongitude;

	// raw lat/long of vehicle as reported by BHS.
	private Double observedLatitude;

	private Double observedLongitude;

	// inferred operational status/phase
	private String phase;

	private String status;

	// inference engine telemetry
	private NycVehicleManagementStatusBean managementRecord;

	private String runId;

	private String routeId;

	private Double bearing;

	public NycQueuedInferredLocationBean() {}

	public Long getRecordTimestamp() {
		return recordTimestamp;
	}

	public void setRecordTimestamp(Long recordTimestamp) {
		this.recordTimestamp = recordTimestamp;
	}

	public String getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	public Long getServiceDate() {
		return serviceDate;
	}

	public void setServiceDate(Long serviceDate) {
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

	public void setDistanceAlongBlock(Double distanceAlongBlock) {
		this.distanceAlongBlock = distanceAlongBlock;
	}

	public Double getDistanceAlongTrip() {
		return distanceAlongTrip;
	}

	public void setDistanceAlongTrip(Double distanceAlongTrip) {
		this.distanceAlongTrip = distanceAlongTrip;
	}

	public Double getInferredLatitude() {
		return inferredLatitude;
	}

	public void setInferredLatitude(Double inferredLatitude) {
		this.inferredLatitude = inferredLatitude;
	}

	public Double getInferredLongitude() {
		return inferredLongitude;
	}

	public void setInferredLongitude(Double inferredLongitude) {
		this.inferredLongitude = inferredLongitude;
	}

	public Double getObservedLatitude() {
		return observedLatitude;
	}

	public void setObservedLatitude(Double observedLatitude) {
		this.observedLatitude = observedLatitude;
	}

	public Double getObservedLongitude() {
		return observedLongitude;
	}

	public void setObservedLongitude(Double observedLongitude) {
		this.observedLongitude = observedLongitude;
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

	public NycVehicleManagementStatusBean getManagementRecord() {
		return managementRecord;
	}

	public void setManagementRecord(NycVehicleManagementStatusBean managementRecord) {
		this.managementRecord = managementRecord;
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

	public void setBearing(Double bearing) {
		this.bearing = bearing;
	}

	public double getBearing() {
		return bearing;
	}  

}


