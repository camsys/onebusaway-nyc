package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.LocalizedServiceId;
import org.onebusaway.gtfs.model.calendar.ServiceInterval;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Gaussian;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.transit_data_federation.services.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.realtime.TripLocation;
import org.onebusaway.transit_data_federation.services.realtime.TripLocationService;
import org.onebusaway.transit_data_federation.services.tripplanner.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.TripEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.TripInstanceProxy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class TripsFromObservationServiceImpl implements TripsFromObservationService {

  private TransitGraphDao _transitGraphDao;

  private CalendarService _calendarService;

  private TripLocationService _tripPositionService;

  private DestinationSignCodeService _destinationSignCodeService;

  /**
   * Default is 800 meters
   */
  private double _tripSearchRadius = 800;

  /**
   * Default is 30 minutes
   */
  private long _tripSearchTimeBeforeFirstStop = 30 * 60 * 1000;

  /**
   * Default is 30 minutes
   */
  private long _tripSearchTimeAfteLastStop = 30 * 60 * 1000;

  /**
   * What weight do we assign to a destination sign code trip?
   */
  private double _dscTripProbability = 0.5;

  /**
   * We need some way of scoring nearby trips
   */
  private Gaussian _nearbyTripSigma = new Gaussian(0, 400);

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  @Autowired
  public void setCalendarService(CalendarService calendarService) {
    _calendarService = calendarService;
  }

  @Autowired
  public void setTripPositionService(TripLocationService tripPositionService) {
    _tripPositionService = tripPositionService;
  }

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  /**
   * 
   * @param tripSearchRadius in meters
   */
  public void setTripSearchRadius(double tripSearchRadius) {
    _tripSearchRadius = tripSearchRadius;
  }

  public void setTripSearchTimeBeforeFirstStop(
      long tripSearchTimeBeforeFirstStop) {
    _tripSearchTimeBeforeFirstStop = tripSearchTimeBeforeFirstStop;
  }

  public void setTripSearchTimeAfterLastStop(long tripSearchTimeAfteLastStop) {
    _tripSearchTimeAfteLastStop = tripSearchTimeAfteLastStop;
  }

  @Override
  public CDFMap<TripInstanceProxy> determinePotentialTripsForObservation(
      Observation observation) {

    CDFMap<TripInstanceProxy> potentialTrips = new CDFMap<TripInstanceProxy>();

    /**
     * First source of trips: the destination sign code
     */
    computePotentialTripsFromDestinationSignCode(observation, potentialTrips);

    /**
     * Second source of trips: trips nearby the current gps location
     */
    computeNearbyTrips(observation, potentialTrips);

    return potentialTrips;
  }

  private void computePotentialTripsFromDestinationSignCode(
      Observation observation, CDFMap<TripInstanceProxy> cdf) {

    NycVehicleLocationRecord record = observation.getRecord();
    long time = record.getTime();
    String dsc = record.getDestinationSignCode();

    /**
     * Step 1: Figure out the set of all possible trip ids given the destination
     * sign code
     */
    List<AgencyAndId> dscTripIds = _destinationSignCodeService.getTripIdsForDestinationSignCode(dsc);

    /**
     * Step 2: Figure out which trips are actively running at the specified
     * time. We do that by looking at the range of stop times for each trip +/-
     * some extra buffer time to deal with the fact that it takes time for the
     * bus to get from the base to the first stop, and from the last stop back
     * to the base.
     */

    /**
     * Slight wrinkle: instead of expanding the stop time interval with the
     * from-base + to+base fudge factors, we just expand the current search time
     * interval instead.
     */
    Date timeFrom = new Date(time - _tripSearchTimeAfteLastStop);
    Date timeTo = new Date(time + _tripSearchTimeBeforeFirstStop);

    for (AgencyAndId tripId : dscTripIds) {

      /**
       * Only consider a trip if it exists and has stop times
       */
      TripEntry trip = _transitGraphDao.getTripEntryForId(tripId);
      if (trip == null)
        continue;
      List<StopTimeEntry> stopTimes = trip.getStopTimes();
      if (stopTimes.isEmpty())
        continue;

      LocalizedServiceId serviceId = _calendarService.getLocalizedServiceIdForAgencyAndServiceId(
          tripId.getAgencyId(), trip.getServiceId());

      /**
       * Construct the stop time interval
       */
      StopTimeEntry first = stopTimes.get(0);
      StopTimeEntry last = stopTimes.get(stopTimes.size() - 1);
      ServiceInterval interval = new ServiceInterval(first.getArrivalTime(),
          first.getDepartureTime(), last.getArrivalTime(),
          last.getDepartureTime());

      /**
       * This is where we actually compute the applicable trip instances
       */
      List<Date> serviceDates = _calendarService.getServiceDatesWithinRange(
          serviceId, interval, timeFrom, timeTo);

      for (Date serviceDate : serviceDates) {
        TripInstanceProxy tripInstance = new TripInstanceProxy(trip,
            serviceDate.getTime());
        cdf.put(_dscTripProbability, tripInstance);
      }
    }
  }

  private void computeNearbyTrips(Observation observation,
      CDFMap<TripInstanceProxy> cdf) {

    NycVehicleLocationRecord record = observation.getRecord();
    long time = record.getTime();

    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(
        record.getLatitude(), record.getLongitude(), _tripSearchRadius);

    Map<TripInstanceProxy, TripLocation> tripInstances = _tripPositionService.getScheduledTripsForBounds(
        bounds, time);

    for (Map.Entry<TripInstanceProxy, TripLocation> entry : tripInstances.entrySet()) {
      TripInstanceProxy tripInstance = entry.getKey();
      TripLocation position = entry.getValue();
      CoordinatePoint point = position.getLocation();
      double d = SphericalGeometryLibrary.distance(point.getLat(),
          point.getLon(), record.getLatitude(), record.getLongitude());
      double probability = _nearbyTripSigma.getProbability(d);
      cdf.put(probability, tripInstance);
    }

  }
}
