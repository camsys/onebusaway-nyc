/**
 * Copyright (C) 2010 OpenPlans
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

package org.onebusaway.nyc.presentation.impl.realtime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.*;

import org.onebusaway.nyc.presentation.impl.realtime.siri.OnwardCallsMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.commons.lang.StringUtils;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.impl.AgencySupportLibrary;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.siri.support.SiriApcExtension;
import org.onebusaway.nyc.siri.support.SiriDistanceExtension;
import org.onebusaway.nyc.siri.support.SiriExtensionWrapper;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.realtime.api.OccupancyStatus;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.BlockStopTimeBean;
import org.onebusaway.transit_data.model.blocks.BlockTripBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;

import uk.org.siri.siri.BlockRefStructure;
import uk.org.siri.siri.DataFrameRefStructure;
import uk.org.siri.siri.DestinationRefStructure;
import uk.org.siri.siri.DirectionRefStructure;
import uk.org.siri.siri.ExtensionsStructure;
import uk.org.siri.siri.FramedVehicleJourneyRefStructure;
import uk.org.siri.siri.JourneyPatternRefStructure;
import uk.org.siri.siri.JourneyPlaceRefStructure;
import uk.org.siri.siri.LineRefStructure;
import uk.org.siri.siri.LocationStructure;
import uk.org.siri.siri.MonitoredCallStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.NaturalLanguageStringStructure;
import uk.org.siri.siri.OnwardCallStructure;
import uk.org.siri.siri.OnwardCallsStructure;
import uk.org.siri.siri.OperatorRefStructure;
import uk.org.siri.siri.ProgressRateEnumeration;
import uk.org.siri.siri.SituationRefStructure;
import uk.org.siri.siri.SituationSimpleRefStructure;
import uk.org.siri.siri.StopPointRefStructure;
import uk.org.siri.siri.VehicleRefStructure;
import uk.org.siri.siri.OccupancyEnumeration;

public final class SiriSupport {

	private ConfigurationService _configurationService;

	private NycTransitDataService _nycTransitDataService;

	private PresentationService _presentationService;

	public SiriSupport(ConfigurationService configurationService, NycTransitDataService nycTransitDataService, PresentationService presentationService){
		_configurationService = configurationService;
		_nycTransitDataService = nycTransitDataService;
		_presentationService = presentationService;
	}

	private static Logger _log = LoggerFactory.getLogger(SiriSupport.class);


	/*public enum OnwardCallsMode {
		VEHICLE_MONITORING,
		STOP_MONITORING
	}*/


	/**
	 * NOTE: The tripDetails bean here may not be for the trip the vehicle is currently on
	 * in the case of A-D for stop!
	 */
	public void fillMonitoredVehicleJourney(MonitoredVehicleJourneyStructure monitoredVehicleJourney,
											TripBean framedJourneyTripBean, TripStatusBean currentVehicleTripStatus,
											StopBean monitoredCallStopBean,
											Map<String, SiriSupportPredictionTimepointRecord> stopIdToPredictionRecordMap,
											OnwardCallsMode onwardCallsMode, PresentationService presentationService,
											NycTransitDataService nycTransitDataService, int maximumOnwardCalls,
											long responseTimestamp, boolean showApc, boolean showRawApc) {

		// Get Block Instance For Current Vehicle Trip
		BlockInstanceBean blockInstance = nycTransitDataService
				.getBlockInstance(currentVehicleTripStatus.getActiveTrip()
						.getBlockId(), currentVehicleTripStatus
						.getServiceDate());

		// Get All trips associated with Block
		List<BlockTripBean> blockTrips = blockInstance.getBlockConfiguration().getTrips();
		List<BlockStopTimeBean> blockTripStopTimes = getBlockTripStopTimes(blockTrips, framedJourneyTripBean.getId());

		List<String> progressStatuses = getProgressStatuses(currentVehicleTripStatus, framedJourneyTripBean);

		// Get Next Upcoming Stop if one is not provided
		if(monitoredCallStopBean == null) {
			monitoredCallStopBean = currentVehicleTripStatus.getNextStop();
		}

		monitoredVehicleJourney.setLineRef(getLineRef(framedJourneyTripBean));
		monitoredVehicleJourney.setOperatorRef(getOperatorRef(framedJourneyTripBean));
		monitoredVehicleJourney.setDirectionRef(getDirectionRef(framedJourneyTripBean));
		monitoredVehicleJourney.setPublishedLineName(getRouteShortName(framedJourneyTripBean));
		monitoredVehicleJourney.setJourneyPatternRef(getShapeId(framedJourneyTripBean));
		monitoredVehicleJourney.setDestinationName(getHeadsign(framedJourneyTripBean));
		monitoredVehicleJourney.setVehicleRef(getVehicleRef(currentVehicleTripStatus));
		monitoredVehicleJourney.setMonitored(currentVehicleTripStatus.isPredicted());
		monitoredVehicleJourney.setBearing((float)currentVehicleTripStatus.getOrientation());

		monitoredVehicleJourney.setProgressRate(getProgressRateForPhaseAndStatus(
				currentVehicleTripStatus.getStatus(), currentVehicleTripStatus.getPhase()));

		// Set Origin and Destination
		if(!blockTripStopTimes.isEmpty()){
			monitoredVehicleJourney.setOriginRef(getOrigin(blockTripStopTimes));
			monitoredVehicleJourney.setDestinationRef(getDestination(blockTripStopTimes));
		}

		// Show Occupancy Enumeration eg. FEW_SEATS_AVAILABLE
		if(displayEnumeratedOccupancy(showApc, currentVehicleTripStatus)) {
			monitoredVehicleJourney.setOccupancy(getOccupancyEnumeration(currentVehicleTripStatus));
		}

		// Framed Journey
		monitoredVehicleJourney.setFramedVehicleJourneyRef(getFramedJourney(currentVehicleTripStatus, framedJourneyTripBean));

		// Location
		monitoredVehicleJourney.setVehicleLocation(getLocation(currentVehicleTripStatus));

		// Progress Status
		if(!progressStatuses.isEmpty()) {
			monitoredVehicleJourney.setProgressStatus(getProgressStatus(progressStatuses));
		}

		// Block Ref - Checks to see if current vehicle is matched to a Block first before matching
		if (presentationService.hasFormalBlockLevelMatch(currentVehicleTripStatus)) {
			monitoredVehicleJourney.setBlockRef(getBlockRef(framedJourneyTripBean));
		}

		// scheduled depature time
		if (presentationService.hasFormalBlockLevelMatch(currentVehicleTripStatus)
				&& (presentationService.isInLayover(currentVehicleTripStatus)
				|| !framedJourneyTripBean.getId().equals(currentVehicleTripStatus.getActiveTrip().getId()))) {

			BlockStopTimeBean originDepartureStopTime = getOriginDepartureStopTime(blockTrips, currentVehicleTripStatus, progressStatuses);

			if(originDepartureStopTime != null) {
				Date departureTime = new Date(currentVehicleTripStatus.getServiceDate() + (originDepartureStopTime.getStopTime().getDepartureTime() * 1000));
				monitoredVehicleJourney.setOriginAimedDepartureTime(departureTime);
			}
		}

		// monitored call
		if(!presentationService.isOnDetour(currentVehicleTripStatus)) {
			fillMonitoredCall(monitoredVehicleJourney, blockInstance, framedJourneyTripBean, currentVehicleTripStatus, monitoredCallStopBean,
					presentationService, nycTransitDataService, stopIdToPredictionRecordMap, showApc, showRawApc, responseTimestamp);
		}

		// onward calls
		if(!presentationService.isOnDetour(currentVehicleTripStatus))
			fillOnwardCalls(monitoredVehicleJourney, blockInstance, framedJourneyTripBean, currentVehicleTripStatus, onwardCallsMode,
					presentationService, nycTransitDataService, stopIdToPredictionRecordMap, maximumOnwardCalls, responseTimestamp);

		// situations
		fillSituations(monitoredVehicleJourney, currentVehicleTripStatus);

		if(monitoredVehicleJourney.getOccupancy() != null){
			_log.debug("Has OCCUPANCY: " + monitoredVehicleJourney.getVehicleRef().getValue());
		}

		return;
	}


	private List<BlockStopTimeBean> getBlockTripStopTimes(List<BlockTripBean> blockTrips, String tripId){
		for(BlockTripBean blockTrip : blockTrips){
			if(blockTrip.getTrip().getId().equals(tripId)) {
				return blockTrip.getBlockStopTimes();
			}
		}
		return Collections.EMPTY_LIST;
	}

	private LineRefStructure getLineRef(TripBean framedJourneyTripBean){
		LineRefStructure lineRef = new LineRefStructure();
		lineRef.setValue(framedJourneyTripBean.getRoute().getId());
		return lineRef;
	}

	private OperatorRefStructure getOperatorRef(TripBean framedJourneyTripBean){
		OperatorRefStructure operatorRef = new OperatorRefStructure();
		operatorRef.setValue(AgencySupportLibrary.getAgencyForId(framedJourneyTripBean.getRoute().getId()));
		return operatorRef;
	}

	private DirectionRefStructure getDirectionRef(TripBean framedJourneyTripBean){
		DirectionRefStructure directionRef = new DirectionRefStructure();
		directionRef.setValue(framedJourneyTripBean.getDirectionId());
		return directionRef;
	}

	private NaturalLanguageStringStructure getRouteShortName(TripBean framedJourneyTripBean){
		NaturalLanguageStringStructure routeShortName = new NaturalLanguageStringStructure();
		routeShortName.setValue(framedJourneyTripBean.getRoute().getShortName());
		return routeShortName;
	}

	private JourneyPatternRefStructure getShapeId(TripBean framedJourneyTripBean){
		JourneyPatternRefStructure journeyPattern = new JourneyPatternRefStructure();
		journeyPattern.setValue(framedJourneyTripBean.getShapeId());
		return journeyPattern;
	}

	private NaturalLanguageStringStructure getHeadsign(TripBean framedJourneyTripBean){
		NaturalLanguageStringStructure headsign = new NaturalLanguageStringStructure();
		headsign.setValue(framedJourneyTripBean.getTripHeadsign());
		return headsign;
	}

	private VehicleRefStructure getVehicleRef(TripStatusBean currentVehicleTripStatus){
		VehicleRefStructure vehicleRef = new VehicleRefStructure();
		vehicleRef.setValue(currentVehicleTripStatus.getVehicleId());
		return vehicleRef;
	}

	private boolean displayEnumeratedOccupancy(boolean showApc, TripStatusBean currentVehicleTripStatus){
		if (!showApc
				||currentVehicleTripStatus == null
				|| currentVehicleTripStatus.getActiveTrip() == null
				|| currentVehicleTripStatus.getActiveTrip().getRoute() ==  null) {
			return false;
		}
		return true;
	}

	private JourneyPlaceRefStructure getOrigin(List<BlockStopTimeBean> blockTripStopTimes) {
		if(blockTripStopTimes != null && !blockTripStopTimes.isEmpty()) {
			JourneyPlaceRefStructure origin = new JourneyPlaceRefStructure();
			origin.setValue(blockTripStopTimes.get(0).getStopTime().getStop().getId());
			return origin;
		}
		return null;
	}

	private DestinationRefStructure getDestination(List<BlockStopTimeBean> blockTripStopTimes) {
		if(blockTripStopTimes != null && blockTripStopTimes.size() > 1){
			DestinationRefStructure dest = new DestinationRefStructure();
			StopBean lastStop = blockTripStopTimes.get(blockTripStopTimes.size() - 1).getStopTime().getStop();
			dest.setValue(lastStop.getId());
		}
		return null;
	}

	private OccupancyEnumeration getOccupancyEnumeration(TripStatusBean currentVehicleTripStatus){
		VehicleOccupancyRecord vor =
				_nycTransitDataService.getVehicleOccupancyRecordForVehicleIdAndRoute(
						AgencyAndId.convertFromString(currentVehicleTripStatus.getVehicleId()),
						currentVehicleTripStatus.getActiveTrip().getRoute().getId(),
						currentVehicleTripStatus.getActiveTrip().getDirectionId());
		return mapOccupancyStatusToEnumeration(vor);
	}

	private FramedVehicleJourneyRefStructure getFramedJourney(TripStatusBean currentVehicleTripStatus, TripBean framedJourneyTripBean) {
		FramedVehicleJourneyRefStructure framedJourney = new FramedVehicleJourneyRefStructure();
		DataFrameRefStructure dataFrame = new DataFrameRefStructure();
		dataFrame.setValue(String.format("%1$tY-%1$tm-%1$td", currentVehicleTripStatus.getServiceDate()));
		framedJourney.setDataFrameRef(dataFrame);
		framedJourney.setDatedVehicleJourneyRef(framedJourneyTripBean.getId());
		return framedJourney;
	}

	private LocationStructure getLocation(TripStatusBean currentVehicleTripStatus) {
		LocationStructure location = new LocationStructure();

		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(6);

		if (_presentationService.isOnDetour(currentVehicleTripStatus)) {
			location.setLatitude(new BigDecimal(df.format(currentVehicleTripStatus.getLastKnownLocation().getLat())));
			location.setLongitude(new BigDecimal(df.format(currentVehicleTripStatus.getLastKnownLocation().getLon())));
		} else {
			location.setLatitude(new BigDecimal(df.format(currentVehicleTripStatus.getLocation().getLat())));
			location.setLongitude(new BigDecimal(df.format(currentVehicleTripStatus.getLocation().getLon())));
		}

		return location;
	}

	private List<String> getProgressStatuses(TripStatusBean currentVehicleTripStatus, TripBean framedJourneyTripBean) {
		List<String> progressStatuses = new ArrayList<String>();

		if (_presentationService.isInLayover(currentVehicleTripStatus)) {
			progressStatuses.add("layover");
		}

		if (_presentationService.isSpooking(currentVehicleTripStatus)) {
			progressStatuses.add("spooking");
		}

		// "prevTrip" really means not on the framedvehiclejourney trip
		// Indicates that this trip is in the future and not currently active
		if(!framedJourneyTripBean.getId().equals(currentVehicleTripStatus.getActiveTrip().getId())) {
			progressStatuses.add("prevTrip");
		}

		return progressStatuses;
	}

	private BlockRefStructure getBlockRef(TripBean framedJourneyTripBean) {
		BlockRefStructure blockRef = new BlockRefStructure();
		blockRef.setValue(framedJourneyTripBean.getBlockId());
		return blockRef;
	}

	private NaturalLanguageStringStructure getProgressStatus(List<String> progressStatuses) {
		NaturalLanguageStringStructure progressStatus = new NaturalLanguageStringStructure();
		progressStatus.setValue(StringUtils.join(progressStatuses, ","));
		return progressStatus;
	}

	private BlockStopTimeBean getOriginDepartureStopTime(List<BlockTripBean> blockTrips,
														 TripStatusBean currentVehicleTripStatus,
														 List<String> progressStatuses) {
		for(int t = 0; t < blockTrips.size(); t++) {
			BlockTripBean thisTrip = blockTrips.get(t);
			BlockTripBean nextTrip = null;
			if(t + 1 < blockTrips.size()) {
				nextTrip = blockTrips.get(t + 1);
			}

			if(thisTrip.getTrip().getId().equals(currentVehicleTripStatus.getActiveTrip().getId())) {
				// just started new trip
				if(currentVehicleTripStatus.getDistanceAlongTrip() < (0.5 * currentVehicleTripStatus.getTotalDistanceAlongTrip())
						&& !progressStatuses.contains("prevTrip")) {

					return thisTrip.getBlockStopTimes().get(0);

					// at end of previous trip
				} else {
					if(nextTrip != null) {
						int blockStopTimesLastIndex = thisTrip.getBlockStopTimes().size() - 1;
						BlockStopTimeBean currentTripFinalStopTime = thisTrip.getBlockStopTimes().get(blockStopTimesLastIndex);

						int currentTripLastStopArrivalTime = currentTripFinalStopTime.getStopTime().getArrivalTime();
						int nextTripFirstStopDepartureTime = nextTrip.getBlockStopTimes().get(0).getStopTime().getDepartureTime();

						if(nextTripFirstStopDepartureTime - currentTripLastStopArrivalTime > 60){
							return nextTrip.getBlockStopTimes().get(0);
						}
					}
				}

				break;
			}
		}
		return null;
	}

	/***
	 * PRIVATE NoLongerStatic METHODS
	 */
	public void fillOnwardCalls(MonitoredVehicleJourneyStructure monitoredVehicleJourney,
								BlockInstanceBean blockInstance, TripBean framedJourneyTripBean, TripStatusBean currentVehicleTripStatus, OnwardCallsMode onwardCallsMode,
								PresentationService presentationService, NycTransitDataService nycTransitDataService,
								Map<String, SiriSupportPredictionTimepointRecord> stopLevelPredictions, int maximumOnwardCalls, long responseTimestamp) {

		String tripIdOfMonitoredCall = framedJourneyTripBean.getId();

		monitoredVehicleJourney.setOnwardCalls(new OnwardCallsStructure());

		//////////

		// no need to go further if this is the case!
		if(maximumOnwardCalls == 0) {
			return;
		}

		List<BlockTripBean> blockTrips = blockInstance.getBlockConfiguration().getTrips();

		double distanceOfVehicleAlongBlock = 0;
		int blockTripStopsAfterTheVehicle = 0;
		int onwardCallsAdded = 0;

		boolean foundActiveTrip = false;
		for(int i = 0; i < blockTrips.size(); i++) {
			BlockTripBean blockTrip = blockTrips.get(i);

			if(!foundActiveTrip) {
				if(currentVehicleTripStatus.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
					distanceOfVehicleAlongBlock += currentVehicleTripStatus.getDistanceAlongTrip();
					foundActiveTrip = true;
				} else {
					// a block trip's distance along block is the *beginning* of that block trip along the block
					// so to get the size of this one, we have to look at the next.
					if(i + 1 < blockTrips.size()) {
						distanceOfVehicleAlongBlock = blockTrips.get(i + 1).getDistanceAlongBlock();
					}

					// bus has already served this trip, so no need to go further
					continue;
				}
			}

			if(onwardCallsMode == OnwardCallsMode.STOP_MONITORING) {
				// always include onward calls for the trip the monitored call is on ONLY.
				if(!blockTrip.getTrip().getId().equals(tripIdOfMonitoredCall)) {
					continue;
				}
			}

			for(BlockStopTimeBean stopTime : blockTrip.getBlockStopTimes()) {
				// check for non-revenue stops for onward calls
				if(currentVehicleTripStatus.getActiveTrip().getRoute() != null) {
					String agencyId = currentVehicleTripStatus.getActiveTrip().getRoute().getAgency().getId();
					String routeId = currentVehicleTripStatus.getActiveTrip().getRoute().getId();
					String directionId = currentVehicleTripStatus.getActiveTrip().getDirectionId();
					String stopId  = stopTime.getStopTime().getStop().getId();
					if (!nycTransitDataService.stopHasRevenueServiceOnRoute(agencyId, stopId, routeId, directionId)){
						continue;
					}
				}

				// block trip stops away--on this trip, only after we've passed the stop,
				// on future trips, count always.
				if(currentVehicleTripStatus.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
					if(stopTime.getDistanceAlongBlock() >= distanceOfVehicleAlongBlock) {
						blockTripStopsAfterTheVehicle++;
					} else {
						// stop is behind the bus--no need to go further
						continue;
					}

					// future trip--bus hasn't reached this trip yet, so count all stops
				} else {
					blockTripStopsAfterTheVehicle++;
				}

				SiriSupportPredictionTimepointRecord ssptr = new SiriSupportPredictionTimepointRecord();
				String stopPredictionKey = ssptr.getKey(blockTrip.getTrip().getId(), stopTime.getStopTime().getStop().getId());
				monitoredVehicleJourney.getOnwardCalls().getOnwardCall().add(
						getOnwardCallStructure(stopTime.getStopTime().getStop(), presentationService,
								stopTime.getDistanceAlongBlock() - blockTrip.getDistanceAlongBlock(),
								stopTime.getDistanceAlongBlock() - distanceOfVehicleAlongBlock,
								blockTripStopsAfterTheVehicle - 1,
								stopLevelPredictions.get(stopPredictionKey),
								responseTimestamp,
								stopTime.getStopTime().getArrivalTime()));

				onwardCallsAdded++;

				if(onwardCallsAdded >= maximumOnwardCalls) {
					return;
				}
			}

			// if we get here, we added our stops
			return;
		}

	}

	private void fillMonitoredCall(MonitoredVehicleJourneyStructure monitoredVehicleJourney,
								   BlockInstanceBean blockInstance, TripBean arrivalDepartureTrip,
								   TripStatusBean tripStatus, StopBean monitoredCallStopBean,
								   PresentationService presentationService, NycTransitDataService nycTransitDataService,
								   Map<String, SiriSupportPredictionTimepointRecord> stopLevelPredictions, boolean showApc,
								   boolean showRawApc, long responseTimestamp) {

		List<BlockTripBean> blockTrips = blockInstance.getBlockConfiguration().getTrips();

		double distanceOfVehicleAlongBlock = 0;
		int blockTripStopsAfterTheVehicle = 0;

		boolean foundActiveTrip = false;
		boolean foundArrivalDepartureTrip = false;

		for(int i = 0; i < blockTrips.size(); i++) {
			BlockTripBean blockTrip = blockTrips.get(i);
			// Get DistanceAlongBlock for current active trip on block
			if(foundActiveTrip != true){
				if(tripStatus.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
					distanceOfVehicleAlongBlock = blockTrip.getDistanceAlongBlock()  + tripStatus.getDistanceAlongTrip();
					foundActiveTrip = true;
				}
				else {
					continue;
				}
			}
			// Skip trips that don't match arrival departure (including active trip)
			if(foundActiveTrip && !foundArrivalDepartureTrip) {
				if(arrivalDepartureTrip.getId().equals(blockTrip.getTrip().getId())) {
					foundArrivalDepartureTrip = true;
					// If not active trip, don't show apc
					if(!tripStatus.getActiveTrip().getId().equals(blockTrip.getTrip().getId())) {
						showApc = false;
						showRawApc = false;
					}
				} else {
					// bus has already served this trip, so no need to go further
					continue;
				}
			}

			HashMap<String, Integer> visitNumberForStopMap = new HashMap<String, Integer>();

			// Loop through all stop times until we reach the one for monitored call
			for(BlockStopTimeBean stopTime : blockTrip.getBlockStopTimes()) {
				int visitNumber = getVisitNumber(visitNumberForStopMap, stopTime.getStopTime().getStop());

				// block trip stops away--on this trip, only after we've passed the stop,
				// on future trips, count always.
				if(arrivalDepartureTrip.getId().equals(blockTrip.getTrip().getId())) {
					if(stopTime.getDistanceAlongBlock() >= distanceOfVehicleAlongBlock) {
						blockTripStopsAfterTheVehicle++;
					} else {
						// bus has passed this stop already--no need to go further
						continue;
					}

					// future trip--bus hasn't reached this trip yet, so count all stops
				} else {
					blockTripStopsAfterTheVehicle++;
				}

				// monitored call
				if(stopTime.getStopTime().getStop().getId().equals(monitoredCallStopBean.getId())) {
					if(!presentationService.isOnDetour(tripStatus)) {
						SiriSupportPredictionTimepointRecord ssptr = new SiriSupportPredictionTimepointRecord();
						String stopPredictionKey = ssptr.getKey(blockTrip.getTrip().getId(), stopTime.getStopTime().getStop().getId());
						monitoredVehicleJourney.setMonitoredCall(
								getMonitoredCallStructure(
										stopTime.getStopTime().getStop(),
										nycTransitDataService,
										presentationService,
										stopTime.getDistanceAlongBlock() - blockTrip.getDistanceAlongBlock(),
										stopTime.getDistanceAlongBlock() - distanceOfVehicleAlongBlock,
										visitNumber,
										blockTripStopsAfterTheVehicle - 1,
										stopLevelPredictions.get(stopPredictionKey),
										tripStatus.getVehicleId(),
										showApc,
										showRawApc,
										responseTimestamp,
										stopTime.getStopTime().getArrivalTime()));

					}

					// we found our monitored call--stop
					return;
				}
			}
		}
	}

	private void fillOccupancy(MonitoredVehicleJourneyStructure mvj, NycTransitDataService tds, TripStatusBean tripStatus) {
		if (tripStatus == null
				|| tripStatus.getActiveTrip() == null
				|| tripStatus.getActiveTrip().getRoute() ==  null) {
			return;
		}
		VehicleOccupancyRecord vor =
				tds.getVehicleOccupancyRecordForVehicleIdAndRoute(
						AgencyAndId.convertFromString(tripStatus.getVehicleId()),
						tripStatus.getActiveTrip().getRoute().getId(),
						tripStatus.getActiveTrip().getDirectionId());
		mvj.setOccupancy(mapOccupancyStatusToEnumeration(vor));
	}

	private OccupancyEnumeration mapOccupancyStatusToEnumeration(VehicleOccupancyRecord vor) {
		if (vor == null || vor.getOccupancyStatus() == null) return null;
		switch (vor.getOccupancyStatus()) {
			case UNKNOWN:
				return null;
			case EMPTY:
			case MANY_SEATS_AVAILABLE:
			case FEW_SEATS_AVAILABLE:
				return OccupancyEnumeration.SEATS_AVAILABLE;
			case STANDING_ROOM_ONLY:
				return OccupancyEnumeration.STANDING_AVAILABLE;
			case FULL:
			case CRUSHED_STANDING_ROOM_ONLY:
			case NOT_ACCEPTING_PASSENGERS:
				return OccupancyEnumeration.FULL;
			default:
				return null;
		}
	}

	private static OccupancyEnumeration mapOccupancyStatusToEnumeration(Integer occupancyStatusInteger) {

		if (occupancyStatusInteger == null) {
			return null;
		}

		OccupancyStatus occupancyStatus = OccupancyStatus.toEnum(occupancyStatusInteger);

		switch (occupancyStatus) {
			case UNKNOWN:
				return null;
			case EMPTY:
			case MANY_SEATS_AVAILABLE:
			case FEW_SEATS_AVAILABLE:
				return OccupancyEnumeration.SEATS_AVAILABLE;
			case STANDING_ROOM_ONLY:
				return OccupancyEnumeration.STANDING_AVAILABLE;
			case FULL:
			case CRUSHED_STANDING_ROOM_ONLY:
			case NOT_ACCEPTING_PASSENGERS:
				return OccupancyEnumeration.FULL;
			default:
				return null;
		}
	}


	private void fillSituations(MonitoredVehicleJourneyStructure monitoredVehicleJourney, TripStatusBean tripStatus) {
		if (tripStatus == null || tripStatus.getSituations() == null || tripStatus.getSituations().isEmpty()) {
			return;
		}

		List<SituationRefStructure> situationRef = monitoredVehicleJourney.getSituationRef();
		Set<String> uniqueSituationId = new HashSet<>();

		for (ServiceAlertBean situation : tripStatus.getSituations()) {
			String situationId = situation.getId();
			if(uniqueSituationId.contains(situationId)){
				continue;
			}
			SituationRefStructure sitRef = new SituationRefStructure();
			SituationSimpleRefStructure sitSimpleRef = new SituationSimpleRefStructure();
			sitSimpleRef.setValue(situationId);
			sitRef.setSituationSimpleRef(sitSimpleRef);
			situationRef.add(sitRef);
			uniqueSituationId.add(situationId);
		}
	}

	private static OnwardCallStructure getOnwardCallStructure(StopBean stopBean,
															  PresentationService presentationService,
															  double distanceOfCallAlongTrip, double distanceOfVehicleFromCall, int index,
															  SiriSupportPredictionTimepointRecord prediction, long responseTimestamp, int arrivalTime) {

		OnwardCallStructure onwardCallStructure = new OnwardCallStructure();

		StopPointRefStructure stopPointRef = new StopPointRefStructure();
		stopPointRef.setValue(stopBean.getId());
		onwardCallStructure.setStopPointRef(stopPointRef);

		NaturalLanguageStringStructure stopPoint = new NaturalLanguageStringStructure();
		stopPoint.setValue(stopBean.getName());
		onwardCallStructure.setStopPointName(stopPoint);

		boolean isNearFirstStop = false;
		if (distanceOfCallAlongTrip < 100) isNearFirstStop = true;

		if(prediction != null) {
			if (prediction.getTimepointPredictionRecord().getTimepointPredictedArrivalTime() < responseTimestamp) {
				if (!isNearFirstStop) { onwardCallStructure.setExpectedArrivalTime(new Date(responseTimestamp));}
				else {
					onwardCallStructure.setExpectedDepartureTime(new Date(responseTimestamp));
				}
			} else {
				if (!isNearFirstStop) {	onwardCallStructure.setExpectedArrivalTime(new Date(prediction.getTimepointPredictionRecord().getTimepointPredictedArrivalTime()));}
				else {
					onwardCallStructure.setExpectedDepartureTime(new Date(prediction.getTimepointPredictionRecord().getTimepointPredictedDepartureTime()));
				}
			}
		}

		Calendar calendar = Calendar.getInstance(); // gets a calendar using the default time zone and locale.
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DATE);
		calendar.set(year, month, day, 0, 0, 0);
		calendar.add(Calendar.SECOND, arrivalTime);
		onwardCallStructure.setAimedArrivalTime(calendar.getTime());

		// siri extensions
		SiriExtensionWrapper wrapper = new SiriExtensionWrapper();
		ExtensionsStructure extensionsStructure = new ExtensionsStructure();
		SiriDistanceExtension distances = new SiriDistanceExtension();

		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		df.setGroupingUsed(false);

		distances.setStopsFromCall(index);
		distances.setCallDistanceAlongRoute(Double.valueOf(df.format(distanceOfCallAlongTrip)));
		distances.setDistanceFromCall(Double.valueOf(df.format(distanceOfVehicleFromCall)));
		distances.setPresentableDistance(presentationService.getPresentableDistance(distances));

		wrapper.setDistances(distances);
		extensionsStructure.setAny(wrapper);
		onwardCallStructure.setExtensions(extensionsStructure);

		return onwardCallStructure;
	}

	private MonitoredCallStructure getMonitoredCallStructure(StopBean stopBean,
															 NycTransitDataService nycTransitDataService,
															 PresentationService presentationService,
															 double distanceOfCallAlongTrip,
															 double distanceOfVehicleFromCall,
															 int visitNumber, int index,
															 SiriSupportPredictionTimepointRecord prediction,
															 String vehicleId,
															 boolean showApc,
															 boolean showRawApc,
															 long responseTimestamp,
															 int arrivalTime) {

		MonitoredCallStructure monitoredCallStructure = new MonitoredCallStructure();
		monitoredCallStructure.setVisitNumber(BigInteger.valueOf(visitNumber));

		StopPointRefStructure stopPointRef = new StopPointRefStructure();
		stopPointRef.setValue(stopBean.getId());
		monitoredCallStructure.setStopPointRef(stopPointRef);

		NaturalLanguageStringStructure stopPoint = new NaturalLanguageStringStructure();
		stopPoint.setValue(stopBean.getName());
		monitoredCallStructure.setStopPointName(stopPoint);

		if(prediction != null) {
			fillExpectedArrivalDepartureTimes(monitoredCallStructure,
					prediction.getTimepointPredictionRecord().getTimepointPredictedArrivalTime(),
					prediction.getTimepointPredictionRecord().getTimepointPredictedDepartureTime(),
					responseTimestamp);
		}

		Calendar calendar = Calendar.getInstance(); // gets a calendar using the default time zone and locale.
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH);
		int day = calendar.get(Calendar.DATE);
		calendar.set(year, month, day, 0, 0, 0);
		calendar.add(Calendar.SECOND, arrivalTime);
		monitoredCallStructure.setAimedArrivalTime(calendar.getTime());

		// siri extensions
		SiriExtensionWrapper wrapper = new SiriExtensionWrapper();
		ExtensionsStructure anyExtensions = new ExtensionsStructure();
		SiriDistanceExtension distances = new SiriDistanceExtension();

		DecimalFormat df = new DecimalFormat();
		df.setMaximumFractionDigits(2);
		df.setGroupingUsed(false);

		distances.setStopsFromCall(index);
		distances.setCallDistanceAlongRoute(Double.valueOf(df.format(distanceOfCallAlongTrip)));
		distances.setDistanceFromCall(Double.valueOf(df.format(distanceOfVehicleFromCall)));
		distances.setPresentableDistance(presentationService.getPresentableDistance(distances));

		wrapper.setDistances(distances);

		if (vehicleId != null) {
			VehicleOccupancyRecord vor =
					nycTransitDataService.getLastVehicleOccupancyRecordForVehicleId(AgencyAndId.convertFromString(vehicleId));
			if (showRawApc) {
				SiriApcExtension apcExtension = presentationService.getPresentableApc(vor);
				if (apcExtension != null) {
					wrapper.setCapacities(apcExtension);
				}
			}
		}

		anyExtensions.setAny(wrapper);
		monitoredCallStructure.setExtensions(anyExtensions);

		return monitoredCallStructure;
	}

	private void fillExpectedArrivalDepartureTimes(MonitoredCallStructure monitoredCallStructure,
												   long arrivalTime,
												   long departureTime,
												   long responseTimestamp){

		// Both arrival and departure time are in past
		if (arrivalTime < responseTimestamp && departureTime < responseTimestamp) {
			monitoredCallStructure.setExpectedArrivalTime(new Date(responseTimestamp + 1000));
			monitoredCallStructure.setExpectedDepartureTime(new Date(responseTimestamp + 1000));
		}
		// arrival time undefined and departure time in the future
		else if(arrivalTime < 0 && departureTime > responseTimestamp ) {
			monitoredCallStructure.setExpectedArrivalTime(new Date(departureTime));
			monitoredCallStructure.setExpectedDepartureTime(new Date(departureTime));
		}
		// arrival time
		else if(arrivalTime < responseTimestamp){
			monitoredCallStructure.setExpectedArrivalTime(new Date(responseTimestamp + 1000));
			monitoredCallStructure.setExpectedDepartureTime(new Date(departureTime));
		}
		else if(departureTime < responseTimestamp){
			monitoredCallStructure.setExpectedArrivalTime(new Date(arrivalTime));
			monitoredCallStructure.setExpectedDepartureTime(new Date(arrivalTime));
		}
		else {
			monitoredCallStructure.setExpectedArrivalTime(new Date(arrivalTime));
			monitoredCallStructure.setExpectedDepartureTime(new Date(departureTime));
		}
	}

	private int getVisitNumber(HashMap<String, Integer> visitNumberForStop, StopBean stop) {
		int visitNumber;

		if (visitNumberForStop.containsKey(stop.getId())) {
			visitNumber = visitNumberForStop.get(stop.getId()) + 1;
		} else {
			visitNumber = 1;
		}

		visitNumberForStop.put(stop.getId(), visitNumber);

		return visitNumber;
	}

	private ProgressRateEnumeration getProgressRateForPhaseAndStatus(String status, String phase) {
		if (phase == null) {
			return ProgressRateEnumeration.UNKNOWN;
		}

		if (phase.toLowerCase().startsWith("layover")
				|| phase.toLowerCase().startsWith("deadhead")
				|| phase.toLowerCase().equals("at_base")) {
			return ProgressRateEnumeration.NO_PROGRESS;
		}

		if (status != null && status.toLowerCase().equals("stalled")) {
			return ProgressRateEnumeration.NO_PROGRESS;
		}

		if (phase.toLowerCase().equals("in_progress")) {
			return ProgressRateEnumeration.NORMAL_PROGRESS;
		}

		return ProgressRateEnumeration.UNKNOWN;
	}

}
