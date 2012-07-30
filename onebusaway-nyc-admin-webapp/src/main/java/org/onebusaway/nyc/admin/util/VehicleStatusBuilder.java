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
			vehicleStatus.setPullinTime(extractTime(pullout.getPullinTime()));
			vehicleStatus.setPulloutTime(extractTime(pullout.getPulloutTime()));
		}
		vehicleStatus.setVehicleId(lastknownRecord.getVehicleId());

		String inferredDestination = getInferredDestination(lastknownRecord);
		vehicleStatus.setInferredDestination(inferredDestination);

		vehicleStatus.setInferredState(getInferredState(lastknownRecord));
		vehicleStatus.setObservedDSC(lastknownRecord.getDestinationSignCode());
		vehicleStatus.setDetails("Details");
		vehicleStatus.setRoute(getRoute(lastknownRecord.getInferredRunId()));
		vehicleStatus.setDepot(lastknownRecord.getDepotId());
		vehicleStatus.setEmergencyStatus(lastknownRecord.getEmergencyCode());
		vehicleStatus.setLastUpdate(getLastUpdate(lastknownRecord.getTimeReported()));
		vehicleStatus.setStatus(getStatus(lastknownRecord.getInferredPhase(), 
				lastknownRecord.getTimeReported()));
		vehicleStatus.setTimeReceived(lastknownRecord.getTimeReceived());
		return vehicleStatus;
	}

	private String getStatus(String inferredPhase, String timeReported) {
		String imageSrc = null;
		BigDecimal difference  = getTimeDifference(timeReported);
		
		if((inferredPhase.equals(InferredState.IN_PROGRESS.getState()) || 
				inferredPhase.equals(InferredState.DEADHEAD_DURING.getState()) ||
				inferredPhase.startsWith("LAY")) && (difference.intValue() < 120)) {
			imageSrc = "circle_green.png";
		} else {
			if((inferredPhase.equals(InferredState.AT_BASE.getState())) || 
					(difference.intValue() > 300)) {
				imageSrc = "circle_red.png";
			} else {
				imageSrc = "circle_orange.png";
			}
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
			inferredDestination.append(lastknownRecord.getInferredDSC() + ":");
		}
		
		inferredDestination.append(getRoute(lastknownRecord.getInferredRunId()));
		
		if(StringUtils.isNotBlank(lastknownRecord.getInferredDirectionId())) {
			inferredDestination.append(" Direction: ");
			inferredDestination.append(lastknownRecord.getInferredDirectionId());
		}
		return inferredDestination.toString();
	}
	
	private String getRoute(String inferredRunId) {
		String route = StringUtils.EMPTY;
		if(StringUtils.isNotBlank(inferredRunId)) {
			route = inferredRunId.split("-")[0];
		}
		return route;
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
				BigDecimal hours = difference.divide(new BigDecimal(3600), BigDecimal.ROUND_UP);
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
		int seconds = Seconds.secondsBetween(lastReportedTime, now).getSeconds();
		BigDecimal difference = new BigDecimal(seconds);
		return difference;
	}


}
