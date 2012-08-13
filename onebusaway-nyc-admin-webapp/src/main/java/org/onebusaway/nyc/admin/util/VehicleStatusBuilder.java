package org.onebusaway.nyc.admin.util;

import java.math.BigDecimal;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.onebusaway.nyc.admin.model.json.VehicleLastKnownRecord;
import org.onebusaway.nyc.admin.model.json.VehiclePullout;
import org.onebusaway.nyc.admin.model.ui.InferredState;
import org.onebusaway.nyc.admin.model.ui.VehicleStatus;

/**
 * Builds vehicle status objects/DTOs in the format required by client
 * @author abelsare
 *
 */
public class VehicleStatusBuilder {

	public VehicleStatus buildVehicleStatus(VehiclePullout pullout, VehicleLastKnownRecord lastknownRecord) {
		VehicleStatus vehicleStatus = new VehicleStatus();
		if(pullout != null) {
			//Set the actual times for filtering
			vehicleStatus.setPullinTime(pullout.getPullinTime());
			vehicleStatus.setPulloutTime(pullout.getPulloutTime());
			//Set formatted time for display
			vehicleStatus.setFormattedPullinTime(getPullinTime(pullout.getPulloutTime(), pullout.getPullinTime()));
			vehicleStatus.setFormattedPulloutTime(extractTime(pullout.getPulloutTime()));
		}
		vehicleStatus.setVehicleId(lastknownRecord.getVehicleId());

		String inferredDestination = getInferredDestination(lastknownRecord);
		vehicleStatus.setInferredDestination(inferredDestination);

		vehicleStatus.setInferredState(getInferredState(lastknownRecord));
		
		vehicleStatus.setObservedDSC(lastknownRecord.getDestinationSignCode());
		
		vehicleStatus.setDetails(lastknownRecord.getVehicleId());
		
		vehicleStatus.setRoute(getRoute(lastknownRecord.getInferredRouteId()));
		
		vehicleStatus.setDepot(lastknownRecord.getDepotId());
		vehicleStatus.setEmergencyStatus(lastknownRecord.getEmergencyCode());
		
		vehicleStatus.setLastUpdate(getLastUpdate(lastknownRecord.getTimeReported()));
		
		vehicleStatus.setStatus(getStatus(lastknownRecord.getInferredPhase(), 
				lastknownRecord.getTimeReported(), lastknownRecord.getEmergencyCode(),
				lastknownRecord.getInferredTripId()));
		
		vehicleStatus.setInferredDSC(lastknownRecord.getInferredDSC());
		
		vehicleStatus.setTimeReported(lastknownRecord.getTimeReported());
		
		vehicleStatus.setInferrenceFormal(lastknownRecord.isInferrenceFormal());
		
		return vehicleStatus;
	}

	private String getStatus(String inferredPhase, String timeReported, String emergencyCode,
			String inferredTripId) {
		String imageSrc = null;
		BigDecimal difference  = getTimeDifference(timeReported);
		
		if((inferredPhase.equals(InferredState.IN_PROGRESS.getState()) || 
				inferredPhase.equals(InferredState.DEADHEAD_DURING.getState()) ||
				inferredPhase.startsWith("LAY")) && (difference.compareTo(new BigDecimal(120)) < 0)) {
			imageSrc = getStatusImage(emergencyCode, "green");
		} else {
			if((StringUtils.isNotBlank(inferredTripId)) && 
					(difference.compareTo(new BigDecimal(120)) >= 0)) {
				imageSrc = getStatusImage(emergencyCode, "orange");
			} 
			else if((StringUtils.isBlank(inferredTripId)) ||
					(difference.compareTo(new BigDecimal(300)) > 0)) {
				imageSrc = getStatusImage(emergencyCode, "red");
			}
		}

		return imageSrc;
	}

	private String getStatusImage(String emergencyCode, String imageColor) {
		String imageSrc;
		if(StringUtils.isNotBlank(emergencyCode)) {
			imageSrc = "circle_" + imageColor + "_alert_18x18.png";
		} else {
			imageSrc = "circle_" + imageColor + "18x18.png";
		}
		return imageSrc;
	}

