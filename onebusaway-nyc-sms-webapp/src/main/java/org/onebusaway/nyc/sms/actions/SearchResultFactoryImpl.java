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
package org.onebusaway.nyc.sms.actions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.onebusaway.nyc.presentation.impl.search.AbstractSearchResultFactoryImpl;
import org.onebusaway.nyc.presentation.model.SearchResult;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeService;
import org.onebusaway.nyc.siri.support.SiriDistanceExtension;
import org.onebusaway.nyc.siri.support.SiriExtensionWrapper;
import org.onebusaway.nyc.sms.actions.model.GeocodeResult;
import org.onebusaway.nyc.sms.actions.model.RouteAtStop;
import org.onebusaway.nyc.sms.actions.model.RouteDirection;
import org.onebusaway.nyc.sms.actions.model.RouteResult;
import org.onebusaway.nyc.sms.actions.model.StopResult;
import org.onebusaway.nyc.sms.actions.model.VehicleResult;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.util.configuration.ConfigurationService;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.service_alerts.NaturalLanguageStringBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;

import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import uk.org.siri.siri.MonitoredCallStructure;
import uk.org.siri.siri.MonitoredStopVisitStructure;
import uk.org.siri.siri.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri.NaturalLanguageStringStructure;

public class SearchResultFactoryImpl extends AbstractSearchResultFactoryImpl {

	private static final boolean HTMLIZE_NEWLINES = false;

	private ConfigurationService _configurationService;

	private RealtimeService _realtimeService;

	private NycTransitDataService _nycTransitDataService;

	public SearchResultFactoryImpl(NycTransitDataService nycTransitDataService, 
			RealtimeService realtimeService, ConfigurationService configurationService) {
		_nycTransitDataService = nycTransitDataService;
		_realtimeService = realtimeService;
		_configurationService = configurationService;
	}

	@Override
	public SearchResult getRouteResultForRegion(RouteBean routeBean) { 
		return null;
	}

	@Override
	public SearchResult getRouteResult(RouteBean routeBean) {    
		List<RouteDirection> directions = new ArrayList<RouteDirection>();

		StopsForRouteBean stopsForRoute = _nycTransitDataService.getStopsForRoute(routeBean.getId());

		List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
		for (StopGroupingBean stopGroupingBean : stopGroupings) {
			for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
				NameBean name = stopGroupBean.getName();
				String type = name.getType();

				if (!type.equals("destination"))
					continue;

				Boolean hasUpcomingScheduledService = 
						_nycTransitDataService.routeHasUpcomingScheduledService((routeBean.getAgency()!=null?routeBean.getAgency().getId():null), System.currentTimeMillis(), routeBean.getId(), stopGroupBean.getId());

				// if there are buses on route, always have "scheduled service"
				Boolean routeHasVehiclesInService = 
						_realtimeService.getVehiclesInServiceForRoute(routeBean.getId(), stopGroupBean.getId(), System.currentTimeMillis());

				if(routeHasVehiclesInService) {
					hasUpcomingScheduledService = true;
				}

				// service alerts for this route + direction
				List<NaturalLanguageStringBean> serviceAlertDescriptions = new ArrayList<NaturalLanguageStringBean>();
				List<ServiceAlertBean> serviceAlertBeans = _realtimeService.getServiceAlertsForRouteAndDirection(routeBean.getId(), stopGroupBean.getId());
				populateServiceAlerts(serviceAlertDescriptions, serviceAlertBeans, HTMLIZE_NEWLINES);

				directions.add(new RouteDirection(stopGroupBean, hasUpcomingScheduledService, null, serviceAlertDescriptions));
			}
		}

