package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.JourneyStateTransitionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MtaPathStateBelief;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.MtaTrackingGraph.BlockTripEntryAndDate;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.MtaTrackingGraph.TripInfo;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.RunState.RunStateEdgePredictiveResults;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.InstanceState;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;

import gov.sandia.cognition.learning.algorithm.AbstractBatchAndIncrementalLearner;
import gov.sandia.cognition.math.LogMath;
import gov.sandia.cognition.statistics.ComputableDistribution;
import gov.sandia.cognition.statistics.DataDistribution;
import gov.sandia.cognition.statistics.bayesian.BayesianEstimatorPredictor;
import gov.sandia.cognition.util.AbstractCloneableSerializable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.opentrackingtools.graph.InferenceGraph;
import org.opentrackingtools.graph.paths.edges.PathEdge;
import org.opentrackingtools.graph.paths.edges.impl.EdgePredictiveResults;
import org.opentrackingtools.graph.paths.states.PathState;
import org.opentrackingtools.graph.paths.states.PathStateBelief;
import org.opentrackingtools.impl.WrappedWeightedValue;
import org.opentrackingtools.statistics.distributions.impl.DefaultCountedDataDistribution;
import org.opentrackingtools.statistics.distributions.impl.DeterministicDataDistribution;
import org.opentrackingtools.statistics.filters.vehicles.road.impl.AbstractRoadTrackingFilter;

import umontreal.iro.lecuyer.probdist.FoldedNormalDist;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * An estimator/predictor that allows for predictive sampling of a run/block state.
 * The current implementation simply returns a discrete distribution over
 * the possible runs, with densities determined by their likelihood.
 * Basically, we have a deterministic prior.
 * 
 * @author bwillard
 *
 */
