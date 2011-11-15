package org.onebusaway.nyc.webapp.actions.admin.model;

public class ArchiveRecord {

	private int id;
	private String recordTimestamp;  // date/time
	private String vehicleId;
	private String serviceDate; // date/time
	private double observedLatitude;
	private double observedLongitude;
	private String phase;
	private String status;
	private int bearing;
	private String activeBundleId;
	private long lastUpdateTime; // seems to be in ms
	private long lastLocationUpdateTime; // seems to be in ms
	private double lastObservedLatitude;
	private double lastObservedLongitude;
	private String mostRecentObservedDestinationSignCode;
	private boolean inferenceIsEnabled;
	private boolean inferenceEngineIsPrimary;
	private boolean inferenceIsFormal;
	private boolean emergencyFlag;
	private String depotId;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getRecordTimestamp() {
		return recordTimestamp;
	}
	public void setRecordTimestamp(String recordTimestamp) {
		this.recordTimestamp = recordTimestamp;
	}
	public String getVehicleId() {
		return vehicleId;
	}
	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}
	public String getServiceDate() {
		return serviceDate;
	}
	public void setServiceDate(String serviceDate) {
		this.serviceDate = serviceDate;
	}
	public double getObservedLatitude() {
		return observedLatitude;
	}
	public void setObservedLatitude(double observedLatitude) {
		this.observedLatitude = observedLatitude;
	}
	public double getObservedLongitude() {
		return observedLongitude;
	}
	public void setObservedLongitude(double observedLongitude) {
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
	public int getBearing() {
		return bearing;
	}
	public void setBearing(int bearing) {
		this.bearing = bearing;
	}
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
	public void setLastLocationUpdateTime(long lastLocationUpdateTime) {
		this.lastLocationUpdateTime = lastLocationUpdateTime;
	}
	public double getLastObservedLatitude() {
		return lastObservedLatitude;
	}
	public void setLastObservedLatitude(double lastObservedLatitude) {
		this.lastObservedLatitude = lastObservedLatitude;
	}
	public double getLastObservedLongitude() {
		return lastObservedLongitude;
	}
	public void setLastObservedLongitude(double lastObservedLongitude) {
		this.lastObservedLongitude = lastObservedLongitude;
	}
	public String getMostRecentObservedDestinationSignCode() {
		return mostRecentObservedDestinationSignCode;
	}
	public void setMostRecentObservedDestinationSignCode(
			String mostRecentObservedDestinationSignCode) {
		this.mostRecentObservedDestinationSignCode = mostRecentObservedDestinationSignCode;
	}
	public boolean isInferenceIsEnabled() {
		return inferenceIsEnabled;
	}
	public void setInferenceIsEnabled(boolean inferenceIsEnabled) {
		this.inferenceIsEnabled = inferenceIsEnabled;
	}
	public boolean isInferenceEngineIsPrimary() {
		return inferenceEngineIsPrimary;
	}
	public void setInferenceEngineIsPrimary(boolean inferenceEngineIsPrimary) {
		this.inferenceEngineIsPrimary = inferenceEngineIsPrimary;
	}
	public boolean isInferenceIsFormal() {
		return inferenceIsFormal;
	}
	public void setInferenceIsFormal(boolean inferenceIsFormal) {
		this.inferenceIsFormal = inferenceIsFormal;
	}
	public boolean isEmergencyFlag() {
		return emergencyFlag;
	}
	public void setEmergencyFlag(boolean emergencyFlag) {
		this.emergencyFlag = emergencyFlag;
	}
	public String getDepotId() {
		return depotId;
	}
	public void setDepotId(String depotId) {
		this.depotId = depotId;
	}
}
