package org.onebusaway.nyc.presentation.impl.realtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.presentation.impl.DateUtil;
import org.onebusaway.nyc.presentation.impl.realtime.SiriSupportV2.Filters;
import org.onebusaway.nyc.presentation.impl.realtime.SiriSupportV2.OnwardCallsMode;
import org.onebusaway.nyc.presentation.model.DetailLevel;
import org.onebusaway.nyc.presentation.model.siri.RouteDirection;
import org.onebusaway.nyc.presentation.model.siri.StopRouteDirection;
import org.onebusaway.nyc.presentation.service.realtime.PresentationService;
import org.onebusaway.nyc.presentation.service.realtime.RealtimeServiceV2;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.siri.SiriExtensionWrapper;
import org.onebusaway.nyc.transit_data_federation.siri.SiriJsonSerializerV2;
import org.onebusaway.nyc.transit_data_federation.siri.SiriXmlSerializerV2;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalsAndDeparturesQueryBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.NameBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopGroupBean;
import org.onebusaway.transit_data.model.StopGroupingBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsInclusionBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.org.siri.siri_2.AnnotatedStopPointStructure;
import uk.org.siri.siri_2.LineDirectionStructure;
import uk.org.siri.siri_2.MonitoredStopVisitStructure;
import uk.org.siri.siri_2.MonitoredVehicleJourneyStructure;
import uk.org.siri.siri_2.VehicleActivityStructure;
import uk.org.siri.siri_2.VehicleActivityStructure.MonitoredVehicleJourney;

/**
 * A source of SIRI classes containing real time data, subject to the
 * conventions expressed in the PresentationService.
 * 
 * @author jmaki
 *
 */
@Component
public class RealtimeServiceV2Impl implements RealtimeServiceV2 {

	private NycTransitDataService _nycTransitDataService;

	private PresentationService _presentationService;

	private SiriXmlSerializerV2 _siriXmlSerializer = new SiriXmlSerializerV2();

	private SiriJsonSerializerV2 _siriJsonSerializer = new SiriJsonSerializerV2();

	private Long _now = null;

	@Override
	public void setTime(long time) {
		_now = time;
		_presentationService.setTime(time);
	}

	public long getTime() {
		if (_now != null)
			return _now;
		else
			return System.currentTimeMillis();
	}

	@Autowired
	public void setNycTransitDataService(
			NycTransitDataService transitDataService) {
		_nycTransitDataService = transitDataService;
	}

	@Autowired
	public void setPresentationService(PresentationService presentationService) {
		_presentationService = presentationService;
	}

	@Override
	public PresentationService getPresentationService() {
		return _presentationService;
	}

	@Override
	public SiriJsonSerializerV2 getSiriJsonSerializer() {
		return _siriJsonSerializer;
	}

	@Override
	public SiriXmlSerializerV2 getSiriXmlSerializer() {
		return _siriXmlSerializer;
	}

	/**
	 * SIRI METHODS
	 */
	@Override
	public List<VehicleActivityStructure> getVehicleActivityForRoute(
			String routeId, String directionId, int maximumOnwardCalls,
			long currentTime) {
		List<VehicleActivityStructure> output = new ArrayList<VehicleActivityStructure>();

		boolean useTimePredictionsIfAvailable = _presentationService
				.useTimePredictionsIfAvailable();

		ListBean<TripDetailsBean> trips = getAllTripsForRoute(routeId,
				currentTime);
		for (TripDetailsBean tripDetails : trips.getList()) {
			// filter out interlined routes
			if (routeId != null
					&& !tripDetails.getTrip().getRoute().getId()
							.equals(routeId))
				continue;

			// filtered out by user
			if (directionId != null
					&& !tripDetails.getTrip().getDirectionId()
							.equals(directionId))
				continue;

			if (!_presentationService.include(tripDetails.getStatus()))
				continue;

			VehicleActivityStructure activity = new VehicleActivityStructure();
			activity.setRecordedAtTime(DateUtil
					.toXmlGregorianCalendar(tripDetails.getStatus()
							.getLastUpdateTime()));

			List<TimepointPredictionRecord> timePredictionRecords = null;
			if (useTimePredictionsIfAvailable == true) {
				timePredictionRecords = _nycTransitDataService
						.getPredictionRecordsForTrip(AgencyAndId
								.convertFromString(routeId).getAgencyId(),
								tripDetails.getStatus());
			}

			activity.setMonitoredVehicleJourney(new MonitoredVehicleJourney());
			SiriSupportV2.fillMonitoredVehicleJourney(
					activity.getMonitoredVehicleJourney(),
					tripDetails.getTrip(), tripDetails.getStatus(), null,
					OnwardCallsMode.VEHICLE_MONITORING, _presentationService,
					_nycTransitDataService, maximumOnwardCalls,
					timePredictionRecords, currentTime);

			output.add(activity);
		}

		Collections.sort(output, new Comparator<VehicleActivityStructure>() {
			public int compare(VehicleActivityStructure arg0,
					VehicleActivityStructure arg1) {
				try {
					SiriExtensionWrapper wrapper0 = (SiriExtensionWrapper) arg0
							.getMonitoredVehicleJourney().getMonitoredCall()
							.getExtensions().getAny();
					SiriExtensionWrapper wrapper1 = (SiriExtensionWrapper) arg1
							.getMonitoredVehicleJourney().getMonitoredCall()
							.getExtensions().getAny();
					return wrapper0
							.getDistances()
							.getDistanceFromCall()
							.compareTo(
									wrapper1.getDistances()
											.getDistanceFromCall());
				} catch (Exception e) {
					return -1;
				}
			}
		});

		return output;
	}

