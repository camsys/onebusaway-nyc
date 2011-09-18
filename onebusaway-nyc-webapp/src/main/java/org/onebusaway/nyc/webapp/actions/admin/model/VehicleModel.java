/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.webapp.actions.admin.model;

import org.onebusaway.nyc.transit_data.model.NycVehicleManagementStatusBean;
import org.onebusaway.nyc.transit_data.services.ConfigurationService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

public class VehicleModel {

	private ConfigurationService _configurationService;
	
	private NycVehicleManagementStatusBean _nycVehicleStatusBean;
	
	private VehicleStatusBean _vehicleStatusBean;
	
	public VehicleModel(NycVehicleManagementStatusBean nycVehicleStatusBean, 
						VehicleStatusBean vehicleStatusBean,
						ConfigurationService configurationService) {
		_nycVehicleStatusBean = nycVehicleStatusBean;
		_vehicleStatusBean = vehicleStatusBean;
		_configurationService = configurationService;
	}

	public String getVehicleIdWithoutAgency() {
		return _nycVehicleStatusBean.getVehicleId().split("_")[1];
	}

	public String getVehicleId() {
		// (OGNL doesn't allow form field names with spaces--so we use a placeholder)
		return _nycVehicleStatusBean.getVehicleId().replace(" ", "^");
	}

	public String getStatusClass() {
		long lastUpdateTime = _nycVehicleStatusBean.getLastUpdateTime();
		long lastGpsTime = _nycVehicleStatusBean.getLastLocationUpdateTime();

		long now = System.currentTimeMillis();
		long updateTimeDiff = now - lastUpdateTime;
		long gpsTimeDiff = now - lastGpsTime;
		
		long staleDataThreshold = 
				_configurationService.getConfigurationValueAsInteger("display.staleTimeout", 120) * 1000;
		long hideDataThreshold = 
				_configurationService.getConfigurationValueAsInteger("display.hideTimeout", 300) * 1000;
		long gpsTimeSkewThreshold = 
				_configurationService.getConfigurationValueAsInteger("data.gpsTimeSkewThreshold", 120) * 1000;

		if(updateTimeDiff < staleDataThreshold 
				&& gpsTimeDiff < staleDataThreshold + gpsTimeSkewThreshold)
			return "status green";
		if(updateTimeDiff < hideDataThreshold
				&& gpsTimeDiff < hideDataThreshold + gpsTimeSkewThreshold)
			return "status orange";
//		if(utsRecord == null 
//				&& (updateTimeDiff > hideDataThreshold
//						|| gpsTimeDiff > hideDataThreshold + gpsTimeSkewThreshold))
//			return "status grey";

		return "status red";
	}

	public String getActualPullOut() {
		return "N/A";
	}

	// key for sorting on client-side
	public long getActualPullOutEpoch() {
		return -1;
	}

	public String getScheduledPullIn() {
		return "N/A";
	}

	// key for sorting on client-side
	public long getScheduledPullInEpoch() {
		return -1;
	}

	private String formatTimeInterval(long seconds) {
		if (seconds < 60) {
			return seconds == 1 ? "1 second" : seconds + " seconds";
		} else {    	
			long minutes = seconds / 60;
			float hours = (float)minutes / 60f;
			float days = hours / 24f;

			if(hours < 1) {
				return minutes == 1 ? "1 minute" : minutes + " minutes";
			} else if(days < 1) {
				return hours == 1 ? "1 hour" : String.format("%.1f", hours) + " hours";
			} else {
				return days == 1 ? "1 day" : String.format("%.1f", days) + " days";
			}
		}
	}

	public String getLastUpdateTime() {
		long lastUpdateTime = _nycVehicleStatusBean.getLastUpdateTime();

		if(lastUpdateTime <= 0)
			return "Not Available";

		long now = System.currentTimeMillis();

		long timeDiff = now - lastUpdateTime;
		long seconds = timeDiff / 1000;
		
		return formatTimeInterval(seconds);    
	}

	// key for sorting on client-side
	public long getLastUpdateTimeEpoch() {
		long lastUpdateTime = _nycVehicleStatusBean.getLastUpdateTime();

		if(lastUpdateTime <= 0)
			return -1;
		else
			return lastUpdateTime;
	}

