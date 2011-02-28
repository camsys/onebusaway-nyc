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

import java.text.DateFormat;

import org.onebusaway.nyc.presentation.impl.WebappIdParser;
import org.onebusaway.nyc.presentation.service.ConfigurationBean;
import org.onebusaway.nyc.transit_data.model.NycVehicleStatusBean;
import org.onebusaway.nyc.transit_data.model.UtsRecordBean;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

public class MonitoredVehicleBean implements MonitoredVehicle {
	private static final WebappIdParser idParser = new WebappIdParser();
	
	private NycVehicleStatusBean nycVehicleStatusBean;
	private VehicleStatusBean vehicleStatusBean;
	private ConfigurationBean configuration;
	private VehicleTrackingManagementService vehicleTrackingManagementService;
	private UtsRecordBean utsRecord;

	public MonitoredVehicleBean(NycVehicleStatusBean nycVehicleStatusBean,
			VehicleStatusBean vehicleStatusBean, ConfigurationBean configuration,
			VehicleTrackingManagementService vehicleTrackingManagementService,
			UtsRecordBean utsRecord) {
		
		this.nycVehicleStatusBean = nycVehicleStatusBean;
		this.vehicleStatusBean = vehicleStatusBean;
		this.configuration = configuration;
		this.vehicleTrackingManagementService = vehicleTrackingManagementService;
		this.utsRecord = utsRecord;
	}

	public String getVehicleIdWithoutAgency() {
		String vehicleIdWithAgency = nycVehicleStatusBean.getVehicleId();
		String idWithoutAgency = idParser.parseIdWithoutAgency(vehicleIdWithAgency);
		return idWithoutAgency;
	}

	public String getVehicleId() {
		String vehicleIdWithAgency = nycVehicleStatusBean.getVehicleId();
		return vehicleIdWithAgency;
	}

	public String getStatusClass() {
		long lastUpdateTime = nycVehicleStatusBean.getLastUpdateTime();
		long lastGpsTime = nycVehicleStatusBean.getLastGpsTime();
		long now = System.currentTimeMillis();
		long updateTimeDiff = now - lastUpdateTime;
		long gpsTimeDiff = now - lastGpsTime;

		long staleDataThreshold = configuration.getNoProgressTimeout() * 1000;
		long hideDataThreshold = configuration.getHideTimeout() * 1000;
		long gpsTimeSkewThreshold = configuration.getGpsTimeSkewThreshold() * 1000;

		if(updateTimeDiff < staleDataThreshold 
				&& gpsTimeDiff < staleDataThreshold + gpsTimeSkewThreshold)
			return "status green";
		if(updateTimeDiff < hideDataThreshold
				&& gpsTimeDiff < hideDataThreshold + gpsTimeSkewThreshold)
			return "status orange";
		if(utsRecord == null 
				&& (updateTimeDiff > hideDataThreshold
						|| gpsTimeDiff > hideDataThreshold + gpsTimeSkewThreshold))
			return "status grey";

		return "status red";
	}

	public String getActualPullOut() {
		if(utsRecord == null) {
			return "Unknown";
		} else {
			return DateFormat.getDateInstance().format(utsRecord.getActualPullOut()) + "<br/>" + 
			DateFormat.getTimeInstance().format(utsRecord.getActualPullOut());
		}
	}

	// key for sorting on client-side
	public long getActualPullOutEpoch() {
		if(utsRecord == null) {
			return -1;
		} else {
			return utsRecord.getActualPullOut().getTime();
		}
	}

	public String getScheduledPullIn() {
		if(utsRecord == null) {
			return "Unknown";
		} else {
			return DateFormat.getDateInstance().format(utsRecord.getScheduledPullIn()) + "<br/>" + 
			DateFormat.getTimeInstance().format(utsRecord.getScheduledPullIn());
		}
	}

	// key for sorting on client-side
	public long getScheduledPullInEpoch() {
		if(utsRecord == null) {
			return -1;
		} else {
			return utsRecord.getScheduledPullIn().getTime();
		}
	}

