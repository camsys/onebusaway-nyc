package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.JourneyStateTransitionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.NycTrackingGraph.BlockTripEntryAndDate;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.NycTrackingGraph.TripInfo;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.RunState.RunStateEdgePredictiveResults;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.InstanceState;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;

import gov.sandia.cognition.learning.algorithm.AbstractBatchAndIncrementalLearner;
import gov.sandia.cognition.learning.algorithm.IncrementalLearner;
import gov.sandia.cognition.math.LogMath;
import gov.sandia.cognition.math.MutableDouble;
import gov.sandia.cognition.statistics.ComputableDistribution;
import gov.sandia.cognition.statistics.DataDistribution;
import gov.sandia.cognition.statistics.bayesian.BayesianEstimatorPredictor;
import gov.sandia.cognition.util.AbstractCloneableSerializable;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.opentrackingtools.distributions.CountedDataDistribution;
import org.opentrackingtools.distributions.DeterministicDataDistribution;
import org.opentrackingtools.distributions.PathStateDistribution;
import org.opentrackingtools.estimators.MotionStateEstimatorPredictor;
import org.opentrackingtools.graph.InferenceGraph;
import org.opentrackingtools.paths.PathEdge;
import org.opentrackingtools.util.model.MutableDoubleCount;

import umontreal.iro.lecuyer.probdist.FoldedNormalDist;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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
public class RunStateEstimator extends AbstractCloneableSerializable implements
  IncrementalLearner<PathStateDistribution, DataDistribution<RunState>>,
    BayesianEstimatorPredictor<RunState, RunState, DataDistribution<RunState>> {
  
  private static final long serialVersionUID = -1461026886038720233L;
  
  private final NycTrackingGraph nycGraph;
  private final PathStateDistribution pathStateDistribution;
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

  public RunStateEstimator(NycTrackingGraph graph, Observation obs, 
      PathStateDistribution pathStateBelief, VehicleState prevOldTypeVehicleState, Random rng) {
    this.obs = obs;
    this.nycGraph = graph;
    this.pathStateDistribution = pathStateBelief;
    this.prevOldTypeVehicleState = prevOldTypeVehicleState;
    this.rng = rng;
  }

  @Override
  public CountedDataDistribution<RunState> createPredictiveDistribution(
      DataDistribution<RunState> posterior) {
    return createInitialLearnedObject();
  }
    
  @Override
  public CountedDataDistribution<RunState> createInitialLearnedObject() {
    
    final long time = obs.getTimestamp().getTime();       
    final Date timeFrom = new Date(time - _tripSearchTimeAfterLastStop);
    final Date timeTo = new Date(time + _tripSearchTimeBeforeFirstStop);
    
    final PathEdge pathEdge = pathStateDistribution.getPathState().getEdge();
    final TripInfo tripInfo = nycGraph.getTripInfo(pathEdge.getInferenceGraphEdge());
    final double likelihoodHasNotMoved = likelihoodOfNotMovedState(this.pathStateDistribution);
    double nonNullTotalLikelihood = Double.NEGATIVE_INFINITY;
    
    Map<RunState, MutableDoubleCount> resultDist = Maps.newIdentityHashMap();
    
    if (tripInfo != null) {
      
      /*
       * In this case our previous state was snapped to particular road segment for 
       * run/block set, so find all the active ones in our time window, produce states 
       * for them, and weigh.
       */
      Collection<BlockTripEntryAndDate> activeEntries = tripInfo.getActiveTrips(timeFrom.getTime(), timeTo.getTime());
      for (BlockTripEntryAndDate blockTripEntryAndDate: activeEntries) {
        
        final BlockTripEntry blockTripEntry = blockTripEntryAndDate.getBlockTripEntry();
        long serviceDate = blockTripEntryAndDate.getServiceDate().getTime();
        
        final BlockStateObservation blockStateObs = this.nycGraph.getBlockStateObs(obs, 
            pathStateDistribution.getPathState(), blockTripEntry, serviceDate);
        
        final RunState runStateMoved = new RunState(nycGraph, obs, blockStateObs, false, this.prevOldTypeVehicleState);
        
        
        final RunState.RunStateEdgePredictiveResults mtaEdgeResultsMoved = runStateMoved.
            computeAnnotatedLogLikelihood();
        runStateMoved.setLikelihoodInfo(mtaEdgeResultsMoved);
        mtaEdgeResultsMoved.setMovedLogLikelihood(Math.log(1d-likelihoodHasNotMoved));
        if (mtaEdgeResultsMoved.getTotalLogLik() > Double.NEGATIVE_INFINITY) {
          nonNullTotalLikelihood = LogMath.add(nonNullTotalLikelihood, mtaEdgeResultsMoved.schedLogLikelihood);
          resultDist.put(runStateMoved, new MutableDoubleCount(mtaEdgeResultsMoved.getTotalLogLik(), 1));
        }
        
        final RunState runStateNotMoved = new RunState(nycGraph, obs, blockStateObs, true, this.prevOldTypeVehicleState);
        
        final RunState.RunStateEdgePredictiveResults mtaEdgeResultsNotMoved = runStateNotMoved.
            computeAnnotatedLogLikelihood();
        runStateNotMoved.setLikelihoodInfo(mtaEdgeResultsNotMoved);
        mtaEdgeResultsNotMoved.setMovedLogLikelihood(Math.log(likelihoodHasNotMoved));
        if (mtaEdgeResultsNotMoved.getTotalLogLik() > Double.NEGATIVE_INFINITY) {
          nonNullTotalLikelihood = LogMath.add(nonNullTotalLikelihood, mtaEdgeResultsNotMoved.schedLogLikelihood);
          resultDist.put(runStateNotMoved, new MutableDoubleCount(mtaEdgeResultsNotMoved.getTotalLogLik(), 1));
        }
      }
    } 
    
    /*
     * Our state wasn't on a road corresponding to a run/block set, or it wasn't
     * on a road at all.
     * We add null to ensure a run/block-less possibility.
     */
    Collection<BlockStateObservation> blockStates = nycGraph.getBlockStatesFromObservation((Observation)obs);
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
              || pathStateDistribution.getPathState().isOnRoad()))
        continue;
      
      final RunState runStateMoved = new RunState(nycGraph, obs, blockStateObs, false, this.prevOldTypeVehicleState);
      Preconditions.checkState(runStateMoved.getJourneyState().getPhase() != EVehiclePhase.IN_PROGRESS);
      
      Preconditions.checkState(!pathEdge.isNullEdge() 
          || (runStateMoved.getJourneyState() != JourneyState.inProgress()));
      
      final RunState.RunStateEdgePredictiveResults mtaEdgeResultsMoved = runStateMoved.
          computeAnnotatedLogLikelihood();
      runStateMoved.setLikelihoodInfo(mtaEdgeResultsMoved);
      mtaEdgeResultsMoved.setMovedLogLikelihood(Math.log(1d-likelihoodHasNotMoved));
      if (mtaEdgeResultsMoved.getTotalLogLik() > Double.NEGATIVE_INFINITY) {
        if (blockStateObs != null)
          nonNullTotalLikelihood = LogMath.add(nonNullTotalLikelihood, mtaEdgeResultsMoved.schedLogLikelihood);
        resultDist.put(runStateMoved, new MutableDoubleCount(mtaEdgeResultsMoved.getTotalLogLik(), 1));
      }
      
      final RunState runStateNotMoved = new RunState(nycGraph, obs, blockStateObs, true, this.prevOldTypeVehicleState);
      Preconditions.checkState(runStateNotMoved.getJourneyState().getPhase() != EVehiclePhase.IN_PROGRESS);
      
      Preconditions.checkState(!pathEdge.isNullEdge() 
          || (runStateNotMoved.getJourneyState() != JourneyState.inProgress()));
      
      final RunState.RunStateEdgePredictiveResults mtaEdgeResultsNotMoved = runStateNotMoved.
          computeAnnotatedLogLikelihood();
      runStateNotMoved.setLikelihoodInfo(mtaEdgeResultsNotMoved);
      mtaEdgeResultsNotMoved.setMovedLogLikelihood(Math.log(likelihoodHasNotMoved));
      if (mtaEdgeResultsNotMoved.getTotalLogLik() > Double.NEGATIVE_INFINITY) {
        if (blockStateObs != null)
          nonNullTotalLikelihood = LogMath.add(nonNullTotalLikelihood, mtaEdgeResultsNotMoved.schedLogLikelihood);
        resultDist.put(runStateNotMoved, new MutableDoubleCount(mtaEdgeResultsNotMoved.getTotalLogLik(), 1));
      }
      
    }
    
    /*
     * Now, normalize the non-null block states, so that comparisons
     * with null block states will be valid.
     */
    CountedDataDistribution<RunState> result = 
        new CountedDataDistribution<RunState>(true);
    for (Entry<RunState, MutableDoubleCount> entry : resultDist.entrySet()) {
      if (entry.getKey().getBlockStateObs() != null) {
        final double newValue = entry.getValue().doubleValue() + entry.getValue().count - nonNullTotalLikelihood;
        result.increment(entry.getKey(), newValue);
      } else {
        result.increment(entry.getKey(), entry.getValue().doubleValue() + entry.getValue().count);
      }
    }
    
    Preconditions.checkState(!result.isEmpty());
    // TODO debug. remove.
    if (this.pathStateDistribution.getPathState().isOnRoad() && result.getMaxValueKey().getBlockStateObs() == null)
        Preconditions.checkState(true);
    return result;
  }

  @Override
  public void update(DataDistribution<RunState> priorPredRunStateDist, 
      PathStateDistribution posteriorPathStateDist) {
    
    Preconditions.checkArgument(priorPredRunStateDist instanceof DeterministicDataDistribution<?>);
    /*
     * We must update update the run state, since the path belief gets updated. 
     */
    RunState priorPredRunState = priorPredRunStateDist.getMaxValueKey();
    if (priorPredRunState.getBlockStateObs() != null) {
      final ScheduledBlockLocation priorSchedLoc = priorPredRunState.getBlockStateObs().getBlockState().getBlockLocation();
      final BlockStateObservation newBlockStateObs = nycGraph.getBlockStateObs(obs, 
          posteriorPathStateDist.getPathState(), 
          priorSchedLoc.getActiveTrip(), 
          priorPredRunState.getBlockStateObs().getBlockState().getBlockInstance().getServiceDate());
      priorPredRunState.setBlockStateObs(newBlockStateObs);
    } 
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
  public double likelihoodOfNotMovedState(PathStateDistribution pathStateBelief) {
    final double velocityAvg = 
        MotionStateEstimatorPredictor.getVg().times(
        pathStateBelief.getGroundDistribution().getMean()).norm2();
    final double velocityVar =
        MotionStateEstimatorPredictor.getVg().times(
            pathStateBelief.getGroundDistribution().getCovariance()).times(
                MotionStateEstimatorPredictor.getVg().transpose()).normFrobenius();
    
    final double likelihood = Math.min(1d, Math.max(0d, 
            1d - FoldedNormalDist.cdf(0d, Math.sqrt(velocityVar), velocityAvg)));
    
    return likelihood;
  }
  

  @Override
  public DataDistribution<RunState> learn(Collection<? extends RunState> data) {
    return null;
  }
  

  @Override
  public void update(DataDistribution<RunState> target,
      Iterable<? extends PathStateDistribution> data) {
  }

}
