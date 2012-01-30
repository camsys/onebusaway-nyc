/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions.CategoricalDist;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFactory;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Create particles from an observation.
 * 
 * The general idea here is that we:
 * 
 * <ul>
 * <li>check the vehicle reported operator+UTS/runId for assigned runs, which
 * inform us of potential block locations</li>
 * <li>look for nearby stops/blocks</li>
 * <li>snap to the edges connected to those nodes</li>
 * <li>sample particles from those edges, as weighted by their reported status,
 * and distance from our start point</li>
 * </ul>
 * 
 * @author bwillard, bdferris
 */
public class ParticleFactoryImpl implements ParticleFactory<Observation> {

  private static Logger _log = LoggerFactory.getLogger(ParticleFactoryImpl.class);

  private int _initialNumberOfParticles = 200;

  private JourneyPhaseSummaryLibrary _journeyStatePhaseLibrary = new JourneyPhaseSummaryLibrary();

  private BlockStateSamplingStrategy _blockStateSamplingStrategy;

  private VehicleStateLibrary _vehicleStateLibrary;

  private MotionModelImpl _motionModel;

  static class LocalRandom extends ThreadLocal<Random> {
    long _seed = 0;

    LocalRandom(long seed) {
      _seed = seed;
    }

    @Override
    protected Random initialValue() {
      if (_seed != 0)
        return new Random(_seed);
      else
        return new Random();
    }
  }

  static class LocalRandomDummy extends ThreadLocal<Random> {
    private static Random rng;

    LocalRandomDummy(long seed) {
      if (seed != 0)
        rng = new Random(seed);
      else
        rng = new Random();
    }

    @Override
    synchronized public Random get() {
      return rng;
    }
  }

  static ThreadLocal<Random> threadLocalRng;
  static {
    if (!ParticleFilter.getTestingEnabled()) {
      threadLocalRng = new LocalRandom(0);
    } else {
      threadLocalRng = new LocalRandomDummy(0);

    }
  }

  synchronized public static void setSeed(long seed) {
    if (!ParticleFilter.getTestingEnabled()) {
      threadLocalRng = new LocalRandom(seed);
    } else {
      threadLocalRng = new LocalRandomDummy(seed);

    }
  }

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
  public void setMotionModelLibrary(MotionModelImpl motionModel) {
    _motionModel = motionModel;
  }

  public void setInitialNumberOfParticles(int initialNumberOfParticles) {
    _initialNumberOfParticles = initialNumberOfParticles;
  }

  @Override
  public List<Particle> createParticles(double timestamp, Observation obs) {

    // ProjectedPoint point = obs.getPoint();
    // CDFMap<EdgeState> cdf =
    // _edgeStateLibrary.calculatePotentialEdgeStates(point);

    CategoricalDist<BlockStateObservation> atStartCdf = _blockStateSamplingStrategy.cdfForJourneyAtStart(obs);

    CategoricalDist<BlockStateObservation> inProgresCdf = _blockStateSamplingStrategy.cdfForJourneyInProgress(obs);

    List<Particle> particles = new ArrayList<Particle>(
        _initialNumberOfParticles);

    if (atStartCdf.isEmpty() && inProgresCdf.isEmpty())
      _log.warn("no blocks to sample for obs=" + obs);

    for (int i = 0; i < _initialNumberOfParticles; i++) {

      MotionState motionState = _motionModel.updateMotionState(obs);

      VehicleState state = determineJourneyState(motionState, atStartCdf,
          inProgresCdf, obs);

      Particle p = new Particle(timestamp);
      p.setData(state);
      particles.add(p);
    }

    return particles;
  }

  public VehicleState determineJourneyState(MotionState motionState,
      CategoricalDist<BlockStateObservation> atStartCdf,
      CategoricalDist<BlockStateObservation> inProgressCdf, Observation obs) {

    BlockStateObservation blockState = null;

    // If we're at a base to start, we favor that over all other possibilities
    if (_vehicleStateLibrary.isAtBase(obs.getLocation())) {

      if (blockState == null && atStartCdf.canSample())
        blockState = atStartCdf.sample();

      return vehicleState(motionState, blockState, JourneyState.atBase(), obs);
    }

    // At this point, we could be dead heading before a block or actually on a
    // block in progress. We slightly favor blocks already in progress
    double progressSwitch = threadLocalRng.get().nextDouble();
    if (progressSwitch < 0.75) {

      // No blocks? Jump to the deadhead-before state
      if (!inProgressCdf.canSample())
        return vehicleState(motionState, null,
            JourneyState.deadheadBefore(obs.getLocation()), obs);

      if (blockState == null)
        blockState = inProgressCdf.sample();

      return vehicleState(motionState, blockState, JourneyState.inProgress(),
          obs);
    } else {

      // No blocks? Jump to the deadhead-before state
      if (!atStartCdf.canSample())
        return vehicleState(motionState, null,
            JourneyState.deadheadBefore(obs.getLocation()), obs);

      if (blockState == null)
        blockState = atStartCdf.sample();

      return vehicleState(motionState, blockState,
          JourneyState.deadheadBefore(obs.getLocation()), obs);
    }
  }

  private VehicleState vehicleState(MotionState motionState,
      BlockStateObservation blockState, JourneyState journeyState,
      Observation obs) {

    List<JourneyPhaseSummary> summaries = null;
    if (ParticleFilter.getDebugEnabled()) {
      summaries = _journeyStatePhaseLibrary.extendSummaries(null, blockState,
          journeyState, obs);
    }

    return new VehicleState(motionState, blockState, journeyState, summaries,
        obs);
  }

  public static ThreadLocal<Random> getThreadLocalRng() {
    return threadLocalRng;
  }

  // public static double getNextDouble() {
  // return threadLocalRng.get().nextDouble();
  // }

}