	private String getInferredState(VehicleLastKnownRecord lastknownRecord) {
		String inferredPhase = lastknownRecord.getInferredPhase();
		String inferredState = inferredPhase;
		if(inferredPhase.startsWith("IN")) {
			inferredState = "IN PROGRESS";
		} else {
			if(inferredPhase.startsWith("DEAD")) {
				inferredState = "DEADHEAD";
			} else {
				if(inferredPhase.startsWith("LAY")) {
					inferredState = "LAYOVER";
				}
			}
		}

		return inferredState;
	}

	private String getInferredDestination(
			VehicleLastKnownRecord lastknownRecord) {
		StringBuilder inferredDestination = new StringBuilder();
		//all these fields can be blank
		if(StringUtils.isNotBlank(lastknownRecord.getInferredDSC())) {
			inferredDestination.append(lastknownRecord.getInferredDSC());
		}
		
		inferredDestination.append(getRoute(lastknownRecord.getInferredRouteId()));
		
		if(StringUtils.isNotBlank(lastknownRecord.getInferredDirectionId())) {
			inferredDestination.append(" Direction: ");
			inferredDestination.append(lastknownRecord.getInferredDirectionId());
		}
		return inferredDestination.toString();
	}
	
	private String getRoute(String inferredRouteId) {
		String route = StringUtils.EMPTY;
		if(StringUtils.isNotBlank(inferredRouteId)) {
			String [] routeArray = inferredRouteId.split("_");
			if(routeArray.length > 1) {
				route = ":" +routeArray[1];
			}
		}
		return route;
	}
	
	private String getPullinTime(String pulloutTime, String pullinTime) {
		StringBuilder pullinTimeBuilder = new StringBuilder(extractTime(pullinTime));
		
		DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis();
		
		DateTime pulloutDateTime = formatter.parseDateTime(pulloutTime);
		int pulloutDay = pulloutDateTime.getDayOfMonth();
		
		DateTime pullinDateTime = formatter.parseDateTime(pullinTime);
		int pullinDay = pullinDateTime.getDayOfMonth();
		
		//Check if pullout time falls on the next day
		if(pulloutDay < pullinDay) {
			pullinTimeBuilder.append(" +1 day");
		}
		
		return pullinTimeBuilder.toString();
	}

	private String extractTime(String date) {
		DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis();
		DateTime dateTime = formatter.parseDateTime(date);
		int hour = dateTime.getHourOfDay();
		String formattedHour = String.format("%02d", hour);
		int minute = dateTime.getMinuteOfHour();
		String formattedMinute = String.format("%02d", minute);
		return formattedHour + ":" +formattedMinute;
	}
	
	private String getLastUpdate(String timeReported) {
		String lastUpdate;
		BigDecimal difference = getTimeDifference(timeReported);
		if(difference.compareTo(new BigDecimal(86400)) > 0) {
			//Calculate the difference in days
			BigDecimal days = difference.divide(new BigDecimal(86400), BigDecimal.ROUND_HALF_UP);
			lastUpdate = days.toPlainString() + " days";
		} else {
			if(difference.compareTo(new BigDecimal(3600)) > 0) {
				//Calculate the difference in hours
				BigDecimal hours = difference.divide(new BigDecimal(3600), BigDecimal.ROUND_HALF_UP);
				lastUpdate = hours.toPlainString() + " hours";
			} else {
				if(difference.compareTo(new BigDecimal(60)) > 0) {
					//Calculate the difference in minutes
					BigDecimal minutes = difference.divide(new BigDecimal(60), BigDecimal.ROUND_UP);
					lastUpdate = minutes.toPlainString() + " mins";
				} else {
					lastUpdate = difference + " sec";
				}
			}
		}
		return lastUpdate;
	}

	private BigDecimal getTimeDifference(String timeReported) {
		DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
		DateTime lastReportedTime = formatter.parseDateTime(timeReported);
		DateTime now = new DateTime();
		int seconds;
		if(lastReportedTime.isAfter(now)) {
			seconds= Seconds.secondsBetween(now, lastReportedTime).getSeconds();
		} else {
			seconds= Seconds.secondsBetween(lastReportedTime, now).getSeconds();
		}
		BigDecimal difference = new BigDecimal(seconds);
		
		return difference;
	}


}
