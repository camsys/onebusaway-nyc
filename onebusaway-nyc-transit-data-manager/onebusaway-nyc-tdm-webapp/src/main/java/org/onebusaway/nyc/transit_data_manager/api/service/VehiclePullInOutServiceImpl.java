package org.onebusaway.nyc.transit_data_manager.api.service;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.transit_data_manager.adapters.output.model.json.VehiclePullInOutInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tcip_final_3_0_5_1.SCHPullInOutInfo;

/**
 * Default implementation of {@link VehiclePullInOutService}
 * @author abelsare
 *
 */
public class VehiclePullInOutServiceImpl implements VehiclePullInOutService{

	private Logger log = LoggerFactory.getLogger(VehiclePullInOutServiceImpl.class);
	
	@Override
	public List<VehiclePullInOutInfo> getActivePullOuts(
			List<VehiclePullInOutInfo> allPullouts) {
		List<VehiclePullInOutInfo> activePullouts = new ArrayList<VehiclePullInOutInfo>();
		//Iterate over all pull in/out data to look for active pull outs. An active pull out 
		//has pull out time less than now and pull in time greater than now
		for(VehiclePullInOutInfo currentPullout : allPullouts) {
			SCHPullInOutInfo pullOutInfo = currentPullout.getPullOutInfo();
			SCHPullInOutInfo pullInInfo = currentPullout.getPullInInfo();
			if(isActive(pullOutInfo.getTime(),pullInInfo.getTime())) {
				activePullouts.add(currentPullout);
			}
		}
		return activePullouts;
	}

	@Override
	public VehiclePullInOutInfo getMostRecentActivePullout(
			List<VehiclePullInOutInfo> activePullouts) {
		DateTimeFormatter format = ISODateTimeFormat.dateTimeNoMillis();
		VehiclePullInOutInfo mostRecentActivePullout;

		if(activePullouts.isEmpty()) {
			mostRecentActivePullout = null;
			log.debug("Call to getMostRecentActivePullout with empty list");
		} else {
			mostRecentActivePullout = activePullouts.get(0);
			//Loop thhrough active pull out list to get pull out with the latest time
			for(VehiclePullInOutInfo currentActivePullout : activePullouts) {
				DateTime currentActivepullOutTime = format.parseDateTime(
						currentActivePullout.getPullOutInfo().getTime());
				DateTime mostRecentActivepullOutTime = format.parseDateTime(
						mostRecentActivePullout.getPullOutInfo().getTime());
				if(currentActivepullOutTime.isAfter(mostRecentActivepullOutTime)) {
					mostRecentActivePullout = currentActivePullout;
				}
			}
		}
		return mostRecentActivePullout;
	}
	
	private boolean isActive(String pullOuttimeString, String pullIntimeString) {
		DateTimeFormatter format = ISODateTimeFormat.dateTimeNoMillis();
		DateTime pullOutTime = format.parseDateTime(pullOuttimeString);
		DateTime pullInTime = format.parseDateTime(pullIntimeString);
		boolean activePullOut = pullOutTime.isBeforeNow() && pullInTime.isAfterNow();
		return activePullOut;
	}

}
