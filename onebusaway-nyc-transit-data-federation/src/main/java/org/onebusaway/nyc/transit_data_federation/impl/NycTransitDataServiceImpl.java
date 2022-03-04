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
package org.onebusaway.nyc.transit_data_federation.impl;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.nyc.transit_data_federation.impl.nyc.BundleSearchServiceImpl;
import org.onebusaway.nyc.transit_data_federation.model.bundle.BundleItem;
import org.onebusaway.nyc.transit_data_federation.services.bundle.BundleManagementService;
import org.onebusaway.nyc.transit_data_federation.services.predictions.PredictionIntegrationService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.realtime.api.VehicleOccupancyRecord;
import org.onebusaway.transit_data.OccupancyStatusBean;
import org.onebusaway.transit_data.model.*;
import org.onebusaway.transit_data.model.blocks.BlockBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.ScheduledBlockLocationBean;
import org.onebusaway.transit_data.model.config.BundleMetadata;
import org.onebusaway.transit_data.model.problems.ETripProblemGroupBy;
import org.onebusaway.transit_data.model.problems.StopProblemReportBean;
import org.onebusaway.transit_data.model.problems.StopProblemReportQueryBean;
import org.onebusaway.transit_data.model.problems.StopProblemReportSummaryBean;
import org.onebusaway.transit_data.model.problems.TripProblemReportBean;
import org.onebusaway.transit_data.model.problems.TripProblemReportQueryBean;
import org.onebusaway.transit_data.model.problems.TripProblemReportSummaryBean;
import org.onebusaway.transit_data.model.realtime.CurrentVehicleEstimateBean;
import org.onebusaway.transit_data.model.realtime.CurrentVehicleEstimateQueryBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordBean;
import org.onebusaway.transit_data.model.realtime.VehicleLocationRecordQueryBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertBean;
import org.onebusaway.transit_data.model.service_alerts.ServiceAlertRecordBean;
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;
import org.onebusaway.transit_data.model.trips.*;
import org.onebusaway.transit_data.services.TransitDataService;
import org.onebusaway.transit_data_federation.impl.federated.TransitDataServiceTemplateImpl;
import org.onebusaway.transit_data_federation.services.CancelledTripService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component(value = "nycTransitDataServiceImpl")
class NycTransitDataServiceImpl implements NycTransitDataService {

    private static Logger _log = LoggerFactory.getLogger(NycTransitDataServiceImpl.class);

    @Autowired
    private TransitDataServiceTemplateImpl _transitDataService;

    @Autowired
    private BundleManagementService _bundleManagementService;

    @Autowired(required=false)
    private PredictionIntegrationService _predictionIntegrationService;

    @Autowired(required=false)
    private CancelledTripService _cancelledTripService;

    @Autowired
    @Qualifier("NycBundleSearchService")
    private BundleSearchServiceImpl _bundleSearchService;

	/*@Autowired(required = false)
	private GtfsSometimesHandler _gtfsSometimesHandler;*/

    private int _blockedRequestCounter = 0;

    /**
     * This method blocks until the bundle is ready--this method is called as part of the proxy to each of the underlying
     * methods of the TDS and NYC-TDS below to ensure all calls to those bundle-backed methods succeed (i.e. the bundle is ready
     * to be queried.)
     */
	/*private void blockUntilBundleIsReady() {
		try {
			boolean gtfsSometimes = false;
			while((_bundleManagementService != null && !_bundleManagementService.bundleIsReady())
					|| (_gtfsSometimesHandler != null && (gtfsSometimes = _gtfsSometimesHandler.isApplying()))) {
				_blockedRequestCounter++;

				// only print this every 25 times so we don't fill up the logs!
				if(_blockedRequestCounter > 25) {
					if (gtfsSometimes) {
						_log.warn("GTFS-sometimes application in progress--we've blocked 25 TDS requests since last log event.");
					} else {
						_log.warn("Bundle is not ready or none is loaded--we've blocked 25 TDS requests since last log event.");
					}
					_blockedRequestCounter = 0;
				}

				synchronized(this) {
					Thread.sleep(250);
					Thread.yield();
				}
			}
		} catch(InterruptedException e) {
			return;
		}
	}*/
    private void blockUntilBundleIsReady() {
        try {
            while(_bundleManagementService != null && !_bundleManagementService.bundleIsReady()) {
                _blockedRequestCounter++;

                // only print this every 25 times so we don't fill up the logs!
                if(_blockedRequestCounter > 25) {
                    _log.warn("Bundle is not ready or none is loaded--we've blocked 25 TDS requests since last log event.");
                    _blockedRequestCounter = 0;
                }

                synchronized(this) {
                    Thread.sleep(250);
                    Thread.yield();
                }
            }
        } catch(InterruptedException e) {
            return;
        }
    }

