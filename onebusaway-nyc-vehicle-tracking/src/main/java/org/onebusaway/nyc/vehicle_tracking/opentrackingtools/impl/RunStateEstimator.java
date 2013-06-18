package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.JourneyStateTransitionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.MovedLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.NycTrackingGraph.BlockTripEntryAndDate;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.NycTrackingGraph.TripInfo;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;

import gov.sandia.cognition.learning.algorithm.IncrementalLearner;
import gov.sandia.cognition.statistics.DataDistribution;
import gov.sandia.cognition.statistics.bayesian.BayesianEstimatorPredictor;
import gov.sandia.cognition.util.AbstractCloneableSerializable;

import com.google.common.base.Preconditions;

import org.opentrackingtools.distributions.CountedDataDistribution;
import org.opentrackingtools.distributions.DeterministicDataDistribution;
import org.opentrackingtools.distributions.PathStateDistribution;
import org.opentrackingtools.estimators.MotionStateEstimatorPredictor;
import org.opentrackingtools.graph.InferenceGraphEdge;
import org.opentrackingtools.paths.PathEdge;
import org.opentrackingtools.util.model.MutableDoubleCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.internal.annotations.Sets;

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

/**
 * An estimator/predictor that allows for predictive sampling of a run/block
 * state. The current implementation simply returns a discrete distribution over
 * the possible runs, with densities determined by their likelihood. Basically,
 * we have a deterministic prior.
 * 
 * @author bwillard
 * 
 */
