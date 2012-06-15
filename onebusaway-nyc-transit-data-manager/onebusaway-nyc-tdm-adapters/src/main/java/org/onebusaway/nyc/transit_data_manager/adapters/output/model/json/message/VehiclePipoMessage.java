package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;

/**
 * Holds vehicle pipo information
 * @author abelsare
 *
 */
public class VehiclePipoMessage {
	
	private List<VehiclePullInOutInfo> pulloutData;
	private String status;
	
	/**
	 * @param pulloutData the pulloutData to set
	 */
	public void setPulloutData(List<VehiclePullInOutInfo> pulloutData) {
		this.pulloutData = pulloutData;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}
}