	public String getLastCommTime() {
		long lastUpdateTime = _nycVehicleStatusBean.getLastLocationUpdateTime();

		if(lastUpdateTime <= 0)
			return "Not Available";

		long now = System.currentTimeMillis();
		long timeDiff = now - lastUpdateTime;
		long seconds = timeDiff / 1000;
		
		return formatTimeInterval(seconds); 
	}

	// key for sorting on client-side
	public long getLastCommTimeEpoch() {
		long lastUpdateTime = _nycVehicleStatusBean.getLastLocationUpdateTime();

		if(lastUpdateTime <= 0)
			return -1;
		else
			return lastUpdateTime;
	}

	public String getHeadsign() {
		if (_vehicleStatusBean == null)
			return "Not Available";

		TripBean trip = _vehicleStatusBean.getTrip();

		String mostRecentDestinationSignCode = 
				_nycVehicleStatusBean.getMostRecentObservedDestinationSignCode();

// FIXME
//		boolean mostRecentDSCIsOutOfService = 
//				_vehicleTrackingManagementService.isOutOfServiceDestinationSignCode(mostRecentDestinationSignCode);

		if (trip == null) {
//			if(mostRecentDSCIsOutOfService)
//				return mostRecentDestinationSignCode + ": Not In Service";
//			else
				if(mostRecentDestinationSignCode != null) {
					return "Unknown<br/><span class='error'>(bus sent " + mostRecentDestinationSignCode + ")</span>";
				} else {
					return "Unknown";    			  
				}
		}

		if(_vehicleStatusBean.getPhase().equals(EVehiclePhase.DEADHEAD_AFTER.toLabel()) ||
				_vehicleStatusBean.getPhase().equals(EVehiclePhase.DEADHEAD_BEFORE.toLabel()) ||
				_vehicleStatusBean.getPhase().equals(EVehiclePhase.DEADHEAD_DURING.toLabel())) {

			return "Not Applicable<br/>(bus sent " + mostRecentDestinationSignCode + ")";    	  
		}

		String tripHeadsign = trip.getTripHeadsign();
		String inferredDestinationSignCode = _nycVehicleStatusBean.getLastInferredDestinationSignCode();

		if (inferredDestinationSignCode.equals(mostRecentDestinationSignCode)) {
			return inferredDestinationSignCode + ": " + tripHeadsign;
		} else {
			return inferredDestinationSignCode + ": " + tripHeadsign
			+ "<br/><span class='error'>(bus sent " + mostRecentDestinationSignCode + ")</span>";
		}
	}

	private String upperCaseWords(String s) {
		StringBuilder result = new StringBuilder(s.length());
		String[] words = s.split("\\s|_");
		for(int i=0,l=words.length;i<l;++i) {
			if(i>0) result.append(" ");      
			result.append(Character.toUpperCase(words[i].charAt(0)))
			.append(words[i].substring(1));
		}
		return result.toString();
	}

	public String getInferredState() {
		if(_vehicleStatusBean == null)
			return "Not Available";

		if (!_nycVehicleStatusBean.isInferenceIsEnabled())
			return "Disabled";

		TripBean trip = _vehicleStatusBean.getTrip();
		if (trip == null)
			return "No Trip";

		String status = _vehicleStatusBean.getStatus();
		String phase = _vehicleStatusBean.getPhase();
		StringBuilder sb = new StringBuilder();

		if(phase != null)
			sb.append("Phase: " + this.upperCaseWords(phase));
		else
			sb.append("Phase: None");

		if(sb.length() > 0)
			sb.append("<br/>");

		if(status != null)
			sb.append("Status: " + this.upperCaseWords(status));
		else
			sb.append("Status: None");

		return sb.toString();
	}

	public String getLocation() {
		double lat = _nycVehicleStatusBean.getLastObservedLatitude();
		double lon = _nycVehicleStatusBean.getLastObservedLongitude();

		if(Double.isNaN(lat) || Double.isNaN(lon))
			return "Not Available";

		return lat + "," + lon;
	}

	public String getOrientation() {
		if(_vehicleStatusBean == null)
			return "Unknown";

		TripStatusBean tripStatus = _vehicleStatusBean.getTripStatus();
		if(tripStatus == null) 
			return "Unknown";

		double orientationDeg = tripStatus.getOrientation();
		if(Double.isNaN(orientationDeg))
			return "Unknown";
		else
			return "" + Math.floor(orientationDeg / 5) * 5;		
	}    

	public boolean isDisabled() {
		return !_nycVehicleStatusBean.isInferenceIsEnabled();
	}
}