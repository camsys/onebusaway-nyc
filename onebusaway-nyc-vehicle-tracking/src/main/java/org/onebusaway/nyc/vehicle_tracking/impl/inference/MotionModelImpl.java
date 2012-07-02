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
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions.CategoricalDist;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.DscLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.EdgeLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.GpsLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.NullLocationLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.NullStateLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.RunLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.RunTransitionLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.ScheduleLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.disabled.PriorRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.realtime.api.EVehiclePhase;

import gov.sandia.cognition.math.LogMath;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Sets;

import org.apache.commons.math.util.FastMath;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Motion model implementation for vehicle location inference. Determine if the
 * vehicle is in motion, and propagate the particles' positions or states.
 * 
 * @author bdferris, bwillard
 */
public class MotionModelImpl implements MotionModel<Observation> {

  private static final double _essNoRunInfoTransitionThreshold = 0.60;
  private static final double _essRunInfoTransitionThreshold = 0.65;

  private BlocksFromObservationService _blocksFromObservationService;
  private SensorModelSupportLibrary _sensorModelLibrary;

  /**
   * Distance, in meters, that a bus has to travel to be considered "in motion"
   */
  static private double _motionThreshold = 20;

  @Autowired
  public void setJourneyMotionModel(
      JourneyStateTransitionModel journeyMotionModel) {
  }

  @Autowired
  public void setDestinationSignCodeService(
      DestinationSignCodeService destinationSignCodeService) {
  }

  @Autowired
  public void setSensorModelLibrary(SensorModelSupportLibrary sensorModelLibrary) {
    _sensorModelLibrary = sensorModelLibrary;
  }
  
  @Autowired
  public void setBlocksFromObservationService(
      BlocksFromObservationService blocksFromObservationService) {
    _blocksFromObservationService = blocksFromObservationService;
  }