	@Override
	public VehicleActivityStructure getVehicleActivityForVehicle(
			String vehicleId, int maximumOnwardCalls, long currentTime) {

		boolean useTimePredictionsIfAvailable = _presentationService
				.useTimePredictionsIfAvailable();

		TripForVehicleQueryBean query = new TripForVehicleQueryBean();
		query.setTime(new Date(currentTime));
		query.setVehicleId(vehicleId);

		TripDetailsInclusionBean inclusion = new TripDetailsInclusionBean();
		inclusion.setIncludeTripStatus(true);
		inclusion.setIncludeTripBean(true);
		query.setInclusion(inclusion);

		TripDetailsBean tripDetailsForCurrentTrip = _nycTransitDataService
				.getTripDetailsForVehicleAndTime(query);
		if (tripDetailsForCurrentTrip != null) {
			if (!_presentationService.include(tripDetailsForCurrentTrip
					.getStatus()))
				return null;

			VehicleActivityStructure output = new VehicleActivityStructure();
			output.setRecordedAtTime(DateUtil
					.toXmlGregorianCalendar(tripDetailsForCurrentTrip
							.getStatus().getLastUpdateTime()));

			List<TimepointPredictionRecord> timePredictionRecords = null;
			if (useTimePredictionsIfAvailable == true) {
				timePredictionRecords = _nycTransitDataService
						.getPredictionRecordsForTrip(AgencyAndId
								.convertFromString(vehicleId).getAgencyId(),
								tripDetailsForCurrentTrip.getStatus());
			}

			output.setMonitoredVehicleJourney(new MonitoredVehicleJourney());
			SiriSupportV2.fillMonitoredVehicleJourney(
					output.getMonitoredVehicleJourney(),
					tripDetailsForCurrentTrip.getTrip(),
					tripDetailsForCurrentTrip.getStatus(), null,
					OnwardCallsMode.VEHICLE_MONITORING, _presentationService,
					_nycTransitDataService, maximumOnwardCalls,
					timePredictionRecords, currentTime);

			return output;
		}

		return null;
	}

