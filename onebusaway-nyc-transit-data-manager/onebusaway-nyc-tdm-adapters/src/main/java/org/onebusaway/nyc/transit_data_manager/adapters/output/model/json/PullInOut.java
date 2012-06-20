package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json;

/**
 * Holds vehicle PIPO output data
 * @author abelsare
 *
 */
public class PullInOut {
	private String vehicleId;
	private String vehicleAgencyId;
	private String agencyId;
	private String depot;
	private String serviceDate;
	private String pulloutTime;
	private String run;
	private String operatorId;
	private String pullinTime;
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
	 * @return the agencyId
	 */
	public String getAgencyId() {
		return agencyId;
	}
	/**
	 * @param agencyId the agencyId to set
	 */
	public void setAgencyId(String agencyId) {
		this.agencyId = agencyId;
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
	 * @return the run
	 */
	public String getRun() {
		return run;
	}
	/**
	 * @param run the run to set
	 */
	public void setRun(String run) {
		this.run = run;
	}
	/**
	 * @return the operatorId
	 */
	public String getOperatorId() {
		return operatorId;
	}
	/**
	 * @param operatorId the operatorId to set
	 */
	public void setOperatorId(String operatorId) {
		this.operatorId = operatorId;
	}
	/**
	 * @return the pullInTime
	 */
	public String getPullinTime() {
		return pullinTime;
	}
	/**
	 * @param pullInTime the pullInTime to set
	 */
	public void setPullinTime(String pullinTime) {
		this.pullinTime = pullinTime;
	}
	
}
