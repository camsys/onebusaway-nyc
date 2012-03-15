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

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions.CategoricalDist;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.EdgeLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.GpsLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.PriorRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.ScheduleLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.RunTransitionLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;

import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Sets;

import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Motion model implementation for vehicle location inference. Determine if the
 * vehicle is in motion, and propagate the particles' positions or states.
 * 
 * @author bdferris, bwillard
 */
public class MotionModelImpl implements MotionModel<Observation> {

  private JourneyStateTransitionModel _journeyMotionModel;

  private BlocksFromObservationService _blocksFromObservationService;

  private BlockStateTransitionModel _blockStateTransitionModel;

  /**
   * Distance, in meters, that a bus has to travel to be considered "in motion"
   */
  static private double _motionThreshold = 20;

  @Autowired
  public void setJourneyMotionModel(
      JourneyStateTransitionModel journeyMotionModel) {
    _journeyMotionModel = journeyMotionModel;
  }

  @Autowired
  public void setBlocksFromObservationService(
      BlocksFromObservationService blocksFromObservationService) {
    _blocksFromObservationService = blocksFromObservationService;
  }
  
  @Autowired
  public void setBlocksStateTransitionModel(
      BlockStateTransitionModel blockStateTransitionModel) {
    _blockStateTransitionModel = blockStateTransitionModel;
  }

  static public void setMotionThreshold(double motionThreshold) {
    _motionThreshold = motionThreshold;
  }

  static public double getMotionThreshold() {
    return _motionThreshold;
  }

  static public EdgeLikelihood edgeLikelihood = new EdgeLikelihood();
  static public ScheduleLikelihood schedLikelihood = new ScheduleLikelihood();
  static public RunTransitionLikelihood transitionLikelihood = new RunTransitionLikelihood();
  static public PriorRule priorLikelihood = new PriorRule();

  private JourneyStateTransitionModel _journeyStateTransitionModel;

  private BlockStateSamplingStrategy _blockStateSamplingStrategy;

  @Autowired
  public void setJourneyStateTransitionModel(
      JourneyStateTransitionModel journeyStateTransitionModel) {
    _journeyStateTransitionModel = journeyStateTransitionModel;
  }

  @Autowired
  public void setBlockStateSamplingStrategy(
      BlockStateSamplingStrategy blockStateSamplingStrategy) {
    _blockStateSamplingStrategy = blockStateSamplingStrategy;
  }

  @Override
  public void move(Entry<Particle> parent, double timestamp,
      double timeElapsed, Observation obs, Multiset<Particle> results,
      Multimap<VehicleState, VehicleState> cache) {

    final VehicleState parentState = parent.getElement().getData();
    final MotionState motionState = updateMotionState(parentState, obs);

    final BlockState parentBlockState = parentState.getBlockState();
    final Set<BlockStateObservation> transitions = Sets.newHashSet();
    boolean allowBlockTransition = _blockStateTransitionModel.allowBlockTransition(parentState, obs);
    if (parentBlockState == null || allowBlockTransition) {
      /*
       * These are all the snapped and DSC/run blocks
       */
      transitions.addAll(_blocksFromObservationService.determinePotentialBlockStatesForObservation(obs));
    } else {
      transitions.addAll(_blocksFromObservationService.advanceState(obs, 
          parentState.getBlockState(), -1.0 * GpsLikelihood.gpsStdDev/2.0, 
          GpsLikelihood.gpsStdDev/2.0));
    }
    
    transitions.add(parentState.getBlockStateObservation());
      
    final double vehicleHasNotMovedProb = SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(
        motionState, obs);

    if (transitions.isEmpty()
        || (transitions.size() == 1 && transitions.contains(null))) {
      final double inMotionSample = ParticleFactoryImpl.getThreadLocalRng().get().nextDouble();
      final boolean vehicleNotMoved = inMotionSample < SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(
          motionState, obs);
      final JourneyState journeyState = _journeyStateTransitionModel.getJourneyState(
          null, obs, vehicleNotMoved);
      final VehicleState nullState = new VehicleState(motionState, null,
          journeyState, null, obs);
      results.add(new Particle(timestamp, parent.getElement(), 1.0, nullState));
      return;
    }

    for (int i = 0; i < parent.getCount(); ++i) {
      final CategoricalDist<Particle> transitionDist = new CategoricalDist<Particle>();
      for (final BlockStateObservation proposalEdge : transitions) {

        final SensorModelResult transProb = new SensorModelResult("transition");
        final double inMotionSample = ParticleFactoryImpl.getThreadLocalRng().get().nextDouble();
        final boolean vehicleNotMoved = inMotionSample < vehicleHasNotMovedProb;

        final boolean runChanged = hasRunChanged(parentState.getBlockStateObservation(), proposalEdge);
        BlockStateObservation newEdge;
        if (proposalEdge != null) {
          if (parentBlockState != null) {
            /*
             * We sample the a schedule propagation when there
             * are no snapped states, otherwise, we sample "true' 
             * locations from the GPS's error range. 
             */
            if (proposalEdge.isSnapped()) {
              newEdge = _blockStateSamplingStrategy.sampleGpsObservationState(proposalEdge, obs);
//              newEdge = proposalEdge;
            } else if (runChanged) {
              newEdge = _blockStateSamplingStrategy.samplePriorScheduleState(
                  proposalEdge.getBlockState().getBlockInstance(), obs);
            } else {
//              newEdge = _blockStateSamplingStrategy.sampleTransitionScheduleDev(
//                  parentState.getBlockStateObservation(), obs);
              newEdge = _blockStateSamplingStrategy.sampleTransitionDistanceState(
                  parentState.getBlockStateObservation(), obs, vehicleNotMoved, 
                  parentState.getJourneyState().getPhase());
            }
          } else {
            if (proposalEdge.isSnapped()) {
              newEdge = _blockStateSamplingStrategy.sampleGpsObservationState(proposalEdge, obs);
//              newEdge = proposalEdge;
            } else {
              newEdge = _blockStateSamplingStrategy.samplePriorScheduleState(
                  proposalEdge.getBlockState().getBlockInstance(), obs);
            }
          }
        } else {
          newEdge = null;
        }
        final JourneyState journeyState = _journeyStateTransitionModel.getJourneyState(
            newEdge, obs, vehicleNotMoved);
        final VehicleState newState = new VehicleState(motionState, newEdge,
            journeyState, null, obs);
        final Context context = new Context(parentState, newState, obs);

        /*
         * Edge movement
         */
        transProb.addResultAsAnd(edgeLikelihood.likelihood(null, context));

        /*
         * Schedule Dev
         */
        transProb.addResultAsAnd(schedLikelihood.likelihood(null, context));
        
        /*
         * Transition
         */
        transProb.addResultAsAnd(transitionLikelihood.likelihood(null, context));
        
        /*
         * State priors
         */
//        transProb.addResultAsAnd(priorLikelihood.likelihood(null, context));
        Particle newParticle = new Particle(timestamp, parent.getElement(), 1.0, newState);
        newParticle.setTransResult(transProb);

        transitionDist.put(transProb.getProbability(), newParticle);
      }

      if (transitionDist.canSample()) {
        results.add(transitionDist.sample());
      } else {
        final double inMotionSample = ParticleFactoryImpl.getThreadLocalRng().get().nextDouble();
        final boolean vehicleNotMoved = inMotionSample < SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(
            motionState, obs);
        final JourneyState journeyState = _journeyStateTransitionModel.getJourneyState(
            null, obs, vehicleNotMoved);
        final VehicleState nullState = new VehicleState(motionState, null,
            journeyState, null, obs);
        results.add(new Particle(timestamp, parent.getElement(), 1.0, nullState));
      }
    }
  }

