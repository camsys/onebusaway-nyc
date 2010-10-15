package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.onebusaway.collections.MappingLibrary;
import org.onebusaway.collections.Min;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.model.XYPoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.geospatial.services.UTMLibrary;
import org.onebusaway.geospatial.services.UTMProjection;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.transit_data_federation.impl.shapes.PointAndIndex;
import org.onebusaway.transit_data_federation.impl.shapes.ShapePointsLibrary;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.model.ShapePoints;
import org.onebusaway.transit_data_federation.services.ShapePointService;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocationService;
import org.onebusaway.transit_data_federation.services.tripplanner.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.tripplanner.BlockTripEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class BlockStateSamplingStrategyImpl implements BlockStateSamplingStrategy {

  private static Logger _log = LoggerFactory.getLogger(BlockStateSamplingStrategyImpl.class);

  private DestinationSignCodeService _destinationSignCodeService;

  private ScheduledBlockLocationService _scheduledBlockLocationService;

  private ShapePointService _shapePointService;

  private ShapePointsLibrary _shapePointsLibrary = new ShapePointsLibrary();

  /**
   * We need some way of scoring nearby trips
   */
  private DeviationModel _nearbyTripSigma = new DeviationModel(400.0);

  private DeviationModel _scheduleDeviationSigma = new DeviationModel(32 * 60);

  /****
   * Public Methods
   ****/

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

  public void setLocalMinimumThreshold(double localMinimumThreshold) {
    _shapePointsLibrary.setLocalMinimumThreshold(localMinimumThreshold);
  }

  /**
   * 
   * @param scheduleDeviationSigma time, in seconds
   */
  public void setScheduleDeviationSigma(int scheduleDeviationSigma) {
    _scheduleDeviationSigma = new DeviationModel(scheduleDeviationSigma);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateSamplingStrategy
   * #sampleBlockStateAtJourneyStart(java.util.Set,
   * org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation)
   */

  @Override
  public BlockState sampleBlockStateAtJourneyStart(
      Set<BlockInstance> potentialBlocks, Observation observation) {

    CDFMap<BlockState> cdf = cdfForJourneyAtStart(potentialBlocks,
        observation);

    return cdf.sample();
  }

  @Override
  public CDFMap<BlockState> cdfForJourneyAtStart(
      Set<BlockInstance> potentialBlocks, Observation observation) {

    CDFMap<BlockState> cdf = new CDFMap<BlockState>();

    _log.info("potential blocks found: " + potentialBlocks.size());

    for (BlockInstance blockInstance : potentialBlocks) {

      // Start at the beginning of the block
      BlockState state = getAsState(blockInstance, 0.0);

      double p = scoreJourneyStartState(state, observation);

      cdf.put(p, state);

      System.out.println(state.getBlockLocation().getDistanceAlongBlock()
          + "\t" + state.getBlockLocation().getScheduledTime() + "\t" + p
          + "\t" + blockInstance);
    }
    return cdf;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.onebusaway.nyc.vehicle_tracking.impl.inference.BlockStateSamplingStrategy
   * #sampleBlockStateAtJourneyStart(java.util.Set,
   * org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation,
   * org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState)
   */
  @Override
  public BlockState sampleBlockStateAtJourneyStart(
      Set<BlockInstance> potentialBlocks, Observation observation,
      BlockState currentBlockState) {

    CDFMap<BlockState> cdf = new CDFMap<BlockState>();

    _log.info("potential blocks found: " + potentialBlocks.size());

    for (BlockInstance blockInstance : potentialBlocks) {

      // Start at the beginning of the block
      BlockState state = getAsState(blockInstance, 0.0);

      double p = scoreJourneyStartState(state, observation, currentBlockState);

      cdf.put(p, state);

      System.out.println(state.getBlockLocation().getDistanceAlongBlock()
          + "\t" + state.getBlockLocation().getScheduledTime() + "\t" + p
          + "\t" + blockInstance);
    }

    return cdf.sample();
  }

  @Override
  public CDFMap<BlockState> cdfForJourneyInProgress(
      Set<BlockInstance> potentialBlocks, Observation observation) {

    CDFMap<BlockState> cdf = new CDFMap<BlockState>();

    NycVehicleLocationRecord record = observation.getRecord();

    _log.info("potential blocks found: " + potentialBlocks.size());

    for (BlockInstance blockInstance : potentialBlocks) {

      BlockState state = getBestBlockLocation(record.getTime(),
          observation.getPoint(), blockInstance, 0, Double.POSITIVE_INFINITY);

      double p = scoreState(state, observation);

      cdf.put(p, state);

      System.out.println(state.getBlockLocation().getDistanceAlongBlock()
          + "\t" + state.getBlockLocation().getScheduledTime() + "\t" + p
          + "\t" + blockInstance);
    }

    return cdf;
  }

  /****
   * Private Methods
   ****/

  private BlockState getBestBlockLocation(long timestamp,
      ProjectedPoint targetPoint, BlockInstance blockInstance,
      double blockDistanceFrom, double blockDistanceTo) {

    BlockConfigurationEntry block = blockInstance.getBlock();

    ShapePoints shapePoints = getShapePointsForBlock(block);

    UTMProjection projection = UTMLibrary.getProjectionForPoint(
        shapePoints.getLats()[0], shapePoints.getLons()[0]);

    List<XYPoint> projectedShapePoints = _shapePointsLibrary.getProjectedShapePoints(
        shapePoints, projection);

    if (shapePoints == null || shapePoints.getSize() == 0)
      throw new IllegalStateException("block had no shape points: "
          + block.getBlock().getId());

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

      ScheduledBlockLocation location = _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
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

  private BlockState getAsState(BlockInstance blockInstance,
      double distanceAlongBlock) {
    BlockConfigurationEntry block = blockInstance.getBlock();
    ScheduledBlockLocation blockLocation = _scheduledBlockLocationService.getScheduledBlockLocationFromDistanceAlongBlock(
        block.getStopTimes(), distanceAlongBlock);
    if (blockLocation == null)
      throw new IllegalStateException("no blockLocation for " + blockInstance
          + " d=" + distanceAlongBlock);
    BlockTripEntry activeTrip = blockLocation.getActiveTrip();
    String dsc = _destinationSignCodeService.getDestinationSignCodeForTripId(activeTrip.getTrip().getId());
    return new BlockState(blockInstance, blockLocation, dsc);
  }

  /*****
   * State Evaluation Methods
   ****/

  private double scoreJourneyStartState(BlockState state,
      Observation observation) {
    return scoreJourneyStartDestinationSignCode(state, observation);
  }

  private double scoreJourneyStartState(BlockState state,
      Observation observation, BlockState currentBlockState) {

    double dscScore = scoreJourneyStartDestinationSignCode(state, observation);
    double blockChangeScore = scoreBlockChange(state, currentBlockState);
    return dscScore * blockChangeScore;
  }

  private double scoreBlockChange(BlockState newBlockState,
      BlockState oldBlockState) {
    boolean blockChanged = !newBlockState.getBlockInstance().equals(
        oldBlockState.getBlockInstance());
    if (blockChanged)
      return 0.5;
    else
      return 0.95;
  }

  private double scoreState(BlockState state, Observation observation) {

    ScheduledBlockLocation blockLocation = state.getBlockLocation();
    CoordinatePoint p1 = blockLocation.getLocation();
    ProjectedPoint p2 = observation.getPoint();

    double d = SphericalGeometryLibrary.distance(p1.getLat(), p1.getLon(),
        p2.getLat(), p2.getLon());
    double prob1 = _nearbyTripSigma.probability(d);

    BlockInstance blockInstance = state.getBlockInstance();
    long serviceDate = blockInstance.getServiceDate();
    int scheduledTime = blockLocation.getScheduledTime();

    long time = serviceDate + scheduledTime * 1000;
    long recordTime = observation.getRecord().getTime();

    long timeDelta = Math.abs(time - recordTime) / 1000;
    double prob2 = _scheduleDeviationSigma.probability(timeDelta);

    return prob1 * prob2;
  }

  private double scoreJourneyStartDestinationSignCode(BlockState state,
      Observation observation) {

    NycVehicleLocationRecord record = observation.getRecord();
    String observedDsc = record.getDestinationSignCode();

    boolean observedOutOfService = _destinationSignCodeService.isOutOfServiceDestinationSignCode(observedDsc);

    // If we have an out-of-service DSC, then we favor it equally
    if (observedOutOfService) {
      return 0.5;
    } else {
      // Favor blocks that match the correct DSC
      String dsc = state.getDestinationSignCode();
      if (observedDsc.equals(dsc))
        return 0.95;
      else
        return 0.25;
    }
  }

  private ShapePoints getShapePointsForBlock(BlockConfigurationEntry block) {
    // TODO : BLOCK : Ok, since we have the BlockInstance in the parent
    List<AgencyAndId> shapePointIds = MappingLibrary.map(block.getTrips(),
        "trip.shapeId");
    return _shapePointService.getShapePointsForShapeIds(shapePointIds);
  }

}