	@Override
	public List<MonitoredStopVisitStructure> getMonitoredStopVisitsForStop(
			String stopId, int maximumOnwardCalls, long currentTime) {
		List<MonitoredStopVisitStructure> output = new ArrayList<MonitoredStopVisitStructure>();

		boolean useTimePredictionsIfAvailable = _presentationService
				.useTimePredictionsIfAvailable();

		for (ArrivalAndDepartureBean adBean : getArrivalsAndDeparturesForStop(
				stopId, currentTime)) {
			TripStatusBean statusBeanForCurrentTrip = adBean.getTripStatus();
			TripBean tripBeanForAd = adBean.getTrip();

			if (statusBeanForCurrentTrip == null)
				continue;

			if (!_presentationService.include(statusBeanForCurrentTrip)
					|| !_presentationService.include(adBean,
							statusBeanForCurrentTrip))
				continue;

			MonitoredStopVisitStructure stopVisit = new MonitoredStopVisitStructure();
			stopVisit.setRecordedAtTime(DateUtil
					.toXmlGregorianCalendar(statusBeanForCurrentTrip
							.getLastUpdateTime()));

			List<TimepointPredictionRecord> timePredictionRecords = null;
			if (useTimePredictionsIfAvailable == true) {
				timePredictionRecords = _nycTransitDataService
						.getPredictionRecordsForTrip(AgencyAndId
								.convertFromString(stopId).getAgencyId(),
								statusBeanForCurrentTrip);
			}

			stopVisit
					.setMonitoredVehicleJourney(new MonitoredVehicleJourneyStructure());
			SiriSupportV2.fillMonitoredVehicleJourney(
					stopVisit.getMonitoredVehicleJourney(), tripBeanForAd,
					statusBeanForCurrentTrip, adBean.getStop(),
					OnwardCallsMode.STOP_MONITORING, _presentationService,
					_nycTransitDataService, maximumOnwardCalls,
					timePredictionRecords, currentTime);

			output.add(stopVisit);
		}

		Collections.sort(output, new Comparator<MonitoredStopVisitStructure>() {
			public int compare(MonitoredStopVisitStructure arg0,
					MonitoredStopVisitStructure arg1) {
				try {
					SiriExtensionWrapper wrapper0 = (SiriExtensionWrapper) arg0
							.getMonitoredVehicleJourney().getMonitoredCall()
							.getExtensions().getAny();
					SiriExtensionWrapper wrapper1 = (SiriExtensionWrapper) arg1
							.getMonitoredVehicleJourney().getMonitoredCall()
							.getExtensions().getAny();
					return wrapper0
							.getDistances()
							.getDistanceFromCall()
							.compareTo(
									wrapper1.getDistances()
											.getDistanceFromCall());
				} catch (Exception e) {
					return -1;
				}
			}
		});

		return output;
	}

	@Override
	public Map<Boolean, List<AnnotatedStopPointStructure>> getAnnotatedStopPointStructures(
			CoordinateBounds bounds, DetailLevel detailLevel, long currentTime,
			Map<Filters, String> filters) {
		
		// Cache stops by route so we don't need to call the transit data service repeatedly for the same route
		Map<String, StopsForRouteBean> stopsForRouteCache = new HashMap<String, StopsForRouteBean>();
		
		// Store processed StopBean as AnnotatedStopPointStructure 
		List<AnnotatedStopPointStructure> annotatedStopPoints = new ArrayList<AnnotatedStopPointStructure>();
		
		// AnnotatedStopPointStructures List with hasUpcomingScheduledService
		Map<Boolean, List<AnnotatedStopPointStructure>> output = new HashMap<Boolean, List<AnnotatedStopPointStructure>>();
		
		Boolean upcomingServiceAllStops = true;  
		
		List<StopBean> stopBeans = getStopsForBounds(bounds, currentTime);
		
		processAnnotatedStopPoints(stopBeans,annotatedStopPoints, 
				filters, stopsForRouteCache, detailLevel, currentTime);

		output.put(upcomingServiceAllStops, annotatedStopPoints);
		
		return output;
					

		/*
		 * Collections.sort(output, new
		 * Comparator<MonitoredStopVisitStructure>() { public int
		 * compare(MonitoredStopVisitStructure arg0, MonitoredStopVisitStructure
		 * arg1) { try { SiriExtensionWrapper wrapper0 =
		 * (SiriExtensionWrapper)arg0
		 * .getMonitoredVehicleJourney().getMonitoredCall
		 * ().getExtensions().getAny(); SiriExtensionWrapper wrapper1 =
		 * (SiriExtensionWrapper
		 * )arg1.getMonitoredVehicleJourney().getMonitoredCall
		 * ().getExtensions().getAny(); return
		 * wrapper0.getDistances().getDistanceFromCall
		 * ().compareTo(wrapper1.getDistances().getDistanceFromCall()); }
		 * catch(Exception e) { return -1; } } });
		 */
	}

	@Override
	public Map<Boolean, List<AnnotatedStopPointStructure>> getAnnotatedStopPointStructures(
			List<AgencyAndId> routeIds, 
			DetailLevel detailLevel, 
			long currentTime,
			Map<Filters, String> filters) {
		
		// Cache stops by route so we don't need to call the transit data service repeatedly for the same route
		Map<String, StopsForRouteBean> stopsForRouteCache = new HashMap<String, StopsForRouteBean>();
		
		// Store processed StopBean as AnnotatedStopPointStructure 
		List<AnnotatedStopPointStructure> annotatedStopPoints = new ArrayList<AnnotatedStopPointStructure>();
		
		// AnnotatedStopPointStructures List with hasUpcomingScheduledService
		Map<Boolean, List<AnnotatedStopPointStructure>> output = new HashMap<Boolean, List<AnnotatedStopPointStructure>>();
		
		Boolean upcomingServiceAllStops = null;  
		
		for(AgencyAndId aid : routeIds){

			String routeId = AgencyAndId.convertToString(aid);
		
			StopsForRouteBean stopsForLineRef = _nycTransitDataService.getStopsForRoute(routeId);

	    	processAnnotatedStopPoints(stopsForLineRef.getStops(), annotatedStopPoints, filters, 
		    			stopsForRouteCache, detailLevel, currentTime);

		}
		
		output.put(upcomingServiceAllStops, annotatedStopPoints);
		return output;
			
	}