    /****
     * {@link NycTransitDataService} Interface
     ****/

    @Override
    public List<TimepointPredictionRecord> getPredictionRecordsForTrip(String agencyId,
                                                                       TripStatusBean tripStatus) {
        blockUntilBundleIsReady();
        if (_predictionIntegrationService != null && _predictionIntegrationService.isEnabled()) {
            return _predictionIntegrationService.getPredictionsForTrip(tripStatus);
        }
        return _transitDataService.getPredictionRecordsForTrip(agencyId, tripStatus);
    }

    public List<TimepointPredictionRecord> getPredictionRecordsForVehicleAndTripStatus(String vehicleId,
                                                                                       TripStatusBean tripStatus) {
        blockUntilBundleIsReady();
        if (_predictionIntegrationService != null && _predictionIntegrationService.isEnabled()) {
            return _predictionIntegrationService.getPredictionRecordsForVehicleAndTrip(vehicleId, tripStatus.getActiveTrip().getId());
        }
        return _transitDataService.getPredictionRecordsForTrip(vehicleId, tripStatus);
    }

    public List<TimepointPredictionRecord> getPredictionRecordsForVehicleAndTrip(String vehicleId,
                                                                                 String tripId) {
        blockUntilBundleIsReady();
        if (_predictionIntegrationService != null && _predictionIntegrationService.isEnabled()) {
            return _predictionIntegrationService.getPredictionRecordsForVehicleAndTrip(vehicleId, tripId);
        }
        return Collections.emptyList();
    }

    @Override
    public String getActiveBundleId() {
        blockUntilBundleIsReady();

        BundleItem currentBundle = _bundleManagementService.getCurrentBundleMetadata();

        if(currentBundle != null) {
            return currentBundle.getId();
        } else {
            return null;
        }
    }

    @Override
    public BundleMetadata getBundleMetadata() {
        return null;
    }

    @Override
    public Boolean routeHasUpcomingScheduledService(String agencyId, long time, String routeId, String directionId)  {
        blockUntilBundleIsReady();
        return _transitDataService.routeHasUpcomingScheduledService(agencyId, time, routeId, directionId);
    }

    @Override
    public Boolean stopHasUpcomingScheduledService(String agencyId, long time, String stopId, String routeId, String directionId) {
        blockUntilBundleIsReady();
        return _transitDataService.stopHasUpcomingScheduledService(agencyId, time, stopId, routeId, directionId);
    }

    @Override
    public List<String> getSearchSuggestions(String agencyId, String input) {
        List<String> result = this._bundleSearchService.getSuggestions(input);

        return result;
    }

    /****
     * {@link TransitDataService} Interface
     ****/

    @Override
    public Map<String, List<CoordinateBounds>> getAgencyIdsWithCoverageArea() {
        blockUntilBundleIsReady();
        return _transitDataService.getAgencyIdsWithCoverageArea();
    }

    @Override
    public void cancelAlarmForArrivalAndDepartureAtStop(String arg0) {
        blockUntilBundleIsReady();
        _transitDataService.cancelAlarmForArrivalAndDepartureAtStop(arg0);
    }

    @Override
    public ServiceAlertBean createServiceAlert(String arg0, ServiceAlertBean arg1) {
        blockUntilBundleIsReady();
        return _transitDataService.createServiceAlert(arg0, arg1);
    }

