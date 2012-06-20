package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.PullInOut;

/**
 * Holds vehicle pipo information
 * @author abelsare
 *
 */
public class VehiclePipoMessage {
	
	private List<PullInOut> pullouts;
	private String status;
	
	/**
	 * @param pullouts the pulloutData to set
	 */
	public void setPullouts(List<PullInOut> pullouts) {
		this.pullouts = pullouts;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}
}
