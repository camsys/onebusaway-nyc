package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.gtfs.model.calendar.ServiceInterval;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MtaTrackingGraph.TripInfo;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.DscLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.RunLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.RunTransitionLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.ScheduleLikelihood;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MtaPathStateBelief;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.InstanceState;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.blocks.ServiceIntervalBlock;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;

import gov.sandia.cognition.math.LogMath;
import gov.sandia.cognition.math.matrix.Vector;
import gov.sandia.cognition.statistics.distribution.MultivariateGaussian;
import gov.sandia.cognition.util.Pair;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.opentrackingtools.GpsObservation;
import org.opentrackingtools.graph.InferenceGraph;
import org.opentrackingtools.graph.edges.impl.SimpleInferredEdge;
import org.opentrackingtools.graph.paths.InferredPath;
import org.opentrackingtools.graph.paths.edges.PathEdge;
import org.opentrackingtools.graph.paths.edges.impl.EdgePredictiveResults;
import org.opentrackingtools.graph.paths.impl.InferredPathPrediction;
import org.opentrackingtools.graph.paths.impl.SimpleInferredPath;
import org.opentrackingtools.graph.paths.states.PathState;
import org.opentrackingtools.graph.paths.states.PathStateBelief;
import org.opentrackingtools.impl.VehicleState;
import org.opentrackingtools.impl.WrappedWeightedValue;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MtaInferredPath extends SimpleInferredPath {

  public static class MtaEdgePredictiveResults extends EdgePredictiveResults {
    private final double runPredLogLik;

    public MtaEdgePredictiveResults(EdgePredictiveResults edgePredictiveResults,
        double runPredLogLik) {
      super(edgePredictiveResults.getBeliefPrediction()
          , edgePredictiveResults.getLocationPrediction()
          , edgePredictiveResults.getEdgePredMarginalLogLik()
          , edgePredictiveResults.getEdgePredTransLogLik()
          , edgePredictiveResults.getMeasurementPredLogLik());
      this.runPredLogLik = runPredLogLik;
    }

    public double getRunPredLogLik() {
      return runPredLogLik;
    }

    @Override
    public double getTotalLogLik() {
      return super.getTotalLogLik() + runPredLogLik;
    }

  }

  private static final long _tripSearchTimeAfterLastStop = 5 * 60 * 60 * 1000;

  private static final long _tripSearchTimeBeforeFirstStop = 5 * 60 * 60 * 1000;
  
  public static InferredPath getNullPath() {
    return new MtaInferredPath(MtaPathEdge.getNullPathEdge());
  } 
  
  public static MtaInferredPath getInferredPath(
    List<? extends PathEdge> newEdges, Boolean isBackward) {
    if (newEdges.size() == 1) {
      final PathEdge edge = Iterables.getOnlyElement(newEdges);
      if (edge.isNullEdge())
        return (MtaInferredPath)getNullPath();
    }
    return new MtaInferredPath(ImmutableList.copyOf(newEdges),
        isBackward);
  }

  public static MtaInferredPath getInferredPath(
    PathEdge pathEdge) {
    if (pathEdge.isNullEdge())
      return (MtaInferredPath)getNullPath();
    else
      return new MtaInferredPath(ImmutableList.of(pathEdge), pathEdge.isBackward());
  }
      

  public MtaInferredPath(ImmutableList<PathEdge> edges, Boolean isBackward) {
    super(edges, isBackward);
  }

  public MtaInferredPath(PathEdge edge) {
    super(edge);
  }

  /**
   * This method returns edges over the possible block instances.
   * The log lik reflects these additions.
   */
  @Override
  public InferredPathPrediction getPriorPredictionResults(
      InferenceGraph graph,
      GpsObservation obs,
      VehicleState state,
      Map<PathEdge, EdgePredictiveResults> edgeToPreBeliefAndLogLik) {
    
    InferredPathPrediction generalPathPred = super.getPriorPredictionResults(graph, obs, state, edgeToPreBeliefAndLogLik);
    
    if (generalPathPred == null)
      return null;
    
    org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState prevOldTypeVehicleState;
    if (state != null) {
      prevOldTypeVehicleState = ((MtaVehicleState)state).getOldTypeVehicleState(); 
    } else {
      prevOldTypeVehicleState = null;
    }
    
    final MtaTrackingGraph mtaGraph = (MtaTrackingGraph) graph;
    
    Map<PathEdge, EdgePredictiveResults> updatedEdgeToBeliefMap = Maps.newHashMap();
    List<WrappedWeightedValue<PathEdge>> reweightedPathEdges = Lists.newArrayList();
    
    final long time = obs.getTimestamp().getTime();       
    final Date timeFrom = new Date(time - _tripSearchTimeAfterLastStop);
    final Date timeTo = new Date(time + _tripSearchTimeBeforeFirstStop);
    
    double pathLogLik = Double.NEGATIVE_INFINITY;
    for (PathEdge pathEdge: generalPathPred.getPath().getPathEdges()) {
      final TripInfo tripInfo = mtaGraph.getTripInfo(pathEdge.getInferredEdge());
      final EdgePredictiveResults predEdgeResults = 
          Preconditions.checkNotNull(generalPathPred.getEdgeToPredictiveBelief().get(pathEdge));
      final PathStateBelief pathStateBelief = predEdgeResults.getLocationPrediction();
      
      if (tripInfo != null) {
        /*
         * In this case our previous state was snapped to particular road segment for 
         * run/block set, so find all the active ones in our time window, produce states 
         * for them, and weigh.
         */
        for (BlockTripIndex index: tripInfo.getIndices()) {
          for (Pair<BlockTripEntry, Date> blockTripPair : mtaGraph.getActiveBlockTripEntries(index, timeFrom, timeTo)) {
            
            final BlockTripEntry blockTripEntry = blockTripPair.getFirst();
            
            // TODO FIXME debug. remove.
//            if (blockTripEntry.getTrip().getShapeId().toString().equals("MTA_BXM20075")) {
//              System.out.println("tripId = " + blockTripEntry.toString());
//            }
            
        	  final double distanceAlongBlock = blockTripEntry.getDistanceAlongBlock() 
        			  + pathStateBelief.getGlobalState().getElement(0);
            
            InstanceState instState = new InstanceState(blockTripPair.getSecond().getTime());
            BlockInstance instance = new BlockInstance(blockTripEntry.getBlockConfiguration(), 
                instState);
            
            EdgePredictiveResults predEdgeResultsCopy = predEdgeResults.clone();
            final MtaPathStateBelief mtaPathStateBelief = 
                (MtaPathStateBelief)predEdgeResultsCopy.getLocationPrediction();
            mtaPathStateBelief.setBlockState(mtaGraph.getBlockState(instance, distanceAlongBlock));
            
            final PathEdge mtaPathEdge = new MtaPathEdge(pathEdge.getInferredEdge(), 
                pathEdge.getDistToStartOfEdge(), pathEdge.isBackward(), blockTripEntry, 
                blockTripPair.getSecond().getTime());
                
            org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState currOldTypeVehicleState
              = mtaGraph.createOldTypeVehicleState((Observation)obs, mtaPathStateBelief, (MtaVehicleState)state);
            
            final double runPredLogLik = computeRunPredictiveLogLikelihood(
                mtaGraph,
                (Observation)obs, 
                currOldTypeVehicleState,
                prevOldTypeVehicleState);
            
            final MtaEdgePredictiveResults mtaEdgeResults = new MtaEdgePredictiveResults(
                predEdgeResults, runPredLogLik);
            
            if (runPredLogLik > Double.NEGATIVE_INFINITY) {
              updatedEdgeToBeliefMap.put(mtaPathEdge, mtaEdgeResults);
              
              reweightedPathEdges
                  .add(new WrappedWeightedValue<PathEdge>(mtaPathEdge,
                      mtaEdgeResults.getTotalLogLik()));
              pathLogLik =
                  LogMath.add(pathLogLik,
                      mtaEdgeResults.getTotalLogLik());
            }
          }
        }
      } else {
        /*
         * Our state wasn't on a road corresponding to a run/block set, or it wasn't
         * on a road at all.
         * We add null to ensure a run/block-less possibility.
         */
        Collection<BlockStateObservation> blockStates = mtaGraph.getBlockStateObs((Observation)obs);
        blockStates.add(null);
        for (BlockStateObservation blockStateObs : blockStates) {
          final long serviceDate;
          final BlockTripEntry blockTripEntry;
          EdgePredictiveResults predEdgeResultsCopy = predEdgeResults.clone();
          final MtaPathStateBelief mtaPathStateBelief = 
              (MtaPathStateBelief)predEdgeResultsCopy.getLocationPrediction();
          if (blockStateObs != null) {
            mtaPathStateBelief.setBlockState(blockStateObs.getBlockState());
            blockTripEntry = blockStateObs.getBlockState().getBlockLocation().getActiveTrip();
            serviceDate = blockStateObs.getBlockState().getBlockInstance().getServiceDate();
          } else {
            blockTripEntry = null;
            serviceDate = 0;
          }
          
          final PathEdge mtaPathEdge = new MtaPathEdge(pathEdge.getInferredEdge(), 
              pathEdge.getDistToStartOfEdge(), pathEdge.isBackward(), 
              blockTripEntry, serviceDate);
          
          org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState currOldTypeVehicleState
            = mtaGraph.createOldTypeVehicleState((Observation)obs, mtaPathStateBelief, (MtaVehicleState)state);
          
          Preconditions.checkState(!pathEdge.isNullEdge() 
              || (currOldTypeVehicleState.getJourneyState() != JourneyState.inProgress()));
          
          final double runPredLogLik = computeRunPredictiveLogLikelihood(
              mtaGraph,
              (Observation)obs, 
              currOldTypeVehicleState,
              prevOldTypeVehicleState);
          
          if (runPredLogLik > Double.NEGATIVE_INFINITY) {
            final MtaEdgePredictiveResults mtaEdgeResults = new MtaEdgePredictiveResults(
                predEdgeResultsCopy, runPredLogLik);
            updatedEdgeToBeliefMap.put(mtaPathEdge, mtaEdgeResults);
            
            reweightedPathEdges
                .add(new WrappedWeightedValue<PathEdge>(mtaPathEdge,
                    mtaEdgeResults.getTotalLogLik()));
            pathLogLik =
                LogMath.add(pathLogLik,
                    mtaEdgeResults.getTotalLogLik());
            
          }
        }
      }
    }
    
    /*
     * Total of zero likelihood
     */
    if (Double.isInfinite(pathLogLik))
      return null;

    assert !Double.isNaN(pathLogLik);
    
    final InferredPathPrediction updatedPathPrediction = new InferredPathPrediction(this, 
        updatedEdgeToBeliefMap, 
        generalPathPred.getFilter(), 
        reweightedPathEdges, 
        pathLogLik);
    
    return updatedPathPrediction;
  }
  
  public static double computeRunPredictiveLogLikelihood(
      MtaTrackingGraph graph,
      Observation obs, 
      org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState currentOldTypeVehicleState,
      org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState prevOldTypeVehicleState) {
    double totalLogLik = 0;
    final Context context = new Context(prevOldTypeVehicleState, currentOldTypeVehicleState, obs);
    try {
      totalLogLik += graph.getDscLikelihood().likelihood(context).getLogProbability();
      if (totalLogLik <= Double.NEGATIVE_INFINITY)
        return totalLogLik;
      totalLogLik += graph.getSchedLikelihood().likelihood(context).getLogProbability();
      if (totalLogLik <= Double.NEGATIVE_INFINITY)
        return totalLogLik;
      totalLogLik += graph.getRunLikelihood().likelihood(context).getLogProbability();
      if (totalLogLik <= Double.NEGATIVE_INFINITY)
        return totalLogLik;
      totalLogLik += graph.getRunTransitionLikelihood().likelihood(context).getLogProbability();
      if (totalLogLik <= Double.NEGATIVE_INFINITY)
        return totalLogLik;
      totalLogLik += graph.getNullStateLikelihood().likelihood(context).getLogProbability();
    } catch (BadProbabilityParticleFilterException e) {
      e.printStackTrace();
      totalLogLik = Double.NEGATIVE_INFINITY;
    }
    
    return totalLogLik;
  }

  @Override
  public PathStateBelief getStateBeliefOnPath(PathStateBelief stateBelief) {
    PathStateBelief belief = super.getStateBeliefOnPath(stateBelief);
    return MtaPathStateBelief.getPathStateBelief(belief.getPath(), belief.getGlobalStateBelief());
  }

  @Override
  public PathState getStateOnPath(Vector state) {
    return super.getStateOnPath(state);
  }

  @Override
  public PathState getStateOnPath(PathState currentState) {
    return super.getStateOnPath(currentState);
  }

  @Override
  public void updateEdges(GpsObservation obs, MultivariateGaussian stateBelief,
      InferenceGraph graph) {
    super.updateEdges(obs, stateBelief, graph);
  }

  @Override
  public PathStateBelief getStateBeliefOnPath(MultivariateGaussian belief) {
    return MtaPathStateBelief.getPathStateBelief(this, belief);
  }

  @Override
  public boolean isNullPath() {
    return (this.edges.size() == 1 
        && Iterables.getOnlyElement(this.edges).isNullEdge());
  }

  public static long getTripSearchTimeAfterLastStop() {
    return _tripSearchTimeAfterLastStop;
  }

  public static long getTripSearchTimeBeforeFirstStop() {
    return _tripSearchTimeBeforeFirstStop;
  }

}
