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

import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFactory;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import umontreal.iro.lecuyer.rng.MRG32k3a;
import umontreal.iro.lecuyer.rng.RandomStream;

import java.util.List;
import java.util.Set;

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
  private static int _initialNumberOfParticles = 200;

  private final JourneyPhaseSummaryLibrary _journeyStatePhaseLibrary = new JourneyPhaseSummaryLibrary();

  private MotionModelImpl _motionModel;

  private BlocksFromObservationService _blocksFromObservationService;
  private JourneyStateTransitionModel _journeyStateTransitionModel;
  private BlockStateSamplingStrategy _blockStateSamplingStrategy;

  static class LocalRandom extends ThreadLocal<RandomStream> {
    long _seed = 0;

    LocalRandom(long seed) {
      _seed = seed;
    }

    @Override
    protected RandomStream initialValue() {
      if (_seed != 0)
        return new MRG32k3a();
      else
        return new MRG32k3a();
    }
  }

  static class LocalRandomDummy extends ThreadLocal<RandomStream> {
    private static MRG32k3a rng;

    LocalRandomDummy(long seed) {
      rng = new MRG32k3a();
      if (seed != 0)
        rng.setSeed(new long[] {seed, seed, seed, seed, seed, seed});
    }

    @Override
    synchronized public RandomStream get() {
      return rng;
    }
  }

  static ThreadLocal<RandomStream> threadLocalRng;
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
  public void setBlocksFromObservationService(
      BlocksFromObservationService blocksFromObservationService) {
    _blocksFromObservationService = blocksFromObservationService;
  }

  @Autowired
  public void setBlockStateSamplingStrategy(
      BlockStateSamplingStrategy blockStateSamplingStrategy) {
    _blockStateSamplingStrategy = blockStateSamplingStrategy;
  }

  @Autowired
  public void setJourneyStateTransitionModel(
      JourneyStateTransitionModel journeyStateTransitionModel) {
    _journeyStateTransitionModel = journeyStateTransitionModel;
  }

  @Autowired
  public void setMotionModelLibrary(MotionModelImpl motionModel) {
    _motionModel = motionModel;
  }

  public void setInitialNumberOfParticles(int initialNumberOfParticles) {
    _initialNumberOfParticles = initialNumberOfParticles;
  }

  @Override
  public Multiset<Particle> createParticles(double timestamp, Observation obs) {

    final Set<BlockStateObservation> potentialBlocks = _blocksFromObservationService.determinePotentialBlockStatesForObservation(obs);

    final Multiset<Particle> particles = HashMultiset.create();

    for (final BlockStateObservation blockState : potentialBlocks) {
      final MotionState motionState = _motionModel.updateMotionState(obs);

      final double inMotionSample = threadLocalRng.get().nextDouble();
      final boolean vehicleNotMoved = inMotionSample < SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(
          motionState, obs);
      final JourneyState journeyState = _journeyStateTransitionModel.getJourneyState(
          blockState, obs, vehicleNotMoved);

      BlockStateObservation sampledBlockState;
      if (blockState != null) {
        /*
         * Sample a distance along the block using the snapped observation
         * results as priors.
         */
        if (blockState.isSnapped()
            || JourneyStateTransitionModel.isLocationActive(blockState.getBlockState())) {
          sampledBlockState = _blockStateSamplingStrategy.samplePropagatedDistanceState(
              vehicleNotMoved, obs, blockState);
        } else {
          sampledBlockState = blockState;
        }
      } else {
        sampledBlockState = null;
      }
      final VehicleState state = vehicleState(motionState, sampledBlockState,
          journeyState, obs);
      final Particle p = new Particle(timestamp);
      p.setData(state);
      particles.add(p);
    }

    return particles;
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

  public static ThreadLocal<RandomStream> getThreadLocalRng() {
    return threadLocalRng;
  }

  public static int getInitialNumberOfParticles() {
    return _initialNumberOfParticles;
  }

}