  @Autowired
  public void setBlocksStateTransitionModel(
      BlockStateTransitionModel blockStateTransitionModel) {
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
  public RunLikelihood runLikelihood = new RunLikelihood();
  public DscLikelihood dscLikelihood;
  public NullStateLikelihood nullStateLikelihood;
  public NullLocationLikelihood nullLocationLikelihood;

  private JourneyStateTransitionModel _journeyStateTransitionModel;

  private BlockStateSamplingStrategy _blockStateSamplingStrategy;

  @Autowired
  public void setNullStateLikelihood(NullStateLikelihood nullStateLikelihood) {
    this.nullStateLikelihood = nullStateLikelihood;
  }

  @Autowired
  public void setNullLocationLikelihood(
      NullLocationLikelihood nullLocationLikelihood) {
    this.nullLocationLikelihood = nullLocationLikelihood;
  }

  @Autowired
  public void setDestinationSignCodeRule(DscLikelihood dscRule) {
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

  private boolean allowParentBlockTransition(VehicleState parentState,
      Observation obs) {

    final BlockStateObservation parentBlockState = parentState.getBlockStateObservation();

    if (parentBlockState == null
        && (!obs.hasOutOfServiceDsc() && obs.hasValidDsc()))
      return true;

    if (parentBlockState != null) {
      if (!parentBlockState.isRunFormal()
          && _blocksFromObservationService.hasSnappedBlockStates(obs)
          && obs.hasValidDsc()) {

        /*
         * We have no good run information, but a valid DSC, so we will allow 
         * a run transition when there are snapped states and it's not in progress.
         * This way we're less likely to get deadheads with in-service signs 
         * floating along routes.
         */
        if (EVehiclePhase.IN_PROGRESS != parentState.getJourneyState().getPhase()) {
          return true;
        }
      }
    }

    if (obs.getPreviousObservation() != null) {
      /*
       * If we didn't have snapped states, and now we do.
       */
      final boolean hadNoSnappedStates = !_blocksFromObservationService.hasSnappedBlockStates(obs.getPreviousObservation());
      final boolean hasSnappedStates = _blocksFromObservationService.hasSnappedBlockStates(obs);
      if (hadNoSnappedStates && hasSnappedStates)
        return true;
    }

    return false;
  }

  private boolean allowGeneralBlockTransition(Observation obs, double ess,
      boolean previouslyResampled) {

    if (BlockStateTransitionModel.hasDestinationSignCodeChangedBetweenObservations(obs))
      return true;

    final double essResampleThreshold;
    if (Strings.isNullOrEmpty(obs.getOpAssignedRunId())
        && obs.getFuzzyMatchDistance() == null) {
      essResampleThreshold = _essNoRunInfoTransitionThreshold;
    } else {
      essResampleThreshold = _essRunInfoTransitionThreshold;
    }

    if (previouslyResampled
        && ess / ParticleFactoryImpl.getInitialNumberOfParticles() <= essResampleThreshold) {
      return true;
    }

    return false;
  }
  

  @Override
  public Multiset<Particle> move(Multiset<Particle> particles,
      double timestamp, double timeElapsed, Observation obs,
      boolean previouslyResampled) {

    final Multiset<Particle> results = HashMultiset.create();

    final double ess = ParticleFilter.getEffectiveSampleSize(particles);
    final boolean generalBlockTransition = allowGeneralBlockTransition(obs,
        ess, previouslyResampled);
    double normOffset = Double.NEGATIVE_INFINITY;

    for (final Multiset.Entry<Particle> parent : particles.entrySet()) {
      final VehicleState parentState = parent.getElement().getData();
      final MotionState motionState = parentState.getMotionState();
      final BlockStateObservation parentBlockStateObs = parentState.getBlockStateObservation();

      final Set<BlockStateObservation> transitions = Sets.newHashSet();

      if (generalBlockTransition
          || allowParentBlockTransition(parentState, obs)) {
        /*
         * These are all the snapped and DSC/run blocks
         */
        transitions.addAll(_blocksFromObservationService.determinePotentialBlockStatesForObservation(obs));
      } else if (parentBlockStateObs != null) {
        /*
         * Only the snapped blocks
         */
        transitions.addAll(_blocksFromObservationService.advanceState(obs,
            parentState.getBlockState(), -1.98 * GpsLikelihood.gpsStdDev,
            Double.POSITIVE_INFINITY));
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

      /*
       *  We make a subtle distinction here, by allowing the previous state to remain as a transition
       *  but unaltered.  This helps in cases of deadhead->in-progress transitions, since, later on,
       *  the block state we create in the following will be treated differently, signaled by way of 
       *  pointer equality.
       */
      
      final double vehicleHasNotMovedProb = SensorModelSupportLibrary.computeVehicleHasNotMovedProbability(
          motionState, obs);
      
      for (int i = 0; i < parent.getCount(); ++i) {
        final Particle sampledParticle = sampleTransitionParticle(parent,
            newParentBlockStateObs, obs, vehicleHasNotMovedProb, transitions);
        normOffset = LogMath.add(sampledParticle.getLogWeight(), normOffset);
        results.add(sampledParticle);
      }
    }

    /*
     * Normalize
     */
     for (Entry<Particle> p : results.entrySet()) {
       p.getElement().setLogNormedWeight(p.getElement().getLogWeight()
           + FastMath.log(p.getCount()) - normOffset);
     }

    return results;
  }

  @SuppressWarnings({"unchecked", "rawtypes", "null"})
  private Particle sampleTransitionParticle(Entry<Particle> parent,
      BlockStateObservation newParentBlockStateObs, Observation obs,
      final double vehicleHasNotMovedProb,
      Set<BlockStateObservation> transitions) {

    final long timestamp = obs.getTime();
    final VehicleState parentState = parent.getElement().getData();

    final CategoricalDist<Particle> transitionDist = new CategoricalDist<Particle>();

    Multiset<Particle> debugTransitions = null;
    if (ParticleFilter.getDebugEnabled())
      debugTransitions = HashMultiset.create();

    for (final BlockStateObservation proposalEdge : transitions) {

      final SensorModelResult transProb = new SensorModelResult("Transition");
      final double inMotionSample = ParticleFactoryImpl.getThreadLocalRng().get().nextDouble();
      final boolean vehicleNotMoved = inMotionSample < vehicleHasNotMovedProb;
      final MotionState motionState = updateMotionState(parentState, obs,
          vehicleNotMoved);
      
      final Map.Entry<BlockSampleType, BlockStateObservation> newEdge;
      JourneyState journeyState;
      /*
       * We have to allow for a driver to start a trip late, so
       * we check to see if during the transition it was snapped or not.
       * If not, then we assume it may still be not in service
       */
      newEdge = sampleEdgeFromProposal(
        newParentBlockStateObs, proposalEdge, obs,
        parentState.getJourneyState().getPhase(), vehicleNotMoved);
      journeyState = _journeyStateTransitionModel.getJourneyState(
          newEdge.getValue(), parentState, obs, vehicleNotMoved);
      
      final boolean isLayoverStopped = JourneyStateTransitionModel.isLayoverStopped(
          vehicleNotMoved, obs.getTimeDelta());
      journeyState = adjustInProgressTransition(parentState, newEdge, 
          journeyState, isLayoverStopped);
      
      final VehicleState newState = new VehicleState(motionState,
          newEdge.getValue(), journeyState, null, obs);
      final Context context = new Context(parentState, newState, obs);

      /*
       * Edge movement
       */
      transProb.addResultAsAnd(edgeLikelihood.likelihood(_sensorModelLibrary, context));

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
      transProb.addResultAsAnd(schedLikelihood.likelihood(null, context));

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

      /*
       * Vehicle movement prob.
       */
      SensorModelResult movementResult = new SensorModelResult("movement");
      if (vehicleNotMoved) {
        movementResult.addResultAsAnd(new SensorModelResult("not-moved", vehicleHasNotMovedProb));
      } else {
        movementResult.addResultAsAnd(new SensorModelResult("moved", 1d-vehicleHasNotMovedProb));
      }
      transProb.addResultAsAnd(movementResult);
      
      transProb.addLogResultAsAnd(newEdge.getKey().name(), 0);

      final Particle newParticle = new Particle(timestamp, parent.getElement(),
          0.0, newState);
      newParticle.setResult(transProb);

      if (ParticleFilter.getDebugEnabled())
        debugTransitions.add(newParticle);

      transitionDist.logPut(transProb.getLogProbability(), newParticle);
    }

    final Particle newParticle;
    if (transitionDist.canSample()) {
      newParticle = transitionDist.sample();
      newParticle.setLogWeight(parent.getElement().getLogWeight()
          + newParticle.getResult().getLogProbability());

    } else {
      final SensorModelResult transProb = new SensorModelResult(
          "Transition (null)");
      final double inMotionSample = ParticleFactoryImpl.getThreadLocalRng().get().nextDouble();
      final boolean vehicleNotMoved = inMotionSample < vehicleHasNotMovedProb;
      final MotionState motionState = updateMotionState(parentState, obs,
          vehicleNotMoved);
      final JourneyState journeyState = _journeyStateTransitionModel.getJourneyState(
          null, null, obs, vehicleNotMoved);
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
      transProb.addResultAsAnd(new SensorModelResult("not-moved",
          vehicleNotMoved ? vehicleHasNotMovedProb
              : 1d - vehicleHasNotMovedProb));
      newParticle = new Particle(timestamp, parent.getElement(),
          0.0, nullState);
      newParticle.setResult(transProb);
      newParticle.setLogWeight(parent.getElement().getLogWeight()
          + newParticle.getResult().getLogProbability());

    }
    
    if (ParticleFilter.getDebugEnabled())
      newParticle.setTransitions(debugTransitions);
    
    return newParticle; 
  }

  /**
   * The strict schedule-time based journey state assignment doesn't
   * work for late/early transitions to in-progess, since in-progress 
   * states that are produced will be judged harshly if they're not
   * exactly on the geometry.  This method works around that. 
   * @param parentState
   * @param newEdge
   * @param journeyState
   * @return
   */
  private JourneyState adjustInProgressTransition(VehicleState parentState,
      java.util.Map.Entry<BlockSampleType, BlockStateObservation> newEdge,
      JourneyState journeyState, boolean isLayoverStopped) {
    
    if (!parentState.getJourneyState().getPhase().equals(EVehiclePhase.IN_PROGRESS)
        && journeyState.getPhase().equals(EVehiclePhase.IN_PROGRESS)
        && !newEdge.getValue().isSnapped()) {
      final boolean wasPrevStateDuring = EVehiclePhase.isActiveDuringBlock(
          parentState.getJourneyState().getPhase());
      if (EVehiclePhase.isLayover(parentState.getJourneyState().getPhase())
          && !isLayoverStopped) {
        return wasPrevStateDuring ? JourneyState.deadheadDuring(null)
            : JourneyState.deadheadBefore(null);
      } else if (!EVehiclePhase.isLayover(parentState.getJourneyState().getPhase())
          && isLayoverStopped) {
        return wasPrevStateDuring ? JourneyState.layoverDuring()
            : JourneyState.layoverBefore();
      } else {
        return parentState.getJourneyState();
      }
    } else {
      return journeyState;
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
          newEdge = Maps.immutableEntry(BlockSampleType.NOT_SAMPLED,
              proposalEdge);
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
            newEdge = Maps.immutableEntry(BlockSampleType.NOT_SAMPLED,
                parentBlockStateObs);
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
            newEdge = Maps.immutableEntry(BlockSampleType.NOT_SAMPLED,
                parentBlockStateObs);
          }
        }
      } else {
        if (proposalEdge.isSnapped()) {
          // newEdge =
          // _blockStateSamplingStrategy.sampleGpsObservationState(proposalEdge,
          // obs);
          newEdge = Maps.immutableEntry(BlockSampleType.NOT_SAMPLED,
              proposalEdge);
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

  // @Override
  // public void move(Particle parent, double timestamp, double timeElapsed,
  // Observation obs, Collection<Particle> results) {
  //
  // final VehicleState parentState = parent.getData();
  //
  // final MotionState motionState = updateMotionState(parentState, obs, false);
  //
  // final List<VehicleState> vehicleStates = new ArrayList<VehicleState>();
  // _journeyMotionModel.move(parentState, motionState, obs, vehicleStates);
  //
  // for (final VehicleState vs : vehicleStates)
  // results.add(new Particle(timestamp, parent, 0.0, vs));
  // }

  public MotionState updateMotionState(Observation obs,
      boolean vehicleHasNotMoved) {
    return updateMotionState(null, obs, vehicleHasNotMoved);
  }

  public static MotionState updateMotionState(VehicleState parentState,
      Observation obs, boolean vehicleHasNotMoved) {

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

    return new MotionState(lastInMotionTime, lastInMotionLocation,
        vehicleHasNotMoved);
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
