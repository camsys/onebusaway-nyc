package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.motion_model.StateTransitionService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFactory;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Create initial particles from an initial observation.
 * 
 * The general idea here is that we:
 * 
 * <ul>
 * <li>look for nearby street nodes</li>
 * <li>snap to the edges connected to those nodes</li>
 * <li>sample particles from those edges, as weighted by their distance from our
 * start point</li>
 * </ul>
 * 
 * @author bdferris
 */
public class ParticleFactoryImpl implements ParticleFactory<Observation> {

  private static Logger _log = LoggerFactory.getLogger(ParticleFactoryImpl.class);

  private BlocksFromObservationService _blocksFromObservationService;

  private int _initialNumberOfParticles = 50;

  public double _distanceSamplingFactor = 1.0;

  private EdgeStateLibrary _edgeStateLibrary;

  private BlockStateSamplingStrategyImpl _blockStateSamplingStrategy;

  private StateTransitionService _stateTransitionService;

  @Autowired
  public void setEdgeStateLibrary(EdgeStateLibrary edgeStateLibrary) {
    _edgeStateLibrary = edgeStateLibrary;
  }

  @Autowired
  public void setBlocksFromObservationService(
      BlocksFromObservationService blocksFromObservationService) {
    _blocksFromObservationService = blocksFromObservationService;
  }

  @Autowired
  public void setBlockStateSamplingStrategy(
      BlockStateSamplingStrategyImpl blockStateSamplingStrategy) {
    _blockStateSamplingStrategy = blockStateSamplingStrategy;
  }

  @Autowired
  public void setStateTransitionService(
      StateTransitionService stateTransitionService) {
    _stateTransitionService = stateTransitionService;
  }

  public void setInitialNumberOfParticles(int initialNumberOfParticles) {
    _initialNumberOfParticles = initialNumberOfParticles;
  }

  /**
   * When we sample from potential nearby locations, this controls how likely we
   * are to pick a location nearby versus one far-away. Make this number smaller
   * to keep the samples closer to our start locations. A value of 1.0 is a
   * reasonable start.
   * 
   * @param distanceSamplingFactor
   */
  public void setDistanceSamplingFactor(double distanceSamplingFactor) {
    _distanceSamplingFactor = distanceSamplingFactor;
  }

  @Override
  public List<Particle> createParticles(double timestamp, Observation obs) {

    ProjectedPoint point = obs.getPoint();
    CDFMap<EdgeState> cdf = _edgeStateLibrary.calculatePotentialEdgeStates(point);

    Set<BlockInstance> blocks = _blocksFromObservationService.determinePotentialBlocksForObservation(obs);

    CDFMap<BlockState> atStartCdf = _blockStateSamplingStrategy.cdfForJourneyAtStart(
        blocks, obs);

    CDFMap<BlockState> inProgresCdf = _blockStateSamplingStrategy.cdfForJourneyInProgress(
        blocks, obs);

    List<Particle> particles = new ArrayList<Particle>(
        _initialNumberOfParticles);

    if (blocks.isEmpty())
      _log.warn("no blocks to sample!");

    for (int i = 0; i < _initialNumberOfParticles; i++) {

      EdgeState edgeState = cdf.sample();

      CoordinatePoint locationOnEdge = edgeState.getLocationOnEdge();
      MotionState motionState = new MotionState(obs.getTime(), locationOnEdge);

      JourneyState js = determineJourneyState(edgeState, locationOnEdge,
          blocks, atStartCdf, inProgresCdf, obs);

      VehicleState state = new VehicleState(edgeState, motionState, js);

      Particle p = new Particle(timestamp);
      p.setData(state);
      particles.add(p);
    }

    return particles;
  }

  public JourneyState determineJourneyState(EdgeState edgeState,
      CoordinatePoint locationOnEdge, Set<BlockInstance> blocks,
      CDFMap<BlockState> atStartCdf, CDFMap<BlockState> inProgressCdf,
      Observation obs) {

    // If we're at a base to start, we favor that over all other possibilities
    if (_stateTransitionService.isAtBase(edgeState)) {

      BlockState blockState = null;

      if (!atStartCdf.isEmpty())
        blockState = atStartCdf.sample();

      return _stateTransitionService.transitionToBase(blockState, obs);
    }

    // No blocks? Jump to the unknown state
    if (blocks.isEmpty())
      return JourneyState.unknown();

    // At this point, we could be dead heading before a block or actually on a
    // block in progress. We slightly favor blocks already in progress
    if (Math.random() < 0.75)
      return JourneyState.inProgress(inProgressCdf.sample());
    else
      return JourneyState.deadheadBefore(atStartCdf.sample(), locationOnEdge);
  }
}
