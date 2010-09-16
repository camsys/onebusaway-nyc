package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.collections.MappingLibrary;
import org.onebusaway.collections.Min;
import org.onebusaway.geospatial.model.CoordinateBounds;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.model.XYPoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.geospatial.services.UTMLibrary;
import org.onebusaway.geospatial.services.UTMProjection;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Gaussian;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.transit_data_federation.impl.shapes.PointAndIndex;
import org.onebusaway.transit_data_federation.impl.shapes.ShapePointsLibrary;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.ShapePointService;
import org.onebusaway.transit_data_federation.services.TransitGraphDao;
import org.onebusaway.transit_data_federation.services.realtime.ActiveCalendarService;
import org.onebusaway.transit_data_federation.services.realtime.BlockInstance;
import org.onebusaway.transit_data_federation.services.realtime.BlockLocation;
import org.onebusaway.transit_data_federation.services.realtime.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.realtime.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.tripplanner.BlockEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.TripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class BlocksFromObservationServiceImpl implements BlocksFromObservationService {

  private static Logger _log = LoggerFactory.getLogger(BlocksFromObservationServiceImpl.class);

  private TransitGraphDao _transitGraphDao;

  private ActiveCalendarService _activeCalendarService;

  private DestinationSignCodeService _destinationSignCodeService;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private ShapePointService _shapePointService;

  private ShapePointsLibrary _shapePointsLibrary = new ShapePointsLibrary();

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
   * We need some way of scoring nearby trips
   */
  private Gaussian _nearbyTripSigma = new Gaussian(0, 400);

  private Gaussian _scheduleDeviationSigma = new Gaussian(0, 32 * 60);

  /****
   * Public Methods
   ****/

  @Autowired
  public void setTransitGraphDao(TransitGraphDao transitGraphDao) {
    _transitGraphDao = transitGraphDao;
  }

  @Autowired
  public void setActiveCalendarService(
      ActiveCalendarService activeCalendarService) {
    _activeCalendarService = activeCalendarService;
  }

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  @Autowired
  public void setScheduledBlockService(
      ScheduledBlockLocationService scheduledBlockLocationService) {
    _scheduledBlockLocationService = scheduledBlockLocationService;
  }

  @Autowired
  public void setShapePointService(ShapePointService shapePointService) {
    _shapePointService = shapePointService;
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

  public void setLocalMinimumThreshold(double localMinimumThreshold) {
    _shapePointsLibrary.setLocalMinimumThreshold(localMinimumThreshold);
  }

  /**
   * 
   * @param scheduleDeviationSigma time, in seconds
   */
  public void setScheduleDeviationSigma(int scheduleDeviationSigma) {
    _scheduleDeviationSigma = new Gaussian(0, scheduleDeviationSigma);
  }

  /****
   * {@link BlocksFromObservationService} Interface
   ****/

  @Override
  public CDFMap<BlockState> determinePotentialBlocksForObservation(
      Observation observation) {

    CDFMap<BlockState> potentialBlocks = new CDFMap<BlockState>();

    /**
     * First source of trips: the destination sign code
     */
    computePotentialBlocksFromDestinationSignCode(observation, potentialBlocks);

    /**
     * Second source of trips: trips nearby the current gps location
     */
    computeNearbyBlocks(observation, potentialBlocks);

    return potentialBlocks;
  }

  @Override
  public BlockState advanceState(long timestamp, ProjectedPoint targetPoint,
      BlockState blockState, double maxDistanceToTravel) {

    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    double currentDistanceAlongBlock = blockLocation.getDistanceAlongBlock();

    return getBestBlockLocation(timestamp, targetPoint,
        blockState.getBlockInstance(), currentDistanceAlongBlock,
        currentDistanceAlongBlock + maxDistanceToTravel);
  }

  /*****
   * Private Methods
   * 
   * @param observation
   * @param cdf
   */

  private void computePotentialBlocksFromDestinationSignCode(
      Observation observation, CDFMap<BlockState> cdf) {

    NycVehicleLocationRecord record = observation.getRecord();
    long time = record.getTime();
    String dsc = record.getDestinationSignCode();

    /**
     * Step 1: Figure out the set of all possible trip ids given the destination
     * sign code
     */
    List<AgencyAndId> dscTripIds = _destinationSignCodeService.getTripIdsForDestinationSignCode(dsc);

    if (dscTripIds == null) {
      _log.warn("no trips found for dsc: " + dsc);
      return;
    }

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
    long timeFrom = time - _tripSearchTimeAfteLastStop;
    long timeTo = time + _tripSearchTimeBeforeFirstStop;

    Set<BlockInstance> blockInstances = new HashSet<BlockInstance>();

    for (AgencyAndId tripId : dscTripIds) {

      /**
       * Only consider a trip if it exists and has stop times
       */
      TripEntry trip = _transitGraphDao.getTripEntryForId(tripId);
      if (trip == null)
        continue;

      // TODO : Are we getting back more than we want as blocks will be active
      // longer than their corresponding trips?
      List<BlockInstance> instances = _activeCalendarService.getActiveBlocks(
          trip.getBlock().getId(), timeFrom, timeTo);
      blockInstances.addAll(instances);
    }

    System.out.println(blockInstances.size());

    for (BlockInstance blockInstance : blockInstances) {
      BlockState state = getBestBlockLocation(time, observation.getPoint(),
          blockInstance, 0, Double.POSITIVE_INFINITY);
      double p = scoreState(state, observation);
      cdf.put(p, state);
      System.out.println(blockInstance + "\t"
          + state.getBlockLocation().getDistanceAlongBlock() + "\t"
          + state.getBlockLocation().getScheduledTime() + "\t" + p);
    }
  }

  private void computeNearbyBlocks(Observation observation,
      CDFMap<BlockState> cdf) {

    NycVehicleLocationRecord record = observation.getRecord();
    long time = record.getTime();

    CoordinateBounds bounds = SphericalGeometryLibrary.bounds(
        record.getLatitude(), record.getLongitude(), _tripSearchRadius);

    // TODO : Think about this
    Map<BlockInstance, BlockLocation> blockInstances = new HashMap<BlockInstance, BlockLocation>();

    for (Map.Entry<BlockInstance, BlockLocation> entry : blockInstances.entrySet()) {
      BlockInstance blockInstance = entry.getKey();
      BlockState blockState = getBestBlockLocation(time,
          observation.getPoint(), blockInstance, 0, Double.POSITIVE_INFINITY);

      double p = scoreState(blockState, observation);
      cdf.put(p, blockState);
    }
  }

  private BlockState getBestBlockLocation(long timestamp,
      ProjectedPoint targetPoint, BlockInstance blockInstance,
      double blockDistanceFrom, double blockDistanceTo) {

    BlockEntry block = blockInstance.getBlock();

    ShapePoints shapePoints = getShapePointsForBlock(block);

    UTMProjection projection = UTMLibrary.getProjectionForPoint(
        shapePoints.getLats()[0], shapePoints.getLons()[0]);

    List<XYPoint> projectedShapePoints = _shapePointsLibrary.getProjectedShapePoints(
        shapePoints, projection);

    if (shapePoints == null || shapePoints.getSize() == 0)
      throw new IllegalStateException("block had no shape points: "
          + block.getId());

    double[] distances = shapePoints.getDistTraveled();

    int fromIndex = 0;
    int toIndex = distances.length;

    if (blockDistanceFrom > 0) {
      fromIndex = Arrays.binarySearch(distances, blockDistanceFrom);
      if (fromIndex < 0) {
        fromIndex = -(fromIndex + 1);
        // Include the previous point if we didn't get an exact match
        if (fromIndex > 0)
          fromIndex--;
      }
    }

    if (blockDistanceTo < distances[distances.length - 1]) {
      toIndex = Arrays.binarySearch(distances, blockDistanceTo);
      if (toIndex < 0) {
        toIndex = -(toIndex + 1);
        // Include the previous point if we didn't get an exact match
        if (toIndex < distances.length)
          toIndex++;
      }
    }

    XYPoint xyPoint = new XYPoint(targetPoint.getX(), targetPoint.getY());

    List<PointAndIndex> assignments = _shapePointsLibrary.computePotentialAssignments(
        projectedShapePoints, distances, xyPoint, fromIndex, toIndex);

    if (assignments.size() == 0) {
      return getAsState(blockInstance, blockDistanceFrom);
    } else if (assignments.size() == 1) {
      PointAndIndex pIndex = assignments.get(0);
      return getAsState(blockInstance, pIndex.distanceAlongShape);
    }

    Min<PointAndIndex> best = new Min<PointAndIndex>();

    for (PointAndIndex index : assignments) {

      double distanceAlongBlock = index.distanceAlongShape;

      ScheduledBlockLocation location = _scheduledBlockLocationService.getScheduledBlockPositionFromDistanceAlongBlock(
          block.getStopTimes(), distanceAlongBlock);

      int scheduledTime = location.getScheduledTime();
      long scheduleTimestamp = blockInstance.getServiceDate() + scheduledTime
          * 1000;

      double delta = Math.abs(scheduleTimestamp - timestamp);
      best.add(delta, index);
    }

    PointAndIndex index = best.getMinElement();
    return getAsState(blockInstance, index.distanceAlongShape);
  }

  private ShapePoints getShapePointsForBlock(BlockEntry block) {
    List<AgencyAndId> shapePointIds = MappingLibrary.map(block.getTrips(),
        "shapeId");
    return _shapePointService.getShapePointsForShapeIds(shapePointIds);
  }

  private BlockState getAsState(BlockInstance blockInstance,
      double distanceAlongBlock) {
    BlockEntry block = blockInstance.getBlock();
    ScheduledBlockLocation blockLocation = _scheduledBlockLocationService.getScheduledBlockPositionFromDistanceAlongBlock(
        block.getStopTimes(), distanceAlongBlock);
    TripEntry activeTrip = blockLocation.getActiveTrip();
    String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(activeTrip.getId());
    return new BlockState(blockInstance, blockLocation, dsc);
  }

  private double scoreState(BlockState state, Observation observation) {

    ScheduledBlockLocation blockLocation = state.getBlockLocation();
    CoordinatePoint p1 = blockLocation.getLocation();
    ProjectedPoint p2 = observation.getPoint();

    double d = SphericalGeometryLibrary.distance(p1.getLat(), p1.getLon(),
        p2.getLat(), p2.getLon());
    double prob1 = _nearbyTripSigma.getProbability(d);

    BlockInstance blockInstance = state.getBlockInstance();
    long serviceDate = blockInstance.getServiceDate();
    int scheduledTime = blockLocation.getScheduledTime();

    long time = serviceDate + scheduledTime * 1000;
    long recordTime = observation.getRecord().getTime();

    long timeDelta = Math.abs(time - recordTime) / 1000;
    double prob2 = _scheduleDeviationSigma.getProbability(timeDelta);

    return prob1 * prob2;
  }
}
