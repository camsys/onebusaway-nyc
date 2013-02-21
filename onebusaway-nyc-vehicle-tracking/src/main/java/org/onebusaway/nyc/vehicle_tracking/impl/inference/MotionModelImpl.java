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

import gov.sandia.cognition.math.LogMath;

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.math.util.FastMath;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.transit_data_federation.services.nyc.DestinationSignCodeService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.DscLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.EdgeLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.GpsLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.MovedLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.NullLocationLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.NullStateLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.RunLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.RunTransitionLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.ScheduleLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;
import org.onebusaway.nyc.vehicle_tracking.model.NycRawLocationRecord;
import org.onebusaway.nyc.vehicle_tracking.model.library.TurboButton;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Sets;

/**
 * Motion model implementation for vehicle location inference. Determine if the
 * vehicle is in motion, and propagate the particles' positions or states.
 * 
 * @author bdferris, bwillard
 */
public class MotionModelImpl implements MotionModel<Observation> {
 
  /**
   * Effective sample size thresholds that trigger resampling.
   */
  private static final double _essNoRunInfoTransitionThreshold = 0.60;
  private static final double _essRunInfoTransitionThreshold = 0.65;

  private BlocksFromObservationService _blocksFromObservationService;

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
  public void setBlocksFromObservationService(
      BlocksFromObservationService blocksFromObservationService) {
    _blocksFromObservationService = blocksFromObservationService;
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
  public RunLikelihood runLikelihood = new RunLikelihood();
  public MovedLikelihood movedLikelihood = new MovedLikelihood();
  /*
   * Some of these likelihoods need services attached.
   */
  public DscLikelihood dscLikelihood;
  public NullStateLikelihood nullStateLikelihood;
  public NullLocationLikelihood nullLocationLikelihood;

  private JourneyStateTransitionModel _journeyStateTransitionModel;

  private BlockStateSamplingStrategy _blockStateSamplingStrategy;
  private final static double _distancePastEndMargin = 100d;

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
      
      /*
       * When a bus is waiting to start a block within the snapping distance
       * and then it starts moving, we want to make sure that  
       */
      if (EVehiclePhase.isActiveBeforeBlock(parentState.getJourneyState().getPhase())
          && parentBlockState.isSnapped() 
          && _blocksFromObservationService.hasSnappedBlockStates(obs)) {
        return true;
      }
      
      if (!parentBlockState.isRunFormal()
          && obs.hasValidDsc()) {
        
        /*
         * Always expect that we can go off from a detour when
         * the run-info is bad.
         */
        if (parentState.getJourneyState().getIsDetour()) {
          return true;
        }
        
        /*
         * We have no good run information, but a valid DSC, so we will allow a
         * run transition when there are snapped states and it's not in
         * progress. This way we're less likely to get deadheads with in-service
         * signs floating along routes.
         */
        if (_blocksFromObservationService.hasSnappedBlockStates(obs)) {
          if (EVehiclePhase.IN_PROGRESS != parentState.getJourneyState().getPhase()) {
            return true;
          }
        }
        
        /*
         * If we have no good run info and we're near/at the end of trip and our
         * observations moved a distance past the end of the trip.
         */
        if (JourneyStateTransitionModel.isLocationOnATrip(parentBlockState.getBlockState())) {
          final double tripDab = parentBlockState.getBlockState().getBlockLocation().getDistanceAlongBlock()
              - parentBlockState.getBlockState().getBlockLocation().getActiveTrip().getDistanceAlongBlock();
          final double distanceMoved = TurboButton.distance(obs.getLocation(), 
              obs.getPreviousObservation().getLocation());
          if (tripDab + distanceMoved + _distancePastEndMargin >= 
              parentBlockState.getBlockState().getBlockLocation().getActiveTrip().getTrip().getTotalTripDistance()) {
            return true;
          }
        }
      }
      
      /*
       * If we've finished a run/block and changed our dsc to a valid one, then
       * keep checking for new states.
       */
      if (parentState.getJourneyState().getPhase().equals(EVehiclePhase.DEADHEAD_AFTER)
          && obs.hasValidDsc() && !Objects.equal(obs.getLastValidDestinationSignCode(), 
              parentState.getBlockState().getDestinationSignCode()))
        return true;
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

  public static boolean hasDestinationSignCodeChangedBetweenObservations(
      Observation obs) {

    String previouslyObservedDsc = null;
    final NycRawLocationRecord previousRecord = obs.getPreviousRecord();
    if (previousRecord != null)
      previouslyObservedDsc = previousRecord.getDestinationSignCode();

    final NycRawLocationRecord record = obs.getRecord();
    final String observedDsc = record.getDestinationSignCode();

    return !ObjectUtils.equals(previouslyObservedDsc, observedDsc);
  }
  
  private boolean allowGeneralBlockTransition(Observation obs, double ess,
      boolean previouslyResampled, boolean anySnapped) {

    if (hasDestinationSignCodeChangedBetweenObservations(obs))
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
    
    /*
     * If none of our current particles are snapped, yet there were snapped states and there are
     * currently snapped states, then resample, since there's usually a problem when nothing is snapped
     * in these cases.
     */
    if (obs.getPreviousObservation() != null) {
      final boolean hadSnappedStates = _blocksFromObservationService.hasSnappedBlockStates(obs.getPreviousObservation());
      final boolean hasSnappedStates = _blocksFromObservationService.hasSnappedBlockStates(obs);
      if (hasSnappedStates && hadSnappedStates && !anySnapped) {
        return true;
      }
    }

    return false;
  }

  @Override
  public Multiset<Particle> move(Multiset<Particle> particles,
      double timestamp, double timeElapsed, Observation obs,
      boolean previouslyResampled) throws ParticleFilterException {

    final Multiset<Particle> results = HashMultiset.create();

    boolean anySnapped = false;
    /*
     * TODO FIXME could keep a particle-set-info record...
     */
    for (final Multiset.Entry<Particle> parent : particles.entrySet()) {
      final VehicleState state = parent.getElement().getData();
      if (state.getBlockStateObservation() != null 
          && state.getBlockStateObservation().isSnapped()) {
        anySnapped = true;
        break;
      }
    }
    final double ess = ParticleFilter.getEffectiveSampleSize(particles);
    final boolean generalBlockTransition = allowGeneralBlockTransition(obs,
        ess, previouslyResampled, anySnapped);
    double normOffset = Double.NEGATIVE_INFINITY;

    for (final Multiset.Entry<Particle> parent : particles.entrySet()) {
      final VehicleState parentState = parent.getElement().getData();
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
         * Only the snapped blocks.
         * We are also allowing changes to snapped in-progress states when
         * they were sampled well outside of allowed backward search distance.
         */
        final double backwardDistance = Double.NEGATIVE_INFINITY;        
        transitions.addAll(_blocksFromObservationService.advanceState(obs,
            parentState.getBlockState(), backwardDistance,
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
       * We make a subtle distinction here, by allowing the previous state to
       * remain as a transition but unaltered. This helps in cases of
       * deadhead->in-progress transitions, since, later on, the block state we
       * create in the following will be treated differently, signaled by way of
       * pointer equality.
       */

      final double vehicleHasNotMovedProb = MovedLikelihood.computeVehicleHasNotMovedProbability(obs);

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
    for (final Entry<Particle> p : results.entrySet()) {
      final double thisTotalWeight = p.getElement().getLogWeight() + FastMath.log(p.getCount());
      double logNormed = thisTotalWeight - normOffset;
      if (logNormed > 0d)
        logNormed = 0d;
      p.getElement().setLogNormedWeight(logNormed);
    }

    return results;
  }

  private Particle sampleTransitionParticle(Entry<Particle> parent,
      BlockStateObservation newParentBlockStateObs, Observation obs,
      final double vehicleHasNotMovedProb,
      Set<BlockStateObservation> transitions) throws ParticleFilterException {

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
       * We have to allow for a driver to start a trip late, so we check to see
       * if during the transition it was snapped or not. If not, then we assume
       * it may still be not in service
       */
      newEdge = sampleEdgeFromProposal(newParentBlockStateObs, proposalEdge,
          obs, parentState.getJourneyState().getPhase(), vehicleNotMoved);
      journeyState = _journeyStateTransitionModel.getJourneyState(
          newEdge.getValue(), parentState, obs, vehicleNotMoved);

      final VehicleState newState = new VehicleState(motionState,
          newEdge.getValue(), journeyState, null, obs);
      final Context context = new Context(parentState, newState, obs);

      transProb.addResultAsAnd(edgeLikelihood.likelihood(context));
      transProb.addResultAsAnd(gpsLikelihood.likelihood(context));
      transProb.addResultAsAnd(dscLikelihood.likelihood(context));
      transProb.addResultAsAnd(runLikelihood.likelihood(context));
      transProb.addResultAsAnd(schedLikelihood.likelihood(context));
      transProb.addResultAsAnd(runTransitionLikelihood.likelihood(context));
      transProb.addResultAsAnd(nullStateLikelihood.likelihood(context));
      transProb.addResultAsAnd(nullLocationLikelihood.likelihood(context));
      transProb.addResultAsAnd(movedLikelihood.likelihood(context));

      /*
       * TODO: this is mainly for debug and can/should be removed.
       */
      transProb.addLogResultAsAnd(newEdge.getKey().name(), 0);

      final Particle newParticle = new Particle(timestamp, parent.getElement(),
          0.0, newState);
      newParticle.setResult(transProb);

      if (debugTransitions != null) {
        final double logWeight = parent.getElement().getLogWeight()
            + newParticle.getResult().getLogProbability();
        newParticle.setLogWeight(logWeight);
        debugTransitions.add(newParticle);
      }

      transitionDist.logPut(transProb.getLogProbability(), newParticle);
    }

    final Particle newParticle;
    if (transitionDist.canSample()) {
      newParticle = transitionDist.sample();
      final double logWeight = parent.getElement().getLogWeight()
          + newParticle.getResult().getLogProbability();
      newParticle.setLogWeight(logWeight);

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
      transProb.addResultAsAnd(edgeLikelihood.likelihood(context));
      transProb.addResultAsAnd(gpsLikelihood.likelihood(context));
      transProb.addResultAsAnd(dscLikelihood.likelihood(context));
      transProb.addResultAsAnd(runLikelihood.likelihood(context));
      transProb.addResultAsAnd(schedLikelihood.likelihood(context));
      transProb.addResultAsAnd(runTransitionLikelihood.likelihood(context));
      transProb.addResultAsAnd(nullStateLikelihood.likelihood(context));
      transProb.addResultAsAnd(nullLocationLikelihood.likelihood(context));
      transProb.addResultAsAnd(movedLikelihood.likelihood(context));
      newParticle = new Particle(timestamp, parent.getElement(), 0.0, nullState);
      newParticle.setResult(transProb);
      final double logWeight = parent.getElement().getLogWeight()
          + newParticle.getResult().getLogProbability();
      newParticle.setLogWeight(logWeight);

    }

    if (ParticleFilter.getDebugEnabled())
      newParticle.setTransitions(debugTransitions);

    return newParticle;
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
         * sampling procedures. Also, we propagate non-snapped states
         * differently.
         */
        final double currentScheduleDev = BlockStateObservation.computeScheduleDeviation(
            obs, parentBlockStateObs.getBlockState());
        final boolean runChanged = hasRunChanged(parentBlockStateObs,
            proposalEdge);

        if (proposalEdge.isSnapped()) {
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
          } else if (EVehiclePhase.isActiveDuringBlock(parentPhase)) {
            newEdge = Maps.immutableEntry(BlockSampleType.EDGE_MOVEMENT_SAMPLE,
                _blockStateSamplingStrategy.sampleTransitionDistanceState(
                    parentBlockStateObs, obs, vehicleNotMoved, parentPhase));
          } else if (EVehiclePhase.isActiveBeforeBlock(parentPhase) && FastMath.abs(currentScheduleDev) > 0.0) {
            /*
             * Continue sampling mid-trip joins for this block...
             */
            newEdge = Maps.immutableEntry(BlockSampleType.SCHEDULE_STATE_SAMPLE,
                _blockStateSamplingStrategy.samplePriorScheduleState(
                    parentBlockStateObs.getBlockState().getBlockInstance(), obs));
          } else {
            newEdge = Maps.immutableEntry(BlockSampleType.NOT_SAMPLED,
                parentBlockStateObs);
          }
        }
      } else {
        if (proposalEdge.isSnapped()) {
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

  public static boolean hasRunChanged(BlockStateObservation parentEdge,
      BlockStateObservation proposalEdge) {
    if (proposalEdge != null) {
      if (parentEdge != null) {
        if (proposalEdge.getBlockState().getRunId() != null &&
            parentEdge.getBlockState().getRunId() != null &&
            !proposalEdge.getBlockState().getRunId().equals(
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

      final double d = TurboButton.distance(
          motionState.getLastInMotionLocation(), obs.getLocation());

      /*
       * FIXME there's an inconsistency here (with having sampled
       * whether we moved or not).
       */
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
