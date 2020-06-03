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

package org.onebusaway.nyc.transit_data_manager.adapters.data;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;


public class ImporterVehiclePulloutData implements PulloutData {

	private List<VehiclePullInOutInfo> pulloutData = null;

	public ImporterVehiclePulloutData(List<VehiclePullInOutInfo> pulloutData) {
		this.pulloutData = pulloutData;
	}

	public List<VehiclePullInOutInfo> getAllPullouts() {
		return pulloutData;
	}

	public List<VehiclePullInOutInfo> getPulloutsByBus(Long busNumber) {
		List<VehiclePullInOutInfo> busPullOutInfo = new ArrayList<VehiclePullInOutInfo>();
		
		for(VehiclePullInOutInfo currentPullOut : pulloutData) {
			if(new Long(currentPullOut.getPullOutInfo().getVehicle().getVehicleId()).equals(busNumber)) {
				busPullOutInfo.add(currentPullOut);
			}
		}
		return busPullOutInfo;
	}

	public List<VehiclePullInOutInfo> getPulloutsByDepot(String depotId) {
		List<VehiclePullInOutInfo> depotPullOutInfo = new ArrayList<VehiclePullInOutInfo>();
		
		for(VehiclePullInOutInfo currentPullOut : pulloutData) {
			if(currentPullOut.getPullOutInfo().getGarage().getFacilityName().equals(depotId)) {
				depotPullOutInfo.add(currentPullOut);
			}
		}
		return depotPullOutInfo;
	}

	public List<VehiclePullInOutInfo> getPulloutsByAgency(String agencyId) {
		List<VehiclePullInOutInfo> agencyPullOutInfo = new ArrayList<VehiclePullInOutInfo>();
		
		for(VehiclePullInOutInfo currentPullOut : pulloutData) {
			String agencyDesignator = currentPullOut.getPullOutInfo().getGarage().getAgencydesignator();
			if(agencyDesignator != null && agencyDesignator.equals(agencyId)) {
				agencyPullOutInfo.add(currentPullOut);
			}
		}
		
		return agencyPullOutInfo;
	}
}