	/**
	 * CURRENT IN-SERVICE VEHICLE STATUS FOR ROUTE
	 */

	/**
	 * Returns true if there are vehicles in service for given route+direction
	 */
	@Override
	public boolean getVehiclesInServiceForRoute(String routeId,
			String directionId, long currentTime) {
		ListBean<TripDetailsBean> trips = getAllTripsForRoute(routeId,
				currentTime);
		for (TripDetailsBean tripDetails : trips.getList()) {
			// filter out interlined routes
			if (routeId != null
					&& !tripDetails.getTrip().getRoute().getId()
							.equals(routeId))
				continue;

			// filtered out by user
			if (directionId != null
					&& !tripDetails.getTrip().getDirectionId()
							.equals(directionId))
				continue;

			if (!_presentationService.include(tripDetails.getStatus()))
				continue;

			return true;
		}

		return false;
	}

	/**
	 * Returns true if there are vehicles in service for given route+direction
	 * that will stop at the indicated stop in the future.
	 */
	@Override
	public boolean getVehiclesInServiceForStopAndRoute(String stopId,
			String routeId, long currentTime) {
		for (ArrivalAndDepartureBean adBean : getArrivalsAndDeparturesForStop(
				stopId, currentTime)) {
			TripStatusBean statusBean = adBean.getTripStatus();
			if (!_presentationService.include(statusBean)
					|| !_presentationService.include(adBean, statusBean))
				continue;

			// filtered out by user
			if (routeId != null
					&& !adBean.getTrip().getRoute().getId().equals(routeId))
				continue;

			return true;
		}

		return false;
	}

	/**
	 * SERVICE ALERTS METHODS
	 */

	@Override
	public List<ServiceAlertBean> getServiceAlertsForRoute(String routeId) {
		return getServiceAlertsForRouteAndDirection(routeId, null);
	}

	@Override
	public List<ServiceAlertBean> getServiceAlertsForRouteAndDirection(
			String routeId, String directionId) {
		SituationQueryBean query = new SituationQueryBean();
		SituationQueryBean.AffectsBean affects = new SituationQueryBean.AffectsBean();
		query.getAffects().add(affects);

		affects.setRouteId(routeId);
		if (directionId != null) {
			affects.setDirectionId(directionId);
		} else {
			/*
			 * TODO The route index is not currently being populated correctly;
			 * query by route and direction, and supply both directions if not
			 * present
			 */
			SituationQueryBean.AffectsBean affects1 = new SituationQueryBean.AffectsBean();
			query.getAffects().add(affects1);
			affects1.setRouteId(routeId);
			affects1.setDirectionId("0");
			SituationQueryBean.AffectsBean affects2 = new SituationQueryBean.AffectsBean();
			query.getAffects().add(affects2);
			affects2.setRouteId(routeId);
			affects2.setDirectionId("1");
		}

		ListBean<ServiceAlertBean> serviceAlerts = _nycTransitDataService
				.getServiceAlerts(query);
		return serviceAlerts.getList();
	}

	@Override
	public List<ServiceAlertBean> getServiceAlertsGlobal() {
		SituationQueryBean query = new SituationQueryBean();
		SituationQueryBean.AffectsBean affects = new SituationQueryBean.AffectsBean();

		affects.setAgencyId("__ALL_OPERATORS__");
		query.getAffects().add(affects);

		ListBean<ServiceAlertBean> serviceAlerts = _nycTransitDataService
				.getServiceAlerts(query);
		return serviceAlerts.getList();
	}

	/**
	 * PRIVATE METHODS
	 */
	private ListBean<TripDetailsBean> getAllTripsForRoute(String routeId,
			long currentTime) {
		TripsForRouteQueryBean tripRouteQueryBean = new TripsForRouteQueryBean();
		tripRouteQueryBean.setRouteId(routeId);
		tripRouteQueryBean.setTime(currentTime);

		TripDetailsInclusionBean inclusionBean = new TripDetailsInclusionBean();
		inclusionBean.setIncludeTripBean(true);
		inclusionBean.setIncludeTripStatus(true);
		tripRouteQueryBean.setInclusion(inclusionBean);

		return _nycTransitDataService.getTripsForRoute(tripRouteQueryBean);
	}