		return new RouteResult(routeBean, directions);
	}

	@Override
	public SearchResult getStopResult(StopBean stopBean, Set<RouteBean> routeFilter) {
		List<RouteAtStop> routesAtStop = new ArrayList<RouteAtStop>();
		boolean matchesRouteIdFilter = false;

		HashMap<String, HashMap<Double, VehicleResult>> distanceAwayResultsByDistanceFromStopAndRouteAndDirection =
				getDistanceAwayStringsForStopByDistanceFromStopAndRouteAndDirection(stopBean);

		for(RouteBean routeBean : stopBean.getRoutes()) {
			if((routeFilter == null || routeFilter.isEmpty()) || routeFilter != null && routeFilter.contains(routeBean)) {
				matchesRouteIdFilter = true;
			}

			StopsForRouteBean stopsForRoute = _nycTransitDataService.getStopsForRoute(routeBean.getId());

			List<RouteDirection> directions = new ArrayList<RouteDirection>();
			List<StopGroupingBean> stopGroupings = stopsForRoute.getStopGroupings();
			for (StopGroupingBean stopGroupingBean : stopGroupings) {
				for (StopGroupBean stopGroupBean : stopGroupingBean.getStopGroups()) {
					NameBean name = stopGroupBean.getName();
					String type = name.getType();

					if (!type.equals("destination"))
						continue;

					// filter out route directions that don't stop at this stop
					if(!stopGroupBean.getStopIds().contains(stopBean.getId()))
						continue;

					HashMap<Double, VehicleResult> distanceAwayResultsByDistanceFromStop =
							distanceAwayResultsByDistanceFromStopAndRouteAndDirection.get(routeBean.getId() + "_" + stopGroupBean.getId());

					Boolean hasUpcomingScheduledService = 
							_nycTransitDataService.stopHasUpcomingScheduledService((routeBean.getAgency()!=null?routeBean.getAgency().getId():null), System.currentTimeMillis(), stopBean.getId(), routeBean.getId(), stopGroupBean.getId());

					// if there are buses on route, always have "scheduled service"
					if(distanceAwayResultsByDistanceFromStop != null && !distanceAwayResultsByDistanceFromStop.isEmpty()) {
						hasUpcomingScheduledService = true;
					}

					// service alerts for this route + direction
					List<NaturalLanguageStringBean> serviceAlertDescriptions = new ArrayList<NaturalLanguageStringBean>();
					List<ServiceAlertBean> serviceAlertBeans = _realtimeService.getServiceAlertsForRouteAndDirection(routeBean.getId(), stopGroupBean.getId());
					populateServiceAlerts(serviceAlertDescriptions, serviceAlertBeans, HTMLIZE_NEWLINES);

					directions.add(new RouteDirection(stopGroupBean, hasUpcomingScheduledService, distanceAwayResultsByDistanceFromStop, serviceAlertDescriptions));
				}
			}

			RouteAtStop routeAtStop = new RouteAtStop(routeBean, directions);
			routesAtStop.add(routeAtStop);
		}

		return new StopResult(stopBean, routesAtStop, matchesRouteIdFilter);
	}

	@Override
	public SearchResult getGeocoderResult(NycGeocoderResult geocodeResult, Set<RouteBean> routeFilter) {
		return new GeocodeResult(geocodeResult);   
	}

	/*** 
	 * PRIVATE METHODS
	 */
	private HashMap<String, HashMap<Double, VehicleResult>> getDistanceAwayStringsForStopByDistanceFromStopAndRouteAndDirection(StopBean stopBean) {
		HashMap<String, HashMap<Double, VehicleResult>> result = new HashMap<String, HashMap<Double, VehicleResult>>();

		Boolean showApc = _realtimeService.showApc();
		Boolean showRawApc = _realtimeService.showRawApc();


		// stop visits
		List<MonitoredStopVisitStructure> visitList = 
				_realtimeService.getMonitoredStopVisitsForStop(stopBean.getId(), 0,
						System.currentTimeMillis(), showApc, showRawApc, false);

		for(MonitoredStopVisitStructure visit : visitList) {
			// on detour? don't show it. 
			MonitoredCallStructure monitoredCall = visit.getMonitoredVehicleJourney().getMonitoredCall();
			if(monitoredCall == null)
				continue;

			String distance = getPresentableDistance(visit.getMonitoredVehicleJourney(),
					visit.getRecordedAtTime().getTime(), true);

			String timePrediction = getPresentableTime(visit.getMonitoredVehicleJourney(),
					visit.getRecordedAtTime().getTime(), true);

			SiriExtensionWrapper wrapper = (SiriExtensionWrapper)monitoredCall.getExtensions().getAny();
			SiriDistanceExtension distanceExtension = wrapper.getDistances();    

			String routeId = visit.getMonitoredVehicleJourney().getLineRef().getValue();
			String directionId = visit.getMonitoredVehicleJourney().getDirectionRef().getValue();
			String key = routeId + "_" + directionId;
			String vehicleId = visit.getMonitoredVehicleJourney().getVehicleRef().getValue();
			AgencyAndId vehicleAgencyAndId = AgencyAndId.convertFromString(vehicleId);
			AgencyAndId routeAgencyAndId = AgencyAndIdLibrary.convertFromString(routeId);
			//  here we are careful to query apc for route+direction so trip boundaries are considered
			VehicleOccupancyRecord vor = _nycTransitDataService.getVehicleOccupancyRecordForVehicleIdAndRoute(vehicleAgencyAndId,
							AgencyAndIdLibrary.convertToString(routeAgencyAndId),
							directionId);

			HashMap<Double, VehicleResult> map = result.get(key);
			if(map == null) {
				map = new HashMap<Double,VehicleResult>();
			}

			if(timePrediction != null) {
				map.put(distanceExtension.getDistanceFromCall(), new VehicleResult(timePrediction, vehicleId, vor, getOccupancyConfig()));
			} else {
				map.put(distanceExtension.getDistanceFromCall(), new VehicleResult(distance, vehicleId, vor, getOccupancyConfig()));
			}

			result.put(key, map);
		}

		return result;
	}

	private VehicleResult.OccupancyConfig getOccupancyConfig() {
		String config = _configurationService.getConfigurationValueAsString("display.apcMode", "PASSENGERCOUNT");
		if ("NONE".equals(config)) {
			return VehicleResult.OccupancyConfig.NONE;
		} else if ("OCCUPANCY".equals(config)) {
			return VehicleResult.OccupancyConfig.OCCUPANCY;
		} else if ("LOADFACTOR".equals(config)) {
			return VehicleResult.OccupancyConfig.LOAD_FACTOR;
		} else if ("PASSENGERCOUNT".equals(config)) {
			return VehicleResult.OccupancyConfig.PASSENGER_COUNT;
		} else if ("LOADFACTORPASSENGERCOUNT".equals(config)) {
			return VehicleResult.OccupancyConfig.LOAD_FACTOR_PASSENGER_COUNT;
		} else {
			// we are likely misconfigured; default to PASSENGER_COUNT
			return VehicleResult.OccupancyConfig.PASSENGER_COUNT;
		}

	}


	private String getPresentableOccupancy(
			MonitoredVehicleJourneyStructure journey, long updateTime,
			boolean isStopContext) {

		// only show load in stop-level contexts
		if(!isStopContext) {
			return "";
		}

		// if data is old, no occupancy
		int staleTimeout = _configurationService.getConfigurationValueAsInteger("display.staleTimeout", 120);
		long age = (System.currentTimeMillis() - updateTime) / 1000;

		if (age > staleTimeout) {
			return "";
		}

		if(journey.getOccupancy() != null) {
			String loadOccupancy = journey.getOccupancy().toString();
			loadOccupancy = loadOccupancy.toUpperCase();
			//TODO: Modify output load text here
			if(loadOccupancy.equals("SEATS_AVAILABLE") || loadOccupancy.equals("MANY_SEATS_AVAILABLE"))
				loadOccupancy = "seats available";
			else if (loadOccupancy.equals("FEW_SEATS_AVAILABLE"))
				loadOccupancy = "almost full";
			else if (loadOccupancy.equals("FULL"))
				loadOccupancy = "full";
			
			return loadOccupancy;
		}else
			return "";

	}
	
	private String getPresentableTime(
			MonitoredVehicleJourneyStructure journey, long updateTime,
			boolean isStopContext) {
		String presentableTime = null;
		NaturalLanguageStringStructure progressStatus = journey.getProgressStatus();
		MonitoredCallStructure monitoredCall = journey.getMonitoredCall();

		// only show predictions in stop-level contexts
		if(!isStopContext) {
			return null;
		}

		// if data is old, no predictions
		int staleTimeout = _configurationService.getConfigurationValueAsInteger("display.staleTimeout", 120);
		long age = (System.currentTimeMillis() - updateTime) / 1000;

		if (age > staleTimeout) {
			return null;
		}

		if(monitoredCall.getExpectedArrivalTime() != null) {
			long predictedArrival = monitoredCall.getExpectedArrivalTime().getTime();

			SiriExtensionWrapper wrapper = (SiriExtensionWrapper) monitoredCall.getExtensions().getAny();
			SiriDistanceExtension distanceExtension = wrapper.getDistances();
			String distance = distanceExtension.getPresentableDistance();
			// loadOccupancy has been moved to additional request like service alerts
			double minutes = Math.floor((predictedArrival - updateTime) / 60 / 1000);
			String timeString = Math.round(minutes) + " min" + ((Math.abs(minutes) != 1) ? "s." : ".");

			// if wrapped, only show prediction, if not wrapped, show both
			if(progressStatus != null && progressStatus.getValue().contains("prevTrip")) {
				presentableTime = timeString;
			} 
			else if(progressStatus != null && progressStatus.getValue().contains("layover")){
				presentableTime = getPresentableDistance(journey, updateTime, isStopContext);
			}
			else {
				presentableTime = timeString + ", " + distance;
			}
		}

		return presentableTime;
	}	  

	private String getPresentableDistance(MonitoredVehicleJourneyStructure journey, long updateTime, boolean isStopContext) {
		MonitoredCallStructure monitoredCall = journey.getMonitoredCall();
		SiriExtensionWrapper wrapper = (SiriExtensionWrapper)monitoredCall.getExtensions().getAny();
		SiriDistanceExtension distanceExtension = wrapper.getDistances();    

		String message = "";    
		String distance = _realtimeService.getPresentationService().getPresentableDistance(distanceExtension, "arriving", "stop", "stops", "mile", "miles", "");
		String loadOccupancy = getPresentableOccupancy(journey, updateTime, isStopContext);
		
		NaturalLanguageStringStructure progressStatus = journey.getProgressStatus();

		// wrapped label only appears in stop results
		if(isStopContext) {    	
			if(progressStatus != null && progressStatus.getValue().contains("layover")) {
				if(journey.getOriginAimedDepartureTime() != null) {
					DateFormat formatter = new SimpleDateFormat("h:mm");

					if(journey.getOriginAimedDepartureTime().getTime() < new Date().getTime()) {
						message += "@term.";
					} else {
						message += "@term. sched. dep. " + 
								formatter.format(journey.getOriginAimedDepartureTime());
					}
				} else {
					message += "@term.";
				}
			} else if(progressStatus != null && progressStatus.getValue().contains("prevTrip")) {
				message += "+layover@term.";
			}
		}

		int staleTimeout = _configurationService.getConfigurationValueAsInteger("display.staleTimeout", 120);    
		long age = (System.currentTimeMillis() - updateTime) / 1000;

		if(age > staleTimeout) {
			if(message.length() > 0) {
				message += ", ";
			}

			message += "old";
		}

		if(message.length() > 0)
			return distance + "(" + message + ")";
		else
			return distance + " "+loadOccupancy;
	}


}