    @Override
    public void deleteStopProblemReportForStopIdAndId(String arg0, long arg1) {
        blockUntilBundleIsReady();
        _transitDataService.deleteStopProblemReportForStopIdAndId(arg0, arg1);
    }

    @Override
    public void deleteTripProblemReportForTripIdAndId(String arg0, long arg1) {
        blockUntilBundleIsReady();
        _transitDataService.deleteStopProblemReportForStopIdAndId(arg0, arg1);
    }

    @Override
    public List<AgencyWithCoverageBean> getAgenciesWithCoverage()
            throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getAgenciesWithCoverage();
    }

    @Override
    public AgencyBean getAgency(String arg0) throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getAgency(arg0);
    }

    @Override
    public ListBean<ServiceAlertBean> getAllServiceAlertsForAgencyId(String arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getAllServiceAlertsForAgencyId(arg0);
    }

    @Override
    public List<StopProblemReportBean> getAllStopProblemReportsForStopId(
            String arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getAllStopProblemReportsForStopId(arg0);
    }

    @Override
    public List<String> getAllTripProblemReportLabels() {
        blockUntilBundleIsReady();
        return _transitDataService.getAllTripProblemReportLabels();
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<TripProblemReportBean> getAllTripProblemReportsForTripId(
            String arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getAllTripProblemReportsForTripId(arg0);
    }

    @Override
    public ListBean<VehicleStatusBean> getAllVehiclesForAgency(String arg0,
                                                               long arg1) {
        blockUntilBundleIsReady();
        return _transitDataService.getAllVehiclesForAgency(arg0, arg1);
    }

    @Override
    public ArrivalAndDepartureBean getArrivalAndDepartureForStop(
            ArrivalAndDepartureForStopQueryBean arg0) throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getArrivalAndDepartureForStop(arg0);
    }

    @Override
    public BlockBean getBlockForId(String arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getBlockForId(arg0);
    }

    @Override
    public BlockInstanceBean getBlockInstance(String arg0, long arg1) {
        blockUntilBundleIsReady();
        return _transitDataService.getBlockInstance(arg0, arg1);
    }

    @Override
    public ListBean<CurrentVehicleEstimateBean> getCurrentVehicleEstimates(
            CurrentVehicleEstimateQueryBean arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getCurrentVehicleEstimates(arg0);
    }

    @Override
    public List<OccupancyStatusBean> getHistoricalRidershipForStop(HistoricalOccupancyByStopQueryBean query) {
        blockUntilBundleIsReady();
        return _transitDataService.getHistoricalRidershipForStop(query);
    }

    @Override
    public List<OccupancyStatusBean> getAllHistoricalRiderships(long serviceDate) {
        blockUntilBundleIsReady();
        return _transitDataService.getAllHistoricalRiderships(serviceDate);
    }

    @Override
    public List<OccupancyStatusBean> getHistoricalRidershipsForTrip(AgencyAndId tripId, long serviceDate) {
        blockUntilBundleIsReady();
        return _transitDataService.getHistoricalRidershipsForTrip(tripId, serviceDate);
    }

    @Override
    public List<OccupancyStatusBean> getHistoricalRidershipsForRoute(AgencyAndId routeId, long serviceDate) {
        blockUntilBundleIsReady();
        return _transitDataService.getHistoricalRidershipsForRoute(routeId,serviceDate);
    }

    @Override
    public List<OccupancyStatusBean> getHistoricalRiderships(AgencyAndId routeId, AgencyAndId tripId, AgencyAndId stopId, long serviceDate) {
        blockUntilBundleIsReady();
        return _transitDataService.getHistoricalRiderships(routeId, tripId, stopId, serviceDate);
    }

    @Override
    public RouteBean getRouteForId(String arg0) throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getRouteForId(arg0);
    }

    @Override
    public ListBean<String> getRouteIdsForAgencyId(String arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getRouteIdsForAgencyId(arg0);
    }

    @Override
    public RoutesBean getRoutes(SearchQueryBean arg0) throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getRoutes(arg0);
    }

    @Override
    public ListBean<RouteBean> getRoutesForAgencyId(String arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getRoutesForAgencyId(arg0);
    }

    @Override
    public StopScheduleBean getScheduleForStop(String arg0, Date arg1)
            throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getScheduleForStop(arg0, arg1);
    }

    @Override
    public ScheduledBlockLocationBean getScheduledBlockLocationFromScheduledTime(
            String arg0, long arg1, int arg2) {
        blockUntilBundleIsReady();
        return _transitDataService.getScheduledBlockLocationFromScheduledTime(arg0, arg1, arg2);
    }

    @Override
    public List<BlockInstanceBean> getActiveBlocksForRoute(AgencyAndId agencyAndId, long l, long l1) {
        blockUntilBundleIsReady();
        return _transitDataService.getActiveBlocksForRoute(agencyAndId, l, l1);
    }

    @Override
    public ServiceAlertBean getServiceAlertForId(String arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getServiceAlertForId(arg0);
    }

    @Override
    public ListBean<ServiceAlertBean> getServiceAlerts(SituationQueryBean arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getServiceAlerts(arg0);
    }

    @Override
    public EncodedPolylineBean getShapeForId(String arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getShapeForId(arg0);
    }

    @Override
    public ListBean<String> getShapeIdsForAgencyId(String arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getShapeIdsForAgencyId(arg0);
    }

    @Override
    public TripDetailsBean getSingleTripDetails(TripDetailsQueryBean arg0)
            throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getSingleTripDetails(arg0);
    }

    @Override
    public StopBean getStop(String arg0) throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getStop(arg0);
    }

    @Override
    public StopBean getStopForServiceDate(String stopId, ServiceDate serviceDate) throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getStopForServiceDate(stopId, serviceDate);
    }

    @Override
    public ListBean<String> getStopIdsForAgencyId(String arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getStopIdsForAgencyId(arg0);
    }

    @Override
    public StopProblemReportBean getStopProblemReportForStopIdAndId(String arg0,
                                                                    long arg1) {
        blockUntilBundleIsReady();
        return _transitDataService.getStopProblemReportForStopIdAndId(arg0, arg1);
    }

    @Override
    public ListBean<StopProblemReportSummaryBean> getStopProblemReportSummaries(
            StopProblemReportQueryBean arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getStopProblemReportSummaries(arg0);
    }

    @Override
    public ListBean<StopProblemReportBean> getStopProblemReports(
            StopProblemReportQueryBean arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getStopProblemReports(arg0);
    }

    @Override
    public StopWithArrivalsAndDeparturesBean getStopWithArrivalsAndDepartures(
            String arg0, ArrivalsAndDeparturesQueryBean arg1) throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getStopWithArrivalsAndDepartures(arg0, arg1);
    }

    @Override
    public StopsBean getStops(SearchQueryBean arg0) throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getStops(arg0);
    }

    @Override
    public StopsBean getStopsByName(String stopName) throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getStopsByName(stopName);
    }

    @Override
    public StopsForRouteBean getStopsForRoute(String arg0)
            throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getStopsForRoute(arg0);
    }

    @Override
    public StopsForRouteBean getStopsForRouteForServiceDate(String routeId, ServiceDate serviceDate) throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getStopsForRouteForServiceDate(routeId, serviceDate);
    }

    @Override
    public StopsWithArrivalsAndDeparturesBean getStopsWithArrivalsAndDepartures(
            Collection<String> arg0, ArrivalsAndDeparturesQueryBean arg1)
            throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getStopsWithArrivalsAndDepartures(arg0, arg1);
    }

    @Override
    public TripBean getTrip(String arg0) throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getTrip(arg0);
    }

    @Override
    public ListBean<TripDetailsBean> getTripDetails(TripDetailsQueryBean arg0)
            throws ServiceException {
        blockUntilBundleIsReady();
        return _transitDataService.getTripDetails(arg0);
    }

    @Override
    public TripDetailsBean getTripDetailsForVehicleAndTime(
            TripForVehicleQueryBean arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getTripDetailsForVehicleAndTime(arg0);
    }

    @Override
    public TripProblemReportBean getTripProblemReportForTripIdAndId(String arg0,
                                                                    long arg1) {
        blockUntilBundleIsReady();
        return _transitDataService.getTripProblemReportForTripIdAndId(arg0, arg1);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ListBean<TripProblemReportSummaryBean> getTripProblemReportSummaries(
            TripProblemReportQueryBean arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getTripProblemReportSummaries(arg0);
    }

    @Override
    public ListBean<TripProblemReportBean> getTripProblemReports(
            TripProblemReportQueryBean arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getTripProblemReports(arg0);
    }

    @Override
    public ListBean<TripDetailsBean> getTripsForAgency(
            TripsForAgencyQueryBean arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getTripsForAgency(arg0);
    }

    @Override
    public ListBean<TripDetailsBean> getTripsForBounds(
            TripsForBoundsQueryBean arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getTripsForBounds(arg0);
    }

    @Override
    public ListBean<TripDetailsBean> getTripsForRoute(TripsForRouteQueryBean arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getTripsForRoute(arg0);
    }

    @Override
    public VehicleStatusBean getVehicleForAgency(String arg0, long arg1) {
        blockUntilBundleIsReady();
        return _transitDataService.getVehicleForAgency(arg0, arg1);
    }

    @Override
    public VehicleLocationRecordBean getVehicleLocationRecordForVehicleId(
            String arg0, long arg1) {
        blockUntilBundleIsReady();
        return _transitDataService.getVehicleLocationRecordForVehicleId(arg0, arg1);
    }

    @Override
    public VehicleLocationRecordBean getVehiclePositionForVehicleId(String vehicleId) {
        blockUntilBundleIsReady();
        return _transitDataService.getVehiclePositionForVehicleId(vehicleId);
    }

    @Override
    public ListBean<VehicleLocationRecordBean> getVehicleLocationRecords(
            VehicleLocationRecordQueryBean arg0) {
        blockUntilBundleIsReady();
        return _transitDataService.getVehicleLocationRecords(arg0);
    }

    @Override
    public String registerAlarmForArrivalAndDepartureAtStop(
            ArrivalAndDepartureForStopQueryBean arg0, RegisterAlarmQueryBean arg1) {
        blockUntilBundleIsReady();
        return _transitDataService.registerAlarmForArrivalAndDepartureAtStop(arg0,  arg1);
    }

    @Override
    public void removeAllServiceAlertsForAgencyId(String arg0) {
        blockUntilBundleIsReady();
        _transitDataService.removeAllServiceAlertsForAgencyId(arg0);
    }

    @Override
    public void removeServiceAlert(String arg0) {
        blockUntilBundleIsReady();
        _transitDataService.removeServiceAlert(arg0);
    }

    @Override
    public void reportProblemWithStop(StopProblemReportBean stopProblemReport) {
        blockUntilBundleIsReady();
        _transitDataService.reportProblemWithStop(stopProblemReport);
    }

    @Override
    public void reportProblemWithTrip(TripProblemReportBean tripProblemReport) {
        blockUntilBundleIsReady();
        _transitDataService.reportProblemWithTrip(tripProblemReport);
    }

    @Override
    public void resetVehicleLocation(String arg0) {
        blockUntilBundleIsReady();
        _transitDataService.resetVehicleLocation(arg0);
    }

    @Override
    public void addVehicleOccupancyRecord(VehicleOccupancyRecord vehicleOccupancyRecord) {
        blockUntilBundleIsReady();
        _transitDataService.addVehicleOccupancyRecord(vehicleOccupancyRecord);
    }


    @Override
    public VehicleOccupancyRecord getLastVehicleOccupancyRecordForVehicleId(AgencyAndId vehicleId) {
        blockUntilBundleIsReady();
        return _transitDataService.getLastVehicleOccupancyRecordForVehicleId(vehicleId);
    }

    @Override
    public VehicleOccupancyRecord getVehicleOccupancyRecordForVehicleIdAndRoute(AgencyAndId vehicleId, String routeId, String directionId) {
        blockUntilBundleIsReady();
        return _transitDataService.getVehicleOccupancyRecordForVehicleIdAndRoute(vehicleId, routeId, directionId);
    }

    @Override
    public void submitVehicleLocation(VehicleLocationRecordBean arg0) {
        blockUntilBundleIsReady();
        _transitDataService.submitVehicleLocation(arg0);
    }

    @Override
    public void updateServiceAlert(ServiceAlertBean arg0) {
        blockUntilBundleIsReady();
        _transitDataService.updateServiceAlert(arg0);
    }

    @Override
    public ServiceAlertBean copyServiceAlert(String agencyId, ServiceAlertBean situation) {
        blockUntilBundleIsReady();
        return _transitDataService.copyServiceAlert(agencyId, situation);
    }

    @Override
    public void updateTripProblemReport(TripProblemReportBean tripProblemReport) {
        blockUntilBundleIsReady();
        _transitDataService.updateTripProblemReport(tripProblemReport);
    }

    @Override
    public ListBean<TripProblemReportSummaryBean> getTripProblemReportSummariesByGrouping(
            TripProblemReportQueryBean query, ETripProblemGroupBy groupBy) {
        blockUntilBundleIsReady();
        return _transitDataService.getTripProblemReportSummariesByGrouping(query, groupBy);
    }

    @Override
    public Boolean stopHasRevenueServiceOnRoute(String agencyId, String stopId, String routeId, String directionId) {
        blockUntilBundleIsReady();
        return _transitDataService.stopHasRevenueServiceOnRoute(agencyId, stopId, routeId, directionId);
    }

    @Override
    public Boolean stopHasRevenueService(String agencyId, String stopId) {
        blockUntilBundleIsReady();
        return _transitDataService.stopHasRevenueService(agencyId, stopId);
    }

    @Override
    public List<StopBean> getAllRevenueStops(AgencyWithCoverageBean agency) {
        blockUntilBundleIsReady();
        return _transitDataService.getAllRevenueStops(agency);
    }

    @Override
    public ListBean<ConsolidatedStopMapBean> getAllConsolidatedStops() {
        blockUntilBundleIsReady();
        return _transitDataService.getAllConsolidatedStops();
    }

    @Override
    public ListBean<ServiceAlertRecordBean> getAllServiceAlertRecordsForAgencyId(String agencyId) {
        blockUntilBundleIsReady();
        return _transitDataService.getAllServiceAlertRecordsForAgencyId(agencyId);
    }

    @Override
    public boolean isTripCancelled(String tripId){
        blockUntilBundleIsReady();
        if(_cancelledTripService != null) {
            return _cancelledTripService.isTripCancelled(tripId);
        }
        return false;
    }

    @Override
    public boolean isTripCancelled(AgencyAndId tripId){
        blockUntilBundleIsReady();
        if(_cancelledTripService != null){
            return _cancelledTripService.isTripCancelled(tripId);
        }
        return false;
    }

    @Override
    public ListBean<CancelledTripBean> getAllCancelledTrips(){
        blockUntilBundleIsReady();
        if(_cancelledTripService != null){
            return _cancelledTripService.getAllCancelledTrips();
        }
        return new ListBean<>(Collections.EMPTY_LIST, false);
    }

    @Override
    public void overrideCancelledTrips(List<CancelledTripBean> beans){
        blockUntilBundleIsReady();
        if(_cancelledTripService != null){
            Map<AgencyAndId, CancelledTripBean> cancelledTripsCache = new ConcurrentHashMap<>();
            for(CancelledTripBean bean : beans){
                cancelledTripsCache.put(AgencyAndId.convertFromString(bean.getTrip()),bean);
            }
            _cancelledTripService.updateCancelledTrips(cancelledTripsCache);
        }
    }
}