	private String formatTimeInterval(long seconds) {
		if (seconds < 60) {
			return seconds == 1 ? "1 second" : seconds + " seconds";
		} else {    	
			long minutes = seconds / 60;
			long days = minutes / (60 * 24);

			if(days < 1) {
				return minutes == 1 ? "1 minute" : minutes + " minutes";
			} else {
				return days == 1 ? "1 day" : String.format("%1.2f", days) + " days";
			}
		}
	}

	public String getLastUpdateTime() {
		long lastUpdateTime = nycVehicleStatusBean.getLastUpdateTime();

		if(lastUpdateTime <= 0)
			return "Not Available";

		long now = System.currentTimeMillis();
		long timeDiff = now - lastUpdateTime;
		long seconds = timeDiff / 1000;
		return formatTimeInterval(seconds);      
	}

	// key for sorting on client-side
	public long getLastUpdateTimeEpoch() {
		long lastUpdateTime = nycVehicleStatusBean.getLastUpdateTime();

		if(lastUpdateTime <= 0)
			return -1;
		else
			return lastUpdateTime;
	}

	public String getLastCommTime() {
		long lastUpdateTime = nycVehicleStatusBean.getLastGpsTime();

		if(lastUpdateTime <= 0)
			return "Not Available";

		long now = System.currentTimeMillis();
		long timeDiff = now - lastUpdateTime;
		long seconds = timeDiff / 1000;
		return formatTimeInterval(seconds);      
	}

	// key for sorting on client-side
	public long getLastCommTimeEpoch() {
		long lastUpdateTime = nycVehicleStatusBean.getLastGpsTime();

		if(lastUpdateTime <= 0)
			return -1;
		else
			return lastUpdateTime;
	}

	public String getHeadsign() {
		if (vehicleStatusBean == null)
			return "Not Available";

		TripBean trip = vehicleStatusBean.getTrip();
		String mostRecentDestinationSignCode = nycVehicleStatusBean.getMostRecentDestinationSignCode();
		boolean mostRecentDSCIsOutOfService = 
			vehicleTrackingManagementService.isOutOfServiceDestinationSignCode(mostRecentDestinationSignCode);

		if (trip == null) {
			if(mostRecentDSCIsOutOfService)
				return mostRecentDestinationSignCode + ": Not In Service";
			else
				if(mostRecentDestinationSignCode != null) {
					return "Unknown<br/><span class='error'>(bus sent " + mostRecentDestinationSignCode + ")</span>";
				} else {
					return "Unknown";    			  
				}
		}

		if(nycVehicleStatusBean.getPhase().equals(EVehiclePhase.DEADHEAD_AFTER.toLabel()) ||
				nycVehicleStatusBean.getPhase().equals(EVehiclePhase.DEADHEAD_BEFORE.toLabel()) ||
				nycVehicleStatusBean.getPhase().equals(EVehiclePhase.DEADHEAD_DURING.toLabel())) {
			
			return "Not Applicable<br/>(bus sent " + mostRecentDestinationSignCode + ")";    	  
		}

		String tripHeadsign = trip.getTripHeadsign();
		String inferredDestinationSignCode = nycVehicleStatusBean.getInferredDestinationSignCode();
		
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
		if(vehicleStatusBean == null)
			return "Not Available";

		if (!nycVehicleStatusBean.isEnabled())
			return "Disabled";

		TripBean trip = vehicleStatusBean.getTrip();
		if (trip == null)
			return "No Trip";

		String status = nycVehicleStatusBean.getStatus();
		String phase = nycVehicleStatusBean.getPhase();
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
		double lat = nycVehicleStatusBean.getLastGpsLat();
		double lon = nycVehicleStatusBean.getLastGpsLon();

		if(Double.isNaN(lat) || Double.isNaN(lon))
			return "Not Available";
		
		return lat + "," + lon;
	}

	public String getOrientation() {
		if(vehicleStatusBean == null)
			return "Unknown";
		
		TripStatusBean tripStatus = vehicleStatusBean.getTripStatus();
		
		if(tripStatus == null) 
			return "Unknown";
		
		double orientationDeg = tripStatus.getOrientation();

		if(Double.isNaN(orientationDeg))
			return "Unknown";
		else
			return "" + Math.floor(orientationDeg / 5) * 5;
	}    

	public boolean isDisabled() {
		return !nycVehicleStatusBean.isEnabled();
	}
}