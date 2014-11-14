package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json;

import tcip_final_4_0_0_0.SCHPullInOutInfo;

/**
 * Wraps {@link SCHPullInOutInfo} instances to store vehicle pull in and pull out info at one place. 
 * @author abelsare
 *
 */
public class VehiclePullInOutInfo {

	private SCHPullInOutInfo pullOutInfo;
	private SCHPullInOutInfo pullInInfo;
	
	/**
	 * @return the pullOutInfo
	 */
	public SCHPullInOutInfo getPullOutInfo() {
		return pullOutInfo;
	}
	/**
	 * @param pullOutInfo the pullOutInfo to set
	 */
	public void setPullOutInfo(SCHPullInOutInfo pullOutInfo) {
		this.pullOutInfo = pullOutInfo;
	}
	/**
	 * @return the pullInInfo
	 */
	public SCHPullInOutInfo getPullInInfo() {
		return pullInInfo;
	}
	/**
	 * @param pullInInfo the pullInInfo to set
	 */
	public void setPullInInfo(SCHPullInOutInfo pullInInfo) {
		this.pullInInfo = pullInInfo;
	}
	
	
}
