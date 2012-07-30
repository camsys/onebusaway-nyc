package org.onebusaway.nyc.admin.model.ui;

/**
 * Holds vehicle status info
 * @author abelsare
 *
 */
public class VehicleStatus {
	
	private String status;
	private String vehicleId;
	private String lastUpdate;
	private String inferredState;
	private String inferredDestination;
	private String observedDSC;
	private String pullinTime;
	private String pulloutTime;
	private String details;
	private String route;
	private String depot;
	private String emergencyStatus;
	private String timeReceived;
	
	/**
	 * @return the vehicleId
	 */
	public String getVehicleId() {
		return vehicleId;
	}
	/**
	 * @param vehicleId the vehicleId to set
	 */
	public void setVehicleId(String vehicleId) {
		this.vehicleId = vehicleId;
	}
	/**
	 * @return the lastUpdate
	 */
	public String getLastUpdate() {
		return lastUpdate;
	}
	/**
	 * @param lastUpdateTime the lastUpdate to set
	 */
	public void setLastUpdate(String lastUpdate) {
		this.lastUpdate = lastUpdate;
	}
	/**
	 * @return the inferredState
	 */
	public String getInferredState() {
		return inferredState;
	}
	/**
	 * @param inferredState the inferredState to set
	 */
	public void setInferredState(String inferredState) {
		this.inferredState = inferredState;
	}
	/**
	 * @return the observedDSC
	 */
	public String getObservedDSC() {
		return observedDSC;
	}
	/**
	 * @param observedDSC the observedDSC to set
	 */
	public void setObservedDSC(String observedDSC) {
		this.observedDSC = observedDSC;
	}
	/**
	 * @return the pullinTime
	 */
	public String getPullinTime() {
		return pullinTime;
	}
	/**
	 * @param pullinTime the pullinTime to set
	 */
	public void setPullinTime(String pullinTime) {
		this.pullinTime = pullinTime;
	}
	/**
	 * @return the pulloutTime
	 */
	public String getPulloutTime() {
		return pulloutTime;
	}
	/**
	 * @param pulloutTime the pulloutTime to set
	 */
	public void setPulloutTime(String pulloutTime) {
		this.pulloutTime = pulloutTime;
	}
	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	/**
	 * @return the inferredDestination
	 */
	public String getInferredDestination() {
		return inferredDestination;
	}
	/**
	 * @param inferredDestination the inferredDestination to set
	 */
	public void setInferredDestination(String inferredDestination) {
		this.inferredDestination = inferredDestination;
	}
	/**
	 * @return the details
	 */
	public String getDetails() {
		return details;
	}
	/**
	 * @param details the details to set
	 */
	public void setDetails(String details) {
		this.details = details;
	}
	/**
	 * @return the route
	 */
	public String getRoute() {
		return route;
	}
	/**
	 * @param route the route to set
	 */
	public void setRoute(String route) {
		this.route = route;
	}
	/**
	 * @return the depot
	 */
	public String getDepot() {
		return depot;
	}
	/**
	 * @param depot the depot to set
	 */
	public void setDepot(String depot) {
		this.depot = depot;
	}
	/**
	 * @return the emergencyStatus
	 */
	public String getEmergencyStatus() {
		return emergencyStatus;
	}
	/**
	 * @param emergencyStatus the emergencyStatus to set
	 */
	public void setEmergencyStatus(String emergencyStatus) {
		this.emergencyStatus = emergencyStatus;
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
	
	

}
