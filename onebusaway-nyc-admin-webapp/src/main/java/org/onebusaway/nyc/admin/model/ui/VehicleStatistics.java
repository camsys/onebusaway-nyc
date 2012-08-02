package org.onebusaway.nyc.admin.model.ui;

/**
 * Holds real time statistics for the vehicles fetched from operational api 
 * @author abelsare
 *
 */
public class VehicleStatistics {

	private int vehiclesInEmergency;
	private int vehiclesInRevenueService;
	private int vehiclesTracked;
	private int activeRuns;
	/**
	 * @return the vehiclesInEmergency
	 */
	public int getVehiclesInEmergency() {
		return vehiclesInEmergency;
	}
	/**
	 * @param vehiclesInEmergency the vehiclesInEmergency to set
	 */
	public void setVehiclesInEmergency(int vehiclesInEmergency) {
		this.vehiclesInEmergency = vehiclesInEmergency;
	}
	/**
	 * @return the vehiclesInRevenueService
	 */
	public int getVehiclesInRevenueService() {
		return vehiclesInRevenueService;
	}
	/**
	 * @param vehiclesInRevenueService the vehiclesInRevenueService to set
	 */
	public void setVehiclesInRevenueService(int vehiclesInRevenueService) {
		this.vehiclesInRevenueService = vehiclesInRevenueService;
	}
	/**
	 * @return the vehiclesTracked
	 */
	public int getVehiclesTracked() {
		return vehiclesTracked;
	}
	/**
	 * @param vehiclesTracked the vehiclesTracked to set
	 */
	public void setVehiclesTracked(int vehiclesTracked) {
		this.vehiclesTracked = vehiclesTracked;
	}
	/**
	 * @return the activeRuns
	 */
	public int getActiveRuns() {
		return activeRuns;
	}
	/**
	 * @param activeRuns the activeRuns to set
	 */
	public void setActiveRuns(int activeRuns) {
		this.activeRuns = activeRuns;
	}
	
}