  // @Override
  // public void move(Entry<Particle> parent, double timestamp, double
  // timeElapsed,
  // Observation obs, Multiset<Particle> results,
  // Multimap<VehicleState, VehicleState> cache) {
  //
  // final VehicleState parentState = parent.getElement().getData();
  // final MotionState motionState = updateMotionState(parentState, obs);
  //
  // final Collection<VehicleState> vehicleStates = cache.get(parentState);
  //
  // if (vehicleStates.isEmpty()) {
  // _journeyMotionModel.move(parentState, motionState, obs, vehicleStates);
  // for (final VehicleState vs : vehicleStates)
  // results.add(new Particle(timestamp, parent.getElement(), 1.0, vs),
  // parent.getCount());
  // }
  //
  // }

  public static boolean hasRunChanged(BlockStateObservation parentEdge,
      BlockStateObservation proposalEdge) {
    if (proposalEdge != null) {
      if (parentEdge != null) {
        if (!proposalEdge.getBlockState().getRunId()
            .equals(parentEdge.getBlockState().getRunId())) {
          return true;
        } else {
          return false;
        }
      } else {
        return true;
      }
    } else if (parentEdge != null){
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void move(Particle parent, double timestamp, double timeElapsed,
      Observation obs, Collection<Particle> results) {

    final VehicleState parentState = parent.getData();

    final MotionState motionState = updateMotionState(parentState, obs);

    final List<VehicleState> vehicleStates = new ArrayList<VehicleState>();
    _journeyMotionModel.move(parentState, motionState, obs, vehicleStates);

    for (final VehicleState vs : vehicleStates)
      results.add(new Particle(timestamp, parent, 1.0, vs));
  }

  public MotionState updateMotionState(Observation obs) {
    return updateMotionState(null, obs);
  }

  public static MotionState updateMotionState(VehicleState parentState, Observation obs) {

    long lastInMotionTime = obs.getTime();
    CoordinatePoint lastInMotionLocation = obs.getLocation();

    if (parentState != null) {

      final MotionState motionState = parentState.getMotionState();

      final double d = SphericalGeometryLibrary.distance(
          motionState.getLastInMotionLocation(), obs.getLocation());

      if (d <= _motionThreshold) {
        lastInMotionTime = motionState.getLastInMotionTime();
        lastInMotionLocation = motionState.getLastInMotionLocation();
      }
    }

    return new MotionState(lastInMotionTime, lastInMotionLocation);
  }

  public EdgeLikelihood getEdgeLikelihood() {
    return edgeLikelihood;
  }

  public ScheduleLikelihood getSchedLikelihood() {
    return schedLikelihood;
  }
}
