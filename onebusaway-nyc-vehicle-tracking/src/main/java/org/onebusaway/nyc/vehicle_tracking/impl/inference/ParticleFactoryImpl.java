package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFactory;
import org.onebusaway.nyc.vehicle_tracking.services.BaseLocationService;
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

  private int _initialNumberOfParticles = 50;

  public double _distanceSamplingFactor = 1.0;

  private JourneyPhaseSummaryLibrary _journeyStatePhaseLibrary = new JourneyPhaseSummaryLibrary();

  private BlockStateSamplingStrategy _blockStateSamplingStrategy;

  private VehicleStateLibrary _vehicleStateLibrary;

  private BaseLocationService _baseLocationService;

  @Autowired
  public void setBlockStateSamplingStrategy(
      BlockStateSamplingStrategy blockStateSamplingStrategy) {
    _blockStateSamplingStrategy = blockStateSamplingStrategy;
  }

  @Autowired
  public void setVehicleStateLibrary(VehicleStateLibrary vehicleStateLibrary) {
    _vehicleStateLibrary = vehicleStateLibrary;
  }

  @Autowired
  public void setBaseLocationService(BaseLocationService baseLocationService) {
    _baseLocationService = baseLocationService;
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

    // ProjectedPoint point = obs.getPoint();
    // CDFMap<EdgeState> cdf =
    // _edgeStateLibrary.calculatePotentialEdgeStates(point);

    CDFMap<BlockState> atStartCdf = _blockStateSamplingStrategy.cdfForJourneyAtStart(obs);

    CDFMap<BlockState> inProgresCdf = _blockStateSamplingStrategy.cdfForJourneyInProgress(obs);

    List<Particle> particles = new ArrayList<Particle>(
        _initialNumberOfParticles);

    if (atStartCdf.isEmpty() && inProgresCdf.isEmpty())
      _log.warn("no blocks to sample!");

    for (int i = 0; i < _initialNumberOfParticles; i++) {

      CoordinatePoint location = obs.getLocation();
      boolean atBase = _baseLocationService.getBaseNameForLocation(location) != null;
      boolean atTerminal = _baseLocationService.getTerminalNameForLocation(location) != null;

      MotionState motionState = new MotionState(obs.getTime(), location,
          atBase, atTerminal);

      VehicleState state = determineJourneyState(null, motionState, location,
          atStartCdf, inProgresCdf, obs);

      Particle p = new Particle(timestamp);
      p.setData(state);
      particles.add(p);
    }

    return particles;
  }

  public VehicleState determineJourneyState(EdgeState edgeState,
      MotionState motionState, CoordinatePoint locationOnEdge,
      CDFMap<BlockState> atStartCdf, CDFMap<BlockState> inProgressCdf,
      Observation obs) {

    // If we're at a base to start, we favor that over all other possibilities
    if (_vehicleStateLibrary.isAtBase(obs.getLocation())) {

      BlockState blockState = null;

      if (!atStartCdf.isEmpty())
        blockState = atStartCdf.sample();

      return vehicleState(edgeState, motionState, blockState,
          JourneyState.atBase(), obs);
    }

    // At this point, we could be dead heading before a block or actually on a
    // block in progress. We slightly favor blocks already in progress
    if (Math.random() < 0.75) {

      // No blocks? Jump to the deadhead-after state
      if (inProgressCdf.isEmpty())
        return vehicleState(edgeState, motionState, null,
            JourneyState.deadheadAfter(), obs);

      BlockState blockState = inProgressCdf.sample();
      return vehicleState(edgeState, motionState, blockState,
          JourneyState.inProgress(), obs);
    } else {

      // No blocks? Jump to the deadhead-after state
      if (atStartCdf.isEmpty())
        return vehicleState(edgeState, motionState, null,
            JourneyState.deadheadAfter(), obs);

      BlockState blockState = atStartCdf.sample();
      return vehicleState(edgeState, motionState, blockState,
          JourneyState.deadheadBefore(locationOnEdge), obs);
    }
  }

  private VehicleState vehicleState(EdgeState edgeState,
      MotionState motionState, BlockState blockState,
      JourneyState journeyState, Observation obs) {

    List<JourneyPhaseSummary> summaries = _journeyStatePhaseLibrary.extendSummaries(
        null, blockState, journeyState, obs);

    return new VehicleState(edgeState, motionState, blockState, journeyState,
        summaries, obs);
  }
}
