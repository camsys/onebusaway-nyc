package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Set;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.services.DestinationSignCodeService;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
class BlockStateSamplingStrategyImpl implements BlockStateSamplingStrategy {

  private static Logger _log = LoggerFactory.getLogger(BlockStateSamplingStrategyImpl.class);

  private DestinationSignCodeService _destinationSignCodeService;

  /**
   * We need some way of scoring nearby trips
   */
  private DeviationModel _nearbyTripSigma = new DeviationModel(400.0);

  private DeviationModel _scheduleDeviationSigma = new DeviationModel(32 * 60);

  private BlockStateService _blockStateService;

  /****
   * Public Methods
   ****/

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
  }

  @Autowired
  public void setBlockStateService(BlockStateService blockStateService) {
    _blockStateService = blockStateService;
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

    CDFMap<BlockState> cdf = cdfForJourneyAtStart(potentialBlocks, observation);

    return cdf.sample();
  }

  @Override
  public CDFMap<BlockState> cdfForJourneyAtStart(
      Set<BlockInstance> potentialBlocks, Observation observation) {

    CDFMap<BlockState> cdf = new CDFMap<BlockState>();

    _log.info("potential blocks found: " + potentialBlocks.size());

    for (BlockInstance blockInstance : potentialBlocks) {

      // Start at the beginning of the block
      BlockState state = _blockStateService.getAsState(blockInstance, 0.0);

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
      BlockState currentBlockState,
      boolean phaseTransitionSuggestsBlockTransition) {

    CDFMap<BlockState> cdf = new CDFMap<BlockState>();

    _log.info("potential blocks found: " + potentialBlocks.size());

    for (BlockInstance blockInstance : potentialBlocks) {

      // Start at the beginning of the block
      BlockState state = _blockStateService.getAsState(blockInstance, 0.0);

      double p = scoreJourneyStartState(state, observation, currentBlockState,
          phaseTransitionSuggestsBlockTransition);

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

      BlockState state = _blockStateService.getBestBlockLocation(
          record.getTime(), observation.getPoint(), blockInstance, 0,
          Double.POSITIVE_INFINITY);

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

  private double scoreJourneyStartState(BlockState state,
      Observation observation) {
    return scoreJourneyStartDestinationSignCode(state, observation);
  }

  private double scoreJourneyStartState(BlockState state,
      Observation observation, BlockState currentBlockState,
      boolean phaseTransitionSuggestsBlockTransition) {

    if (phaseTransitionSuggestsBlockTransition)
      return 0.95;

    double dscScore = scoreJourneyStartDestinationSignCode(state, observation);
    double blockChangeScore = scoreBlockChange(state, currentBlockState);
    return dscScore * blockChangeScore;
  }

  private double scoreBlockChange(BlockState newBlockState,
      BlockState oldBlockState) {

    boolean blockChanged = blockChanged(newBlockState, oldBlockState);
    if (blockChanged)
      return 0.5;
    else
      return 0.95;
  }

  private boolean blockChanged(BlockState newBlockState,
      BlockState oldBlockState) {
    if (newBlockState == null && oldBlockState == null)
      return false;
    if (newBlockState == null && oldBlockState != null)
      return true;
    if (newBlockState != null && oldBlockState == null)
      return true;
    return !newBlockState.getBlockInstance().equals(
        oldBlockState.getBlockInstance());
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
}