public class RunStateEstimator extends AbstractCloneableSerializable implements
    IncrementalLearner<PathStateDistribution, DataDistribution<RunState>>,
    BayesianEstimatorPredictor<RunState, RunState, DataDistribution<RunState>> {

  private static final long serialVersionUID = -1461026886038720233L;
  protected static Logger _log = LoggerFactory
      .getLogger(RunStateEstimator.class);
  
  private final NycTrackingGraph nycGraph;
  private final NycVehicleStateDistribution nycVehicleStateDist;
  private final Observation obs;
  private static final long _tripSearchTimeAfterLastStop = 5 * 60 * 60 * 1000;
  private static final long _tripSearchTimeBeforeFirstStop = 5 * 60 * 60 * 1000;
  private boolean debug = false;
  public void setDebug(boolean debug) {
    this.debug = debug;
    if (this.debug)
      _log.warn("Debugging is ON!");
  }
  
  public static long getTripSearchTimeAfterLastStop() {
    return _tripSearchTimeAfterLastStop;
  }

  public static long getTripSearchTimeBeforeFirstStop() {
    return _tripSearchTimeBeforeFirstStop;
  }

  public RunStateEstimator(NycTrackingGraph graph, Observation obs,
      NycVehicleStateDistribution nycVehicleStateDist) {
    this.obs = obs;
    this.nycGraph = graph;
    if (graph != null) {
      this.debug = graph.isDebug();
    }
    this.nycVehicleStateDist = nycVehicleStateDist;
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

    final PathEdge pathEdge = nycVehicleStateDist.getPathStateParam().getParameterPrior().getPathState().getEdge();
    final TripInfo tripInfo = nycGraph.getTripInfo(pathEdge.getInferenceGraphSegment());
    final double likelihoodHasNotMoved = likelihoodOfNotMovedState(nycVehicleStateDist.getPathStateParam().getParameterPrior());

    Comparator<RunState> comparator = new Comparator<RunState>() {
      @Override
      public int compare(RunState o1, RunState o2) {
        return Double.compare(o1.getLikelihoodInfo().getTotalLogLik(), 
            o2.getLikelihoodInfo().getTotalLogLik());
      }
    };
    
    Map<RunState, MutableDoubleCount> resultDist = null;
    if (debug) {
      resultDist = new TreeMap<RunState, MutableDoubleCount>(comparator);
    } else {
      resultDist = new HashMap<RunState, MutableDoubleCount>();
    }

    final RunState currentRunState = nycVehicleStateDist.getRunStateParam() != null
        ? nycVehicleStateDist.getRunStateParam().getValue() : null;
    final VehicleState currentOldTypeVehicleState = currentRunState != null
        ? currentRunState.getVehicleState() : null;
    /*
     * Track the on-road dscs so that we can avoid them as deadhead-durings
     * later down the line.  (If we don't, then we'll constantly be in a
     * difficult situation with in-progress states.)
     */
    final Set<String> onRoadDscs = Sets.newHashSet();

    if (tripInfo != null) {
      
      /*
       * In this case our previous state was snapped to particular road segment
       * for run/block set, so find all the active ones in our time window,
       * produce states for them, and weigh.
       */
      final Collection<BlockTripEntryAndDate> activeEntries = tripInfo.getActiveTrips(
          timeFrom.getTime(), timeTo.getTime());
      for (final BlockTripEntryAndDate blockTripEntryAndDate : activeEntries) {

        final BlockTripEntry blockTripEntry = blockTripEntryAndDate.getBlockTripEntry();
        final long serviceDate = blockTripEntryAndDate.getServiceDate().getTime();
        Preconditions.checkState(tripInfo.getShapeIds().contains(blockTripEntry.getTrip().getShapeId()));

        final BlockStateObservation blockStateObs = this.nycGraph.getBlockStateObs(
            obs,
            nycVehicleStateDist.getPathStateParam().getParameterPrior().getPathState(),
            blockTripEntry, serviceDate);
        
        final AgencyAndId blockStateShapeId = blockStateObs.getBlockState().getBlockLocation()
            .getActiveTrip().getTrip().getShapeId();
        Preconditions.checkState(tripInfo.getShapeIds().contains(blockStateShapeId));

        final RunState runStateMoved = new RunState(nycGraph, obs,
            nycVehicleStateDist, blockStateObs, false,
            currentOldTypeVehicleState, false);

        final RunState.RunStateEdgePredictiveResults mtaEdgeResultsMoved = runStateMoved.computeAnnotatedLogLikelihood();
        runStateMoved.setLikelihoodInfo(mtaEdgeResultsMoved);
        mtaEdgeResultsMoved.setMovedLogLikelihood(Math.log(1d - likelihoodHasNotMoved));
        if (mtaEdgeResultsMoved.getTotalLogLik() > Double.NEGATIVE_INFINITY) {
          onRoadDscs.add(runStateMoved.blockStateObs.getBlockState().getDestinationSignCode());
          resultDist.put(runStateMoved, new MutableDoubleCount(
              mtaEdgeResultsMoved.getTotalLogLik(), 1));
        }

        final RunState runStateNotMoved = new RunState(nycGraph, obs,
            nycVehicleStateDist, blockStateObs, true,
            currentOldTypeVehicleState, false);

        final RunState.RunStateEdgePredictiveResults mtaEdgeResultsNotMoved = runStateNotMoved.computeAnnotatedLogLikelihood();
        runStateNotMoved.setLikelihoodInfo(mtaEdgeResultsNotMoved);
        mtaEdgeResultsNotMoved.setMovedLogLikelihood(Math.log(likelihoodHasNotMoved));
        if (mtaEdgeResultsNotMoved.getTotalLogLik() > Double.NEGATIVE_INFINITY) {
          onRoadDscs.add(runStateMoved.blockStateObs.getBlockState().getDestinationSignCode());
          resultDist.put(runStateNotMoved, new MutableDoubleCount(
              mtaEdgeResultsNotMoved.getTotalLogLik(), 1));
        }
      }
    }

    /*
     * Our state wasn't on a road corresponding to a run/block set, or it wasn't
     * on a road at all. We add null to ensure a run/block-less possibility.
     */
    final Collection<BlockStateObservation> blockStates = nycGraph.getBlockStatesFromObservation(obs);
    blockStates.add(null);

    /*
     * Now add the state that simply moves forward along the block distance
     * according to the projected distance moved.
     */
    if (currentRunState != null) {
      if (currentRunState.getBlockStateObs() != null) {
        final double distProjected = MotionStateEstimatorPredictor.Og.times(
            nycVehicleStateDist.getPathStateParam().getParameterPrior().getGroundDistribution().getMean().minus(
                nycVehicleStateDist.getParentState().getPathStateParam().getParameterPrior().getGroundDistribution().getMean())).norm2();
        blockStates.add(nycGraph.getBlocksFromObservationService().getBlockStateObservationFromDist(
            this.obs,
            currentRunState.getBlockStateObs().getBlockState().getBlockInstance(),
            currentRunState.getBlockStateObs().getBlockState().getBlockLocation().getDistanceAlongBlock()
                + distProjected));
      }
      
    }
    

    for (final BlockStateObservation blockStateObs : blockStates) {
      
      /*
       * We want to avoid certain deadhead-durings when we're already considering
       * the related in-progresses, and detours when we're actually on the trip.
       */
      final boolean isInService = blockStateObs != null
          && JourneyStateTransitionModel.isLocationOnATrip(blockStateObs.getBlockState())
          && !obs.hasOutOfServiceDsc();
      
      final boolean isDeadheadDuring = blockStateObs != null
          && !JourneyStateTransitionModel.isLocationOnATrip(blockStateObs.getBlockState())
          && !obs.hasOutOfServiceDsc()
          && blockStateObs.getBlockState().getBlockLocation().getDistanceAlongBlock() > 0d
          && blockStateObs.getBlockState().getBlockLocation().getDistanceAlongBlock() < 
             blockStateObs.getBlockState().getBlockInstance().getBlock().getTotalBlockDistance();
      
      if (blockStateObs != null) {
        /*
         * The first condition avoids creating detours for geometries that
         * we know we're on.
         * The last condition is the deadhead check mentioned above.
         */
        if((isInService && tripInfo != null
          && tripInfo.getShapeIds().contains(
              blockStateObs.getBlockState().getRunTripEntry().getTripEntry().getShapeId()))
          || (isDeadheadDuring 
              && onRoadDscs.contains(blockStateObs.getBlockState().getDestinationSignCode())) ) {
          continue;
        }
      }

      final boolean movedIsDetoured = blockStateObs != null
          ? JourneyStateTransitionModel.isDetour(blockStateObs, null, true,
              currentOldTypeVehicleState) : false;

      /*
       * Only consider in-progress states when we've determined they're detours.
       */
      if (!isInService || movedIsDetoured) {

        final RunState runStateMoved = new RunState(nycGraph, obs,
            nycVehicleStateDist, blockStateObs, false,
            currentOldTypeVehicleState, movedIsDetoured);

        Preconditions.checkState(runStateMoved.getJourneyState().getPhase() != EVehiclePhase.IN_PROGRESS);

        final RunState.RunStateEdgePredictiveResults mtaEdgeResultsMoved = runStateMoved.computeAnnotatedLogLikelihood();
        runStateMoved.setLikelihoodInfo(mtaEdgeResultsMoved);
        mtaEdgeResultsMoved.setMovedLogLikelihood(Math.log(1d - likelihoodHasNotMoved));
        
        if (mtaEdgeResultsMoved.getTotalLogLik() > Double.NEGATIVE_INFINITY) {
          resultDist.put(runStateMoved, new MutableDoubleCount(
              mtaEdgeResultsMoved.getTotalLogLik(), 1));
        }
      }

      final boolean notMovedIsDetoured = blockStateObs != null
          ? JourneyStateTransitionModel.isDetour(blockStateObs, null, false,
              currentOldTypeVehicleState) : false;

      if (!isInService || notMovedIsDetoured) {
        final RunState runStateNotMoved = new RunState(nycGraph, obs,
            nycVehicleStateDist, blockStateObs, true,
            currentOldTypeVehicleState, notMovedIsDetoured);

        final RunState.RunStateEdgePredictiveResults mtaEdgeResultsNotMoved = runStateNotMoved.computeAnnotatedLogLikelihood();
        runStateNotMoved.setLikelihoodInfo(mtaEdgeResultsNotMoved);
        mtaEdgeResultsNotMoved.setMovedLogLikelihood(Math.log(likelihoodHasNotMoved));
        if (mtaEdgeResultsNotMoved.getTotalLogLik() > Double.NEGATIVE_INFINITY) {
          resultDist.put(runStateNotMoved, new MutableDoubleCount(
              mtaEdgeResultsNotMoved.getTotalLogLik(), 1));
        }
      }
    }

    final CountedDataDistribution<RunState> result = new CountedDataDistribution<RunState>(
        true);
    for (final Entry<RunState, MutableDoubleCount> entry : resultDist.entrySet()) {
      
      /*
       * DEBUG check that the shape/edge geom has a valid mapping
       */
      final BlockStateObservation blockStateObs = entry.getKey().getBlockStateObs();
      if (blockStateObs != null && blockStateObs.isSnapped()) {
        final AgencyAndId shapeId = blockStateObs.getBlockState()
            .getBlockLocation().getActiveTrip().getTrip().getShapeId();
        Preconditions.checkState(this.nycGraph.getLengthsAlongShapeMap().contains(
            shapeId, pathEdge.getInferenceGraphSegment().getGeometry()));
      }

      result.increment(entry.getKey(), entry.getValue().doubleValue(),
          entry.getValue().count);
    }

    Preconditions.checkState(!result.isEmpty());
    return result;
  }

  @Override
  public void update(DataDistribution<RunState> priorPredRunStateDist,
      PathStateDistribution posteriorPathStateDist) {

    Preconditions.checkArgument(priorPredRunStateDist instanceof DeterministicDataDistribution<?>);

    final DeterministicDataDistribution<RunState> priorRunDist = (DeterministicDataDistribution<RunState>) priorPredRunStateDist;

    //TODO can this be removed?
    //final PathStateDistribution priorPathStateDist = this.nycVehicleStateDist.getPathStateParam().getParameterPrior();
    final RunState priorPredRunState = priorPredRunStateDist.getMaxValueKey();
    /*
     * We must update update the run state, since the path belief gets updated.
     */
    final RunState posteriorRunState = priorPredRunState.clone();
    /*
     * The update might've changed our estimated motion status. Also, since
     * we're using a MAP-like final estimate, go with the max likelihood.
     */
    final double likelihoodHasNotMoved = likelihoodOfNotMovedState(posteriorPathStateDist);
    if (likelihoodHasNotMoved > 0.5d) {
      posteriorRunState.setVehicleHasNotMoved(true);
    } else {
      posteriorRunState.setVehicleHasNotMoved(false);
    }

    if (posteriorRunState.getBlockStateObs() != null) {

      final boolean predictedInProgress = posteriorRunState.getJourneyState().getPhase() == EVehiclePhase.IN_PROGRESS;
      if (predictedInProgress
          && !posteriorRunState.getJourneyState().getIsDetour()) {
        final ScheduledBlockLocation priorSchedLoc = posteriorRunState.getBlockStateObs().getBlockState().getBlockLocation();

        final BlockStateObservation newBlockStateObs = nycGraph.getBlockStateObs(
            obs,
            posteriorPathStateDist.getPathState(),
            priorSchedLoc.getActiveTrip(),
            posteriorRunState.getBlockStateObs().getBlockState().getBlockInstance().getServiceDate());

        posteriorRunState.setBlockStateObs(newBlockStateObs);
      } else {
        // if (predictedInProgress) {
        // /*
        // * TODO should we propagate states? how?
        // */
        //
        // } else {
        // /*
        // * TODO should we propagate states? how?
        // */
        // }
      }
    }
    priorRunDist.setElement(posteriorRunState);
  }

  /**
   * Sample a state of moved/not-moved using a belief's velocity distribution. <br>
   * This is done by evaluating the complement of a CDF from a folded normal,
   * with mean 0 and a variance from the Frobenius norm of the belief's velocity
   * covariance, at the belief's velocity mean
   * 
   * @param pathStateBelief
   * @return
   */
  public double likelihoodOfNotMovedState(PathStateDistribution pathStateBelief) {
    return MovedLikelihood.computeVehicleHasNotMovedProbability(obs);
//    final double velocityAvg = MotionStateEstimatorPredictor.getVg().times(
//        pathStateBelief.getGroundDistribution().getMean()).norm2();
//    final double velocityVar = MotionStateEstimatorPredictor.getVg().times(
//        pathStateBelief.getGroundDistribution().getCovariance()).times(
//        MotionStateEstimatorPredictor.getVg().transpose()).normFrobenius();
//
//    final double likelihood = Math.min(
//        1d,
//        Math.max(0d,
//            1d - FoldedNormalDist.cdf(0d, Math.sqrt(velocityVar), velocityAvg)));
//
//    return likelihood;
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
