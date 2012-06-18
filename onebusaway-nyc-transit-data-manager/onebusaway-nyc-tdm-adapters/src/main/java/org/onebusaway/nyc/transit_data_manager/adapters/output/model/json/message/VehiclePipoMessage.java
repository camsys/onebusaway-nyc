package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.message;

import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;

import tcip_final_3_0_5_1.SCHPullInOutInfo;

/**
 * Holds vehicle pipo information
 * @author abelsare
 *
 */
public class VehiclePipoMessage {
	
	private List<SCHPullInOutInfo> pulloutData;
	private String status;
	
	/**
	 * @param pulloutData the pulloutData to set
	 */
	public void setPulloutData(List<SCHPullInOutInfo> pulloutData) {
		this.pulloutData = pulloutData;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}
}
