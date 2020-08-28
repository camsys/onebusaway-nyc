/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onebusaway.nyc.transit_data_manager.adapters.output.model.json;

import tcip_final_3_0_5_1.SCHPullInOutInfo;

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
