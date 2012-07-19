package org.onebusaway.nyc.admin.model.ui;

/**
 * Holds vehicle status info
 * @author abelsare
 *
 */
public class VehicleStatus {
	
	private String vehicleId;
	private String lastUpdateTime;
	private String inferredState;
	private String inferredDSC;
	private String route;
	private String direction;
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
	 * @return the inferredDSC
	 */
	public String getInferredDSC() {
		return inferredDSC;
	}
	/**
	 * @param inferredDSC the inferredDSC to set
	 */
	public void setInferredDSC(String inferredDSC) {
		this.inferredDSC = inferredDSC;
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
	 * @return the direction
	 */
	public String getDirection() {
		return direction;
	}
	/**
	 * @param direction the direction to set
	 */
	public void setDirection(String direction) {
		this.direction = direction;
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
	

}