	private List<ArrivalAndDepartureBean> getArrivalsAndDeparturesForStop(
			String stopId, long currentTime) {
		ArrivalsAndDeparturesQueryBean query = new ArrivalsAndDeparturesQueryBean();
		query.setTime(currentTime);
		query.setMinutesBefore(5 * 60);
		query.setMinutesAfter(5 * 60);

		StopWithArrivalsAndDeparturesBean stopWithArrivalsAndDepartures = _nycTransitDataService
				.getStopWithArrivalsAndDepartures(stopId, query);

		return stopWithArrivalsAndDepartures.getArrivalsAndDepartures();
	}

	private List<StopBean> getStopsForBounds(CoordinateBounds bounds,
			long currentTime) {
		if (bounds != null) {
			SearchQueryBean queryBean = new SearchQueryBean();
			queryBean.setType(SearchQueryBean.EQueryType.BOUNDS_OR_CLOSEST);
			queryBean.setBounds(bounds);
			queryBean.setMaxCount(100);

			StopsBean stops = _nycTransitDataService.getStops(queryBean);
			return stops.getStops();
		}
		return new ArrayList<StopBean>();
	}
	
	public void processAnnotatedStopPoints(
			List<StopBean> stopBeans, 
			List<AnnotatedStopPointStructure> annotatedStopPoints, 
			Map<Filters, String> filters, 
			Map<String, StopsForRouteBean> stopsForRouteCache,
			DetailLevel detailLevel,
			long currentTime
			) {
		
		// Filter Values
		String upcomingScheduledServiceFilter = filters.get(Filters.UPCOMING_SCHEDULED_SERVICE);
		String directionIdFilter = filters.get(Filters.DIRECTION_REF);
		
		for (StopBean stopBean : stopBeans) {
			
			StopRouteDirection stopRouteDirection = new StopRouteDirection(stopBean);
		
			// route is a route serving the current stopBeanForSearchResult
			for (RouteBean route : stopBean.getRoutes()) {
				
				// Query for all stops on this route
				StopsForRouteBean stopsForRoute = stopsForRouteCache.get(route.getId());
				if (stopsForRoute == null) {
					stopsForRoute = _nycTransitDataService.getStopsForRoute(route.getId());
					stopsForRouteCache.put(route.getId(), stopsForRoute);
				}
	
				// Get the groups of stops on this route. The id of each group corresponds to a GTFS direction id for this route.
				for (StopGroupingBean stopGrouping : stopsForRoute.getStopGroupings()) {
					for (StopGroupBean stopGroup : stopGrouping.getStopGroups()) {
						NameBean name = stopGroup.getName();
				        String type = name.getType();
						String directionId = stopGroup.getId();
						
						// Destination and DirectionId Filter
						if(!type.equals("destination") || !SiriSupportV2.passFilter(directionId, directionIdFilter))
							continue;
						
						// Check if the current stop is served in this direction. If so, record it.
						if (stopGroup.getStopIds().contains(stopBean.getId())) {
							
							// filter hasUpcomingScheduledService
							Boolean hasUpcomingScheduledService = _nycTransitDataService
									.stopHasUpcomingScheduledService((route
											.getAgency() != null ? route
											.getAgency().getId() : null), 
									System.currentTimeMillis(), 
									stopBean.getId(), 
									route.getId(), 
									directionId
							);
							
							String hasUpcomingScheduledServiceVal = String.valueOf(hasUpcomingScheduledService);
	
							if(!SiriSupportV2.passFilter(hasUpcomingScheduledServiceVal,upcomingScheduledServiceFilter) 
									|| !hasUpcomingScheduledService)
								continue;
							
							stopRouteDirection.addRouteDirection(new RouteDirection(route, directionId, hasUpcomingScheduledService));
						}
					}
				}
			}
			
			AnnotatedStopPointStructure annotatedStopPoint = new AnnotatedStopPointStructure();
			
			boolean isValid = SiriSupportV2.fillAnnotatedStopPointStructure(annotatedStopPoint,
					stopRouteDirection, filters, detailLevel, currentTime);
			
			if(isValid)
				annotatedStopPoints.add(annotatedStopPoint);
		}
		
	}

}