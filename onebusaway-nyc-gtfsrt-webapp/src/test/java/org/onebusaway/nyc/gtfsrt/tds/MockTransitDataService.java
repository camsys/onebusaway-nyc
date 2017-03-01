package org.onebusaway.nyc.gtfsrt.tds;

import com.google.common.collect.Maps;
import org.onebusaway.exceptions.ServiceException;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.model.EncodedPolylineBean;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ShapePoint;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.onebusaway.nyc.gtfsrt.util.BlockTripMapReader;
import org.onebusaway.nyc.transit_data.services.NycTransitDataService;
import org.onebusaway.realtime.api.TimepointPredictionRecord;
import org.onebusaway.transit_data.model.AgencyBean;
import org.onebusaway.transit_data.model.AgencyWithCoverageBean;
import org.onebusaway.transit_data.model.ArrivalAndDepartureBean;
import org.onebusaway.transit_data.model.ArrivalAndDepartureForStopQueryBean;
import org.onebusaway.transit_data.model.ArrivalsAndDeparturesQueryBean;
import org.onebusaway.transit_data.model.ListBean;
import org.onebusaway.transit_data.model.RegisterAlarmQueryBean;
import org.onebusaway.transit_data.model.RouteBean;
import org.onebusaway.transit_data.model.RoutesBean;
import org.onebusaway.transit_data.model.SearchQueryBean;
import org.onebusaway.transit_data.model.StopBean;
import org.onebusaway.transit_data.model.StopScheduleBean;
import org.onebusaway.transit_data.model.StopWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.StopsBean;
import org.onebusaway.transit_data.model.StopsForRouteBean;
import org.onebusaway.transit_data.model.StopsWithArrivalsAndDeparturesBean;
import org.onebusaway.transit_data.model.TripStopTimeBean;
import org.onebusaway.transit_data.model.TripStopTimesBean;
import org.onebusaway.transit_data.model.VehicleStatusBean;
import org.onebusaway.transit_data.model.blocks.BlockBean;
import org.onebusaway.transit_data.model.blocks.BlockInstanceBean;
import org.onebusaway.transit_data.model.blocks.ScheduledBlockLocationBean;
import org.onebusaway.transit_data.model.oba.LocalSearchResult;
import org.onebusaway.transit_data.model.oba.MinTravelTimeToStopsBean;
import org.onebusaway.transit_data.model.oba.TimedPlaceBean;
import org.onebusaway.transit_data.model.problems.ETripProblemGroupBy;
import org.onebusaway.transit_data.model.problems.PlannedTripProblemReportBean;
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
import org.onebusaway.transit_data.model.service_alerts.SituationQueryBean;
import org.onebusaway.transit_data.model.tripplanning.ConstraintsBean;
import org.onebusaway.transit_data.model.tripplanning.ItinerariesBean;
import org.onebusaway.transit_data.model.tripplanning.TransitLocationBean;
import org.onebusaway.transit_data.model.tripplanning.TransitShedConstraintsBean;
import org.onebusaway.transit_data.model.tripplanning.VertexBean;
import org.onebusaway.transit_data.model.trips.TripBean;
import org.onebusaway.transit_data.model.trips.TripDetailsBean;
import org.onebusaway.transit_data.model.trips.TripDetailsQueryBean;
import org.onebusaway.transit_data.model.trips.TripForVehicleQueryBean;
import org.onebusaway.transit_data.model.trips.TripStatusBean;
import org.onebusaway.transit_data.model.trips.TripsForAgencyQueryBean;
import org.onebusaway.transit_data.model.trips.TripsForBoundsQueryBean;
import org.onebusaway.transit_data.model.trips.TripsForRouteQueryBean;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import java.io.File;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static org.junit.Assert.fail;

public class MockTransitDataService implements NycTransitDataService {

    private GtfsRelationalDao _dao;
    private Map<String, VehicleLocationRecordBean> _vehicleLocations;
    private BlockTripMap _tripsForBlock;

    public MockTransitDataService(String defaultAgencyId, String gtfsFile, String blockTripFile) {
        GtfsReader reader = new GtfsReader();
        GtfsRelationalDaoImpl dao = new GtfsRelationalDaoImpl();
        reader.setEntityStore(dao);

        try {
            URL resource = this.getClass().getResource("/" + gtfsFile);
            File file = new File(resource.toURI());
            reader.setInputLocation(file);
            reader.setDefaultAgencyId(defaultAgencyId);
            reader.run();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception when loading GTFS");
        }

        _dao = dao;
        _vehicleLocations = Maps.newHashMap();
        _tripsForBlock = new BlockTripMap(_dao);

        // set blocks in trip
        Map<String, String> tripIdToBlockId = new BlockTripMapReader().getTripToBlockMap(blockTripFile);
        for (Trip trip : _dao.getAllTrips()) {
            String blockId = tripIdToBlockId.get(trip.getId().toString());
            if (blockId != null)
                trip.setBlockId(blockId);
        }

    }

    @Override
    public List<TimepointPredictionRecord> getPredictionRecordsForVehicleAndTrip(String VehicleId, String TripId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean stopHasRevenueServiceOnRoute(String agencyId, String stopId, String routeId, String directionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean stopHasRevenueService(String agencyId, String stopId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AgencyWithCoverageBean> getAgenciesWithCoverage() throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AgencyBean getAgency(String s) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public RoutesBean getRoutes(SearchQueryBean searchQueryBean) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public RouteBean getRouteForId(String s) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<String> getRouteIdsForAgencyId(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<RouteBean> getRoutesForAgencyId(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StopsForRouteBean getStopsForRoute(String s) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TripBean getTrip(String s) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public TripDetailsBean getSingleTripDetails(TripDetailsQueryBean tripDetailsQueryBean) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<TripDetailsBean> getTripDetails(TripDetailsQueryBean tripDetailsQueryBean) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<TripDetailsBean> getTripsForBounds(TripsForBoundsQueryBean tripsForBoundsQueryBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<TripDetailsBean> getTripsForRoute(TripsForRouteQueryBean tripsForRouteQueryBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<TripDetailsBean> getTripsForAgency(TripsForAgencyQueryBean tripsForAgencyQueryBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlockBean getBlockForId(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BlockInstanceBean getBlockInstance(String s, long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ScheduledBlockLocationBean getScheduledBlockLocationFromScheduledTime(String s, long l, int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VehicleStatusBean getVehicleForAgency(String s, long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<VehicleStatusBean> getAllVehiclesForAgency(String s, long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VehicleLocationRecordBean getVehicleLocationRecordForVehicleId(String s, long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<VehicleLocationRecordBean> getVehicleLocationRecords(VehicleLocationRecordQueryBean vehicleLocationRecordQueryBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void submitVehicleLocation(VehicleLocationRecordBean vlrb) {
        _vehicleLocations.put(vlrb.getVehicleId(), vlrb);
    }

    @Override
    public void resetVehicleLocation(String s) {
        _vehicleLocations.remove(s);
    }

    @Override
    public TripDetailsBean getTripDetailsForVehicleAndTime(TripForVehicleQueryBean tripForVehicleQueryBean) {
        String vehicleId = tripForVehicleQueryBean.getVehicleId();
        VehicleLocationRecordBean vlrb = _vehicleLocations.get(vehicleId);
        if (vlrb == null)
            return null;

        AgencyAndId tripId = AgencyAndId.convertFromString(vlrb.getTripId());
        for (Trip trip : _dao.getAllTrips()) {
            if (trip.getId().equals(tripId)) {
                return tripDetailsBean(trip, vlrb);
            }
        }

        return null;
    }

    @Override
    public StopWithArrivalsAndDeparturesBean getStopWithArrivalsAndDepartures(String s, ArrivalsAndDeparturesQueryBean arrivalsAndDeparturesQueryBean) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public StopsWithArrivalsAndDeparturesBean getStopsWithArrivalsAndDepartures(Collection<String> collection, ArrivalsAndDeparturesQueryBean arrivalsAndDeparturesQueryBean) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ArrivalAndDepartureBean getArrivalAndDepartureForStop(ArrivalAndDepartureForStopQueryBean arrivalAndDepartureForStopQueryBean) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String registerAlarmForArrivalAndDepartureAtStop(ArrivalAndDepartureForStopQueryBean arrivalAndDepartureForStopQueryBean, RegisterAlarmQueryBean registerAlarmQueryBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void cancelAlarmForArrivalAndDepartureAtStop(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StopScheduleBean getScheduleForStop(String s, Date date) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public StopsBean getStops(SearchQueryBean searchQueryBean) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public StopBean getStop(String s) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<String> getStopIdsForAgencyId(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EncodedPolylineBean getShapeForId(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<String> getShapeIdsForAgencyId(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<CurrentVehicleEstimateBean> getCurrentVehicleEstimates(CurrentVehicleEstimateQueryBean currentVehicleEstimateQueryBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ItinerariesBean getItinerariesBetween(TransitLocationBean transitLocationBean, TransitLocationBean transitLocationBean1, long l, ConstraintsBean constraintsBean) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reportProblemWithPlannedTrip(TransitLocationBean transitLocationBean, TransitLocationBean transitLocationBean1, long l, ConstraintsBean constraintsBean, PlannedTripProblemReportBean plannedTripProblemReportBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<VertexBean> getStreetGraphForRegion(double v, double v1, double v2, double v3) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public MinTravelTimeToStopsBean getMinTravelTimeToStopsFrom(CoordinatePoint coordinatePoint, long l, TransitShedConstraintsBean transitShedConstraintsBean) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TimedPlaceBean> getLocalPaths(String s, ConstraintsBean constraintsBean, MinTravelTimeToStopsBean minTravelTimeToStopsBean, List<LocalSearchResult> list) throws ServiceException {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceAlertBean createServiceAlert(String s, ServiceAlertBean serviceAlertBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateServiceAlert(ServiceAlertBean serviceAlertBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeServiceAlert(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceAlertBean getServiceAlertForId(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<ServiceAlertBean> getAllServiceAlertsForAgencyId(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAllServiceAlertsForAgencyId(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<ServiceAlertBean> getServiceAlerts(SituationQueryBean situationQueryBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reportProblemWithStop(StopProblemReportBean stopProblemReportBean) {

    }

    @Override
    public ListBean<StopProblemReportSummaryBean> getStopProblemReportSummaries(StopProblemReportQueryBean stopProblemReportQueryBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<StopProblemReportBean> getStopProblemReports(StopProblemReportQueryBean stopProblemReportQueryBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<StopProblemReportBean> getAllStopProblemReportsForStopId(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StopProblemReportBean getStopProblemReportForStopIdAndId(String s, long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteStopProblemReportForStopIdAndId(String s, long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reportProblemWithTrip(TripProblemReportBean tripProblemReportBean) {

    }

    @Override
    public ListBean<TripProblemReportSummaryBean> getTripProblemReportSummaries(TripProblemReportQueryBean tripProblemReportQueryBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<TripProblemReportSummaryBean> getTripProblemReportSummariesByGrouping(TripProblemReportQueryBean tripProblemReportQueryBean, ETripProblemGroupBy eTripProblemGroupBy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListBean<TripProblemReportBean> getTripProblemReports(TripProblemReportQueryBean tripProblemReportQueryBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TripProblemReportBean> getAllTripProblemReportsForTripId(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TripProblemReportBean getTripProblemReportForTripIdAndId(String s, long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateTripProblemReport(TripProblemReportBean tripProblemReportBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteTripProblemReportForTripIdAndId(String s, long l) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getAllTripProblemReportLabels() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getActiveBundleId() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<TimepointPredictionRecord> getPredictionRecordsForTrip(String s, TripStatusBean tripStatusBean) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean routeHasUpcomingScheduledService(String s, long l, String s1, String s2) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean stopHasUpcomingScheduledService(String s, long l, String s1, String s2, String s3) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getSearchSuggestions(String s, String s1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, List<CoordinateBounds>> getAgencyIdsWithCoverageArea() {
        throw new UnsupportedOperationException();
    }

    // Helper functions

    private TripBean tripBean(Trip trip) {
        TripBean bean = new TripBean();
        bean.setBlockId(trip.getBlockId());
        bean.setDirectionId(trip.getDirectionId());
        bean.setId(trip.getId().getId());
        bean.setRouteShortName(trip.getRouteShortName());
        bean.setServiceId(trip.getServiceId().getId());
        bean.setShapeId(trip.getShapeId().getId());
        bean.setTripHeadsign(trip.getTripHeadsign());
        bean.setTripShortName(trip.getTripShortName());
        bean.setRoute(routeBean(trip.getRoute()));
        // need this? bean.setTotalTripDistance();
        return bean;
    }

    private TripDetailsBean tripDetailsBean(Trip trip, VehicleLocationRecordBean vlrb) {
        TripDetailsBean bean = new TripDetailsBean();
        TripBean tripBean = tripBean(trip);
        bean.setTrip(tripBean);
        bean.setTripId(trip.getId().getId());
        bean.setServiceDate(vlrb.getServiceDate());
        bean.setStatus(tripStatusBean(trip, tripBean, vlrb));
        bean.setSchedule(createScheduleForTrip(trip));
        return bean;
    }

    private RouteBean routeBean(Route route) {
        RouteBean.Builder builder = RouteBean.builder();
        builder.setId(route.getId().toString());
        builder.setColor(route.getColor());
        builder.setDescription(route.getDesc());
        builder.setLongName(route.getLongName());
        builder.setShortName(route.getShortName());
        builder.setType(route.getType());
        builder.setUrl(route.getUrl());
        return builder.create();
    }

    private TripStatusBean tripStatusBean(Trip trip, TripBean tripBean, VehicleLocationRecordBean vlrb) {
        TripStatusBean status = new TripStatusBean();
        status.setServiceDate(vlrb.getServiceDate());
        status.setActiveTrip(tripBean);
        status.setVehicleId(vlrb.getVehicleId());
        long time = vlrb.getTimeOfLocationUpdate();
        if (time <= 0)
            time = vlrb.getTimeOfRecord();
        status.setLastUpdateTime(time);
        status.setScheduleDeviation(vlrb.getScheduleDeviation());
        // distance along block. need block data structure
        status.setLocation(vlrb.getCurrentLocation());
        status.setOrientation(vlrb.getCurrentOrientation());
        status.setStatus(vlrb.getStatus());
        status.setPhase(vlrb.getPhase());

        double distanceAlongTrip = vlrb.getDistanceAlongBlock() - getAccumulatedBlockDistanceForTrip(trip);
        status.setDistanceAlongTrip(distanceAlongTrip);

        // set next stop
        Iterator<StopTime> stopTimes = _dao.getStopTimesForTrip(trip).iterator();
        int seq = -1;
        double distance = 0;
        StopTime st = null;
        while (stopTimes.hasNext()) {
            st = stopTimes.next();
            if (seq >= st.getStopSequence())
                throw new IllegalArgumentException("bad stop times: not in sequence");
            if (!st.isShapeDistTraveledSet())
                throw new IllegalArgumentException("bad stop times: no shape dist");
            seq = st.getStopSequence();
            distance += st.getShapeDistTraveled();
            if (distance > distanceAlongTrip)
              break;
        }
        if (st != null)
            status.setNextStop(stopBean(st.getStop()));

        return status;
    }

    private StopBean stopBean(Stop stop) {
        StopBean bean = new StopBean();
        bean.setId(stop.getId().toString());
        bean.setName(stop.getName());
        return bean;
    }

    private TripStopTimesBean createScheduleForTrip(Trip trip) {
        TripStopTimesBean schedule = new TripStopTimesBean();
        for (StopTime stopTime : _dao.getStopTimesForTrip(trip)) {
            TripStopTimeBean bean = new TripStopTimeBean();
            bean.setArrivalTime(stopTime.getArrivalTime());
            bean.setDepartureTime(stopTime.getDepartureTime());
            bean.setDistanceAlongTrip(stopTime.getShapeDistTraveled()); // todo units
            schedule.addStopTime(bean);
        }
        return schedule;
    }

    private double getAccumulatedBlockDistanceForTrip(Trip trip) {
        double distance = 0;
        SortedSet<? extends TripEntry> trips = _tripsForBlock.getOrderedBlockTripsForTrip(trip);
        for (TripEntry t : trips) {
            if (t.getId().equals(trip.getId()))
                break;
            distance += t.getTotalTripDistance();
        }
        return distance;
    }

}
