package org.onebusaway.nyc.transit_data_federation.model;

import java.io.Serializable;

public class NycQueuedInferredLocationRecord implements Serializable {

	private static final long serialVersionUID = 1L;
		
	private Long recordTimestamp;
	
	private Long locationUpdateTimestamp;

	private String vehicleId;

	private String destinationSignCode;
	
	private Long serviceDate;
	
	private Long scheduleDeviation;
	
	private String blockId;

	private String tripId;

	private Double distanceAlongBlock;

	private Double distanceAlongTrip;

	private Double inferredLatitude;
	
	private Double inferredLongitude;

	private Double rawLatitude;
	
	private Double rawLongitude;

	private String phase;
	
	private String status;
	
	private boolean inferenceIsFormal;
	
	public NycQueuedInferredLocationRecord() {}

	public Long getRecordTimestamp() {
		return recordTimestamp;
	}

	public void setRecordTimestamp(Long recordTimestamp) {
		this.recordTimestamp = recordTimestamp;
	}

	public Long getLocationUpdateTimestamp() {
		return locationUpdateTimestamp;
	}

	public void setLocationUpdateTimestamp(Long locationUpdateTimestamp) {
		this.locationUpdateTimestamp = locationUpdateTimestamp;
	}

	public String getVehicleId() {
		return vehicleId;
	}

	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}

	public String getDestinationSignCode() {
		return destinationSignCode;
	}

	public void setDestinationSignCode(String destinationSignCode) {
		this.destinationSignCode = destinationSignCode;
	}

	public Long getServiceDate() {
		return serviceDate;
	}

	public void setServiceDate(Long serviceDate) {
		this.serviceDate = serviceDate;
	}

	public Long getScheduleDeviation() {
		return scheduleDeviation;
	}

	public void setScheduleDeviation(Long scheduleDeviation) {
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

	public Double getRawLatitude() {
		return rawLatitude;
	}

	public void setRawLatitude(Double rawLatitude) {
		this.rawLatitude = rawLatitude;
	}

	public Double getRawLongitude() {
		return rawLongitude;
	}

	public void setRawLongitude(Double rawLongitude) {
		this.rawLongitude = rawLongitude;
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

	public boolean isInferenceIsFormal() {
		return inferenceIsFormal;
	}

	public void setInferenceIsFormal(boolean inferenceIsFormal) {
		this.inferenceIsFormal = inferenceIsFormal;
	}


}