public class RunStateEstimator extends AbstractBatchAndIncrementalLearner<RunState, DataDistribution<RunState>> implements
    BayesianEstimatorPredictor<RunState, RunState, DataDistribution<RunState>> {
  
  private static final long serialVersionUID = -1461026886038720233L;
  
  private final MtaTrackingGraph mtaGraph;
  private final PathStateBelief pathStateBelief;
  private final VehicleState prevOldTypeVehicleState;
  private final Observation obs;
  private final Random rng;
  
  private static final long _tripSearchTimeAfterLastStop = 5 * 60 * 60 * 1000;
  private static final long _tripSearchTimeBeforeFirstStop = 5 * 60 * 60 * 1000;
  
  public static long getTripSearchTimeAfterLastStop() {
    return _tripSearchTimeAfterLastStop;
  }

  public static long getTripSearchTimeBeforeFirstStop() {
    return _tripSearchTimeBeforeFirstStop;
  }

  public RunStateEstimator(MtaTrackingGraph graph, Observation obs, 
      PathStateBelief pathStateBelief, VehicleState prevOldTypeVehicleState, Random rng) {
    this.obs = obs;
    this.mtaGraph = graph;
    this.pathStateBelief = pathStateBelief;
    this.prevOldTypeVehicleState = prevOldTypeVehicleState;
    this.rng = rng;
  }

  @Override
  public DataDistribution<RunState> createPredictiveDistribution(
      DataDistribution<RunState> posterior) {
    return createInitialLearnedObject();
  }
    
  @Override
  public DataDistribution<RunState> createInitialLearnedObject() {
    
    DefaultCountedDataDistribution<RunState> result = new DefaultCountedDataDistribution<RunState>(true);
    
    final long time = obs.getTimestamp().getTime();       
    final Date timeFrom = new Date(time - _tripSearchTimeAfterLastStop);
    final Date timeTo = new Date(time + _tripSearchTimeBeforeFirstStop);
    
    final PathEdge pathEdge = pathStateBelief.getEdge();
    final TripInfo tripInfo = mtaGraph.getTripInfo(pathEdge.getInferredEdge());
    final double likelihoodHasNotMoved = likelihoodOfNotMovedState(this.pathStateBelief);
    
    if (tripInfo != null) {
      /*
       * In this case our previous state was snapped to particular road segment for 
       * run/block set, so find all the active ones in our time window, produce states 
       * for them, and weigh.
       */
      Collection<BlockTripEntryAndDate> activeEntries = tripInfo.getTimeIndex().query(timeFrom.getTime(), timeTo.getTime());
      for (BlockTripEntryAndDate blockTripEntryAndDate: activeEntries) {
        
        final BlockTripEntry blockTripEntry = blockTripEntryAndDate.getBlockTripEntry();
        long serviceDate = blockTripEntryAndDate.getServiceDate().getTime();
        
        final BlockStateObservation blockStateObs = this.mtaGraph.getBlockStateObs(obs, pathStateBelief, 
            blockTripEntry, serviceDate);
        
        final RunState runStateMoved = new RunState(mtaGraph, obs, blockStateObs, false, this.prevOldTypeVehicleState);
        
        final RunState runStateNotMoved = new RunState(mtaGraph, obs, blockStateObs, true, this.prevOldTypeVehicleState);
        
        final RunState.RunStateEdgePredictiveResults mtaEdgeResultsMoved = runStateMoved.
            computeAnnotatedLogLikelihood();
        if (mtaEdgeResultsMoved.getTotalLogLik() > Double.NEGATIVE_INFINITY) {
          result.increment(runStateMoved, mtaEdgeResultsMoved.getTotalLogLik());
        }
        final RunState.RunStateEdgePredictiveResults mtaEdgeResultsNotMoved = runStateNotMoved.
            computeAnnotatedLogLikelihood();
        if (mtaEdgeResultsNotMoved.getTotalLogLik() > Double.NEGATIVE_INFINITY) {
          result.increment(runStateNotMoved, mtaEdgeResultsNotMoved.getTotalLogLik());
        }
      }
    } 
    
    /*
     * Our state wasn't on a road corresponding to a run/block set, or it wasn't
     * on a road at all.
     * We add null to ensure a run/block-less possibility.
     */
    Collection<BlockStateObservation> blockStates = mtaGraph.getBlockStatesFromObservation((Observation)obs);
    blockStates.add(null);
    for (BlockStateObservation blockStateObs : blockStates) {
      
      /*
       * We don't want to infer a run with road location when the state
       * isn't on one.
       * We do allow a null run for an on-road state, though.  This comes in 
       * handy when we add more streets to the graph (ones that don't correspond
       * to routes).
       */
      if (blockStateObs != null 
          && (JourneyStateTransitionModel.isLocationOnATrip(blockStateObs.getBlockState())
              || pathStateBelief.isOnRoad()))
        continue;
      
      final Long serviceDate;
      final BlockTripEntry blockTripEntry;
      if (blockStateObs != null) {
        blockTripEntry = blockStateObs.getBlockState().getBlockLocation().getActiveTrip();
        serviceDate = blockStateObs.getBlockState().getBlockInstance().getServiceDate();
      } else {
        blockTripEntry = MtaPathEdge.getNullBlockTripEntry();
        serviceDate = null;
      }
      
      final RunState runStateMoved = new RunState(mtaGraph, obs, blockStateObs, false, this.prevOldTypeVehicleState);
      Preconditions.checkState(runStateMoved.getJourneyState().getPhase() != EVehiclePhase.IN_PROGRESS);
      
      Preconditions.checkState(!pathEdge.isNullEdge() 
          || (runStateMoved.getJourneyState() != JourneyState.inProgress()));
      
      final RunState runStateNotMoved = new RunState(mtaGraph, obs, blockStateObs, true, this.prevOldTypeVehicleState);
      Preconditions.checkState(runStateNotMoved.getJourneyState().getPhase() != EVehiclePhase.IN_PROGRESS);
      
      Preconditions.checkState(!pathEdge.isNullEdge() 
          || (runStateNotMoved.getJourneyState() != JourneyState.inProgress()));
      
      final RunState.RunStateEdgePredictiveResults mtaEdgeResultsMoved = runStateMoved.
          computeAnnotatedLogLikelihood();
      if (mtaEdgeResultsMoved.getTotalLogLik() > Double.NEGATIVE_INFINITY) {
        result.increment(runStateMoved, mtaEdgeResultsMoved.getTotalLogLik());
      }
      final RunState.RunStateEdgePredictiveResults mtaEdgeResultsNotMoved = runStateNotMoved.
          computeAnnotatedLogLikelihood();
      if (mtaEdgeResultsNotMoved.getTotalLogLik() > Double.NEGATIVE_INFINITY) {
        result.increment(runStateNotMoved, mtaEdgeResultsNotMoved.getTotalLogLik());
      }
      
    }
    
    return result;
  }

  /**
   * No-op: prior is deterministic
   */
  @Override
  public DeterministicDataDistribution<RunState> learn(
      Collection<? extends RunState> data) {
    return null;
  }
  
  /**
   * No-op: deterministic
   */
  @Override
  public void update(DataDistribution<RunState> target,
      RunState data) {
  }


  /**
   * Sample a state of moved/not-moved using a belief's velocity distribution.
   * <br>
   * This is done by evaluating the complement of a CDF from a folded normal,
   * with mean 0 and a variance from the Frobenius norm of the belief's velocity 
   * covariance, at the belief's velocity mean
   * 
   * @param pathStateBelief
   * @return
   */
  public double likelihoodOfNotMovedState(PathStateBelief pathStateBelief) {
    final double velocityAvg = 
        AbstractRoadTrackingFilter.getVg().times(
        pathStateBelief.getGroundState()).norm2();
    final double velocityVar =
        AbstractRoadTrackingFilter.getVg().times(
            pathStateBelief.getGroundBelief().getCovariance()).times(
                AbstractRoadTrackingFilter.getVg().transpose()).normFrobenius();
    
    final double likelihood = Math.min(1d, Math.max(0d, 
            1d - FoldedNormalDist.cdf(0d, Math.sqrt(velocityVar), velocityAvg)));
    
    return likelihood;
    
//    final boolean vehicleHasNotMoved;
//    if (rng.nextDouble() < likelihood)
//      vehicleHasNotMoved = true;
//    else
//      vehicleHasNotMoved = false;
//    
//    return vehicleHasNotMoved;
  }

}
