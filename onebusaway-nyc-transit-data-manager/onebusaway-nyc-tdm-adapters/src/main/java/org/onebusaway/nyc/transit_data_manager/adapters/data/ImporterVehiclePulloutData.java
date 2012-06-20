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
			if(currentPullOut.getPullOutInfo().getVehicle().getVehicleId() == busNumber.longValue()) {
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
}
