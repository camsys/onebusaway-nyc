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

import org.onebusaway.container.refresh.Refreshable;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions.CategoricalDist;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.DestinationSignCodeRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.EdgeLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.GpsLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.NullLocationLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.NullStateLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.PriorRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.RunRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.RunTransitionLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.ScheduleLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data.model.blocks.BlockStatusBean;
import org.onebusaway.transit_data_federation.impl.RefreshableResources;

import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Sets;

import org.apache.commons.math.util.FastMath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

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

  private DestinationSignCodeService _destinationSignCodeService;

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
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
    _destinationSignCodeService = destinationSignCodeService;
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

  public EdgeLikelihood edgeLikelihood = new EdgeLikelihood();
  public ScheduleLikelihood schedLikelihood = new ScheduleLikelihood();
  public RunTransitionLikelihood runTransitionLikelihood = new RunTransitionLikelihood();
  public GpsLikelihood gpsLikelihood = new GpsLikelihood();
  public PriorRule priorLikelihood = new PriorRule();
  public RunRule runLikelihood = new RunRule();
  public DestinationSignCodeRule dscLikelihood;
  public NullStateLikelihood nullStateLikelihood;
  public NullLocationLikelihood nullLocationLikelihood;

  private JourneyStateTransitionModel _journeyStateTransitionModel;

  private BlockStateSamplingStrategy _blockStateSamplingStrategy;

  @Autowired
  public void setNullStateLikelihood(
      NullStateLikelihood nullStateLikelihood) {
    this.nullStateLikelihood = nullStateLikelihood;
  }
  
  @Autowired
  public void setNullLocationLikelihood(
      NullLocationLikelihood nullLocationLikelihood) {
    this.nullLocationLikelihood = nullLocationLikelihood;
  }
  
  @Autowired
  public void setDestinationSignCodeRule(
      DestinationSignCodeRule dscRule) {
    this.dscLikelihood = dscRule;
  }
  
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

  private boolean allowParentBlockTransition(VehicleState parentState, Observation obs) {
    
    final BlockStateObservation parentBlockState = parentState.getBlockStateObservation();
    
    if (parentBlockState == null && !obs.isOutOfService())
      return true;

    if (parentBlockState != null) {
      /*
       * Check if we don't have useable run-info
       */
      if (Strings.isNullOrEmpty(obs.getOpAssignedRunId())
          && obs.getFuzzyMatchDistance() == null) {
        
        /*
         * We have no run information, so we will allow a run transition
         * when we hit deadhead-after.
         */
        if (parentState.getJourneyState().getPhase() == EVehiclePhase.DEADHEAD_AFTER) {
          return true;
        }
        
//        final Observation prevObs = obs.getPreviousObservation();
//        /**
//         * Have we just transitioned out of a terminal?
//         */
//        if (prevObs != null) {
//          /**
//           * If we were assigned a block, then use the block's terminals, otherwise,
//           * all terminals.
//           */
//          if (parentBlockState != null) {
//            final boolean wasAtBlockTerminal = VehicleStateLibrary.isAtPotentialTerminal(
//                prevObs.getRecord(),
//                parentBlockState.getBlockState().getBlockInstance());
//            final boolean isAtBlockTerminal = VehicleStateLibrary.isAtPotentialTerminal(
//                obs.getRecord(),
//                parentBlockState.getBlockState().getBlockInstance());
//    
//            if (wasAtBlockTerminal && !isAtBlockTerminal) {
//              return true;
//            }
//          }
//        }
      } 
    }
    
    return false;
  }
  
  private boolean allowGeneralBlockTransition(Observation obs, double ess) {
    
    if (BlockStateTransitionModel.hasDestinationSignCodeChangedBetweenObservations(obs))
      return true;
    
    final double essResampleThreshold;
    if (Strings.isNullOrEmpty(obs.getOpAssignedRunId())
        && obs.getFuzzyMatchDistance() == null) {
      essResampleThreshold =  0.5;
    } else {
      essResampleThreshold = 0.25;
    }
        
    if (ess / ParticleFactoryImpl.getInitialNumberOfParticles() <= essResampleThreshold) 
      return true;
    
    return false;
  }
  
  @Override
  public Multiset<Particle> move(Multiset<Particle> particles,
      double timestamp, double timeElapsed, Observation obs) {

    final Multiset<Particle> results = HashMultiset.create();

    final double ess = ParticleFilter.getEffectiveSampleSize(particles);
    boolean generalBlockTransition = allowGeneralBlockTransition(obs, ess);
    
    /*
     * Since the particle weights will drop to zero very quickly
     * we need to offset by the highest weight.
     */
    double offset = 0d;
    
    for (final Multiset.Entry<Particle> parent : particles.entrySet()) {
      final VehicleState parentState = parent.getElement().getData();
      final MotionState motionState = updateMotionState(parentState, obs);
      final BlockStateObservation parentBlockStateObs = parentState.getBlockStateObservation();

      final Set<BlockStateObservation> transitions = Sets.newHashSet();
      
      if (generalBlockTransition 
          || allowParentBlockTransition(parentState, obs)) {
        /*
         * These are all the snapped and DSC/run blocks
         */
        transitions.addAll(_blocksFromObservationService.determinePotentialBlockStatesForObservation(obs));
      } else if (parentBlockStateObs != null){
        /*
         * Only the snapped blocks
         */
        transitions.addAll(_blocksFromObservationService.advanceState(obs,
            parentState.getBlockState(), -1.0 * GpsLikelihood.gpsStdDev / 2.0,
            Double.POSITIVE_INFINITY));
        // GpsLikelihood.gpsStdDev/2.0));
      }

      BlockStateObservation newParentBlockStateObs;
      if (parentBlockStateObs != null) {
        newParentBlockStateObs = _blocksFromObservationService.getBlockStateObservationFromDist(
            obs,
            parentBlockStateObs.getBlockState().getBlockInstance(),
            parentBlockStateObs.getBlockState().getBlockLocation().getDistanceAlongBlock());
      } else {
        newParentBlockStateObs = null;
      }
      transitions.add(newParentBlockStateObs);

      final double vehicleHasNotMovedProb = SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(
          motionState, obs);

      for (int i = 0; i < parent.getCount(); ++i) {
        Particle sampledParticle = sampleTransitionParticle(parent, newParentBlockStateObs,
            obs, vehicleHasNotMovedProb, transitions);
//        if (sampledParticle.getLogWeight() > offset)
//          offset = sampledParticle.getLogWeight();
        results.add(sampledParticle);
      }
    }
    
//    for (Particle p : results) {
//      p.setLogWeight(p.getLogWeight() - offset);
//    }

    return results;
  }

  private Particle sampleTransitionParticle(Entry<Particle> parent,
      BlockStateObservation newParentBlockStateObs, Observation obs,
      final double vehicleHasNotMovedProb,
      Set<BlockStateObservation> transitions) {

    final long timestamp = obs.getTime();
    final VehicleState parentState = parent.getElement().getData();
    final MotionState motionState = updateMotionState(parentState, obs);

    final CategoricalDist<Particle> transitionDist = new CategoricalDist<Particle>();

    for (final BlockStateObservation proposalEdge : transitions) {

      final SensorModelResult transProb = new SensorModelResult("Transition");
      final double inMotionSample = ParticleFactoryImpl.getThreadLocalRng().get().nextDouble();
      final boolean vehicleNotMoved = inMotionSample < vehicleHasNotMovedProb;

      final Map.Entry<BlockSampleType, BlockStateObservation> newEdge = sampleEdgeFromProposal(
          newParentBlockStateObs, proposalEdge, obs,
          parentState.getJourneyState().getPhase(), vehicleNotMoved);
      final JourneyState journeyState = _journeyStateTransitionModel.getJourneyState(
          newEdge.getValue(), obs, vehicleNotMoved);
      final VehicleState newState = new VehicleState(motionState, newEdge.getValue(),
          journeyState, null, obs);
      final Context context = new Context(parentState, newState, obs);

      /*
       * Edge movement
       */
//      if (newEdge.getKey() != BlockSampleType.EDGE_MOVEMENT_SAMPLE) {
        transProb.addResultAsAnd(edgeLikelihood.likelihood(null, context));
//      }
      
      /*
       * Gps
       */
      transProb.addResultAsAnd(gpsLikelihood.likelihood(null, context));

      /*
       * Dsc 
       */
      transProb.addResultAsAnd(dscLikelihood.likelihood(null, context));
      
      /*
       * Run 
       */
      transProb.addResultAsAnd(runLikelihood.likelihood(null, context));
      
      /*
       * Schedule Dev
       */
//      if (newEdge.getKey() != BlockSampleType.SCHEDULE_STATE_SAMPLE) {
        transProb.addResultAsAnd(schedLikelihood.likelihood(null, context));
//      }

      /*
       * Run Transition
       */
      transProb.addResultAsAnd(runTransitionLikelihood.likelihood(null, context));
      
      /*
       * Null-state's Prior
       */
      transProb.addResultAsAnd(nullStateLikelihood.likelihood(null, context));
      
      /*
       * Null-location's Prior
       */
      transProb.addResultAsAnd(nullLocationLikelihood.likelihood(null, context));
      
      final Particle newParticle = new Particle(timestamp, parent.getElement(),
          0.0, newState);
      newParticle.setTransResult(transProb);

      transitionDist.put(transProb.getProbability(), newParticle);
    }

    if (transitionDist.canSample()) {
      final Particle sampledParticle = transitionDist.sample();
      sampledParticle.setLogWeight(parent.getElement().getLogWeight()
          + sampledParticle.getTransResult().getLogProbability());
//      sampledParticle.setLogWeight(sampledParticle.getTransResult().getLogProbability());
      return sampledParticle;
    } else {
      final SensorModelResult transProb = new SensorModelResult("Transition (null)");
      final double inMotionSample = ParticleFactoryImpl.getThreadLocalRng().get().nextDouble();
      final boolean vehicleNotMoved = inMotionSample < SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(
          motionState, obs);
      final JourneyState journeyState = _journeyStateTransitionModel.getJourneyState(
          null, obs, vehicleNotMoved);
      final VehicleState nullState = new VehicleState(motionState, null,
          journeyState, null, obs);
      final Context context = new Context(parentState, nullState, obs);
      transProb.addResultAsAnd(edgeLikelihood.likelihood(null, context));
      transProb.addResultAsAnd(gpsLikelihood.likelihood(null, context));
      transProb.addResultAsAnd(dscLikelihood.likelihood(null, context));
      transProb.addResultAsAnd(runLikelihood.likelihood(null, context));
      transProb.addResultAsAnd(schedLikelihood.likelihood(null, context));
      transProb.addResultAsAnd(runTransitionLikelihood.likelihood(null, context));
      transProb.addResultAsAnd(nullStateLikelihood.likelihood(null, context));
      transProb.addResultAsAnd(nullLocationLikelihood.likelihood(null, context));
      final Particle newParticle = new Particle(timestamp, parent.getElement(),
          0.0, nullState);
      newParticle.setTransResult(transProb);
      newParticle.setLogWeight(parent.getElement().getLogWeight()
          + newParticle.getTransResult().getLogProbability());
//      newParticle.setLogWeight(newParticle.getTransResult().getLogProbability());
      
      return newParticle; 
    }
  }

  public static enum BlockSampleType {
    NOT_SAMPLED, SCHEDULE_STATE_SAMPLE, EDGE_MOVEMENT_SAMPLE
  };
  
  private Map.Entry<BlockSampleType, BlockStateObservation> sampleEdgeFromProposal(
      BlockStateObservation parentBlockStateObs,
      BlockStateObservation proposalEdge, Observation obs,
      EVehiclePhase parentPhase, boolean vehicleNotMoved) {
    Map.Entry<BlockSampleType, BlockStateObservation> newEdge;
    if (proposalEdge != null) {
      if (parentBlockStateObs != null) {
        /*
         * When a state is transitioning we need to consider whether the
         * proposal edge is snapped or not, since that leads to different
         * sampling procedures. Also, we propogate non-snapped states
         * differently.
         */
        final double currentScheduleDev = BlockStateObservation.computeScheduleDeviation(
            obs, parentBlockStateObs.getBlockState());
        final boolean runChanged = hasRunChanged(parentBlockStateObs,
            proposalEdge);

        if (proposalEdge.isSnapped()) {
          // newEdge =
          // _blockStateSamplingStrategy.sampleGpsObservationState(proposalEdge,
          // obs);
          newEdge = Maps.immutableEntry(BlockSampleType.NOT_SAMPLED, proposalEdge);
        } else if (runChanged) {
          newEdge = Maps.immutableEntry(BlockSampleType.SCHEDULE_STATE_SAMPLE, 
              _blockStateSamplingStrategy.samplePriorScheduleState(
                proposalEdge.getBlockState().getBlockInstance(), obs));
        } else {
          /*
           * When our previous journey state was deadhead-before we don't want
           * to "jump the gun" and expect it to be at the start of the first
           * trip and moving along, so we wait for a snapped location to appear.
           */
          if (EVehiclePhase.AT_BASE == parentPhase) {
            newEdge = Maps.immutableEntry(BlockSampleType.NOT_SAMPLED, parentBlockStateObs);
          } else if (EVehiclePhase.isActiveDuringBlock(parentPhase)
              || (EVehiclePhase.isActiveBeforeBlock(parentPhase) && FastMath.abs(currentScheduleDev) > 0.0)) {
            /*
             * If it's active in the block or deadhead_before after the start
             * time...
             */
            newEdge = Maps.immutableEntry(BlockSampleType.EDGE_MOVEMENT_SAMPLE, 
                _blockStateSamplingStrategy.sampleTransitionDistanceState(
                  parentBlockStateObs, obs, vehicleNotMoved, parentPhase));
          } else {
            newEdge = Maps.immutableEntry(BlockSampleType.NOT_SAMPLED, parentBlockStateObs);
          }
        }
      } else {
        if (proposalEdge.isSnapped()) {
          // newEdge =
          // _blockStateSamplingStrategy.sampleGpsObservationState(proposalEdge,
          // obs);
          newEdge = Maps.immutableEntry(BlockSampleType.NOT_SAMPLED, proposalEdge);
        } else {
          newEdge = Maps.immutableEntry(BlockSampleType.SCHEDULE_STATE_SAMPLE, 
              _blockStateSamplingStrategy.samplePriorScheduleState(
                proposalEdge.getBlockState().getBlockInstance(), obs));
        }
      }
    } else {
      newEdge = Maps.immutableEntry(BlockSampleType.NOT_SAMPLED, null);
    }

    return newEdge;
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
        if (!proposalEdge.getBlockState().getRunId().equals(
            parentEdge.getBlockState().getRunId())) {
          return true;
        } else {
          return false;
        }
      } else {
        return true;
      }
    } else if (parentEdge != null) {
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
      results.add(new Particle(timestamp, parent, 0.0, vs));
  }

  public MotionState updateMotionState(Observation obs) {
    return updateMotionState(null, obs);
  }

  public static MotionState updateMotionState(VehicleState parentState,
      Observation obs) {

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

  public GpsLikelihood getGpsLikelihood() {
    return gpsLikelihood;
  }
}
