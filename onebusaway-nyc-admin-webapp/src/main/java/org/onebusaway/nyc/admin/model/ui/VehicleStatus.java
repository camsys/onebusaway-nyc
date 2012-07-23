package org.onebusaway.nyc.admin.model.ui;

/**
 * Holds vehicle status info
 * @author abelsare
 *
 */
public class VehicleStatus {
	
	private String status;
	private String vehicleId;
	private String lastUpdateTime;
	private String inferredState;
	private String inferredDestination;
	private String observedDSC;
	private String pullinTime;
	private String pulloutTime;
	
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
	 * @return the lastUpdateTime
	 */
	public String getLastUpdateTime() {
		return lastUpdateTime;
	}
	/**
	 * @param lastUpdateTime the lastUpdateTime to set
	 */
	public void setLastUpdateTime(String lastUpdateTime) {
		this.lastUpdateTime = lastUpdateTime;
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
	

}
