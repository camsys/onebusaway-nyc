package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.gtfs.model.calendar.ServiceInterval;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MtaTrackingGraph.BlockTripEntryAndDate;
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
import org.onebusaway.realtime.api.EVehiclePhase;
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
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.builder.ToStringBuilder;
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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MtaInferredPath extends SimpleInferredPath {

  public static class MtaEdgePredictiveResults extends EdgePredictiveResults {

    protected double nullStateLogLikelihood;
    protected double runTransitionLogLikelihood;
    protected double runLogLikelihood;
    protected double schedLogLikelihood;
    protected double dscLogLikelihood;
    private Double total;

    public MtaEdgePredictiveResults(EdgePredictiveResults edgePredictiveResults) {
      super(edgePredictiveResults.getBeliefPrediction()
          , edgePredictiveResults.getLocationPrediction()
          , edgePredictiveResults.getEdgePredMarginalLogLik()
          , edgePredictiveResults.getEdgePredTransLogLik()
          , edgePredictiveResults.getMeasurementPredLogLik());
    }

    @Override
    public double getTotalLogLik() {
      if (total == null) {
        total = super.getTotalLogLik() + nullStateLogLikelihood
            + runTransitionLogLikelihood + runLogLikelihood + schedLogLikelihood
            + dscLogLikelihood;
      }
      
      return total;
    }

    public void setDscLogLikelihood(double logLikelihood) {
      total = null;
      this.dscLogLikelihood = logLikelihood;
    }

    public void setSchedLogLikelihood(double logLikelihood) {
      total = null;
      this.schedLogLikelihood = logLikelihood;
    }

    public void setRunLogLikelihood(double logLikelihood) {
      total = null;
      this.runLogLikelihood = logLikelihood;
    }

    public void setRunTransitionLogLikelihood(double logLikelihood) {
      total = null;
      this.runTransitionLogLikelihood = logLikelihood;
    }

    public void setNullStateLogLikelihood(double logLikelihood) {
      total = null;
      this.nullStateLogLikelihood = logLikelihood;
    }

    @Override
    public String toString() {
      ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("locationPrediction", locationPrediction);
      builder.append("total", total);
      builder.append("edgePredMarginalLogLik", edgePredMarginalLogLik);
      builder.append("edgePredTransLogLik", edgePredTransLogLik);
      builder.append("measurementPredLogLik", measurementPredLogLik);
      builder.append("nullStateLogLikelihood", nullStateLogLikelihood);
      builder.append("runTransitionLogLikelihood", runTransitionLogLikelihood);
      builder.append("runLogLikelihood", runLogLikelihood);
      builder.append("schedLogLikelihood", schedLogLikelihood);
      builder.append("dscLogLikelihood", dscLogLikelihood);
      return builder.toString();
    }

    @Override
    public EdgePredictiveResults clone() {
      MtaEdgePredictiveResults clone = (MtaEdgePredictiveResults) super.clone();
      clone.dscLogLikelihood = dscLogLikelihood;
      clone.runLogLikelihood = runLogLikelihood;
      clone.runTransitionLogLikelihood = runTransitionLogLikelihood;
      clone.schedLogLikelihood = schedLogLikelihood;
      clone.nullStateLogLikelihood = nullStateLogLikelihood;
      return clone;
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
      
  public MtaInferredPath(SimpleInferredPath path) {
    this.edgeIds = path.getEdgeIds();
    this.edges = path.getPathEdges();
    this.geometry = path.getGeometry();
    this.isBackward = path.isBackward();
    this.totalPathDistance = path.getTotalPathDistance();
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
        Collection<BlockTripEntryAndDate> activeEntries = tripInfo.getTimeIndex().query(timeFrom.getTime(), timeTo.getTime());
        for (BlockTripEntryAndDate blockTripEntryAndDate: activeEntries) {
          
          final BlockTripEntry blockTripEntry = blockTripEntryAndDate.getBlockTripEntry();
          long serviceDate = blockTripEntryAndDate.getServiceDate().getTime();
      	  final double distanceAlongBlock = blockTripEntry.getDistanceAlongBlock() 
      			  + pathStateBelief.getGlobalState().getElement(0);
          
          InstanceState instState = new InstanceState(serviceDate);
          BlockInstance instance = new BlockInstance(blockTripEntry.getBlockConfiguration(), 
              instState);
          
          MtaInferredPath mtaInferredPath = MtaInferredPath.deepCopyPath(
              (MtaInferredPath) predEdgeResults.getLocationPrediction().getPath());
          final MtaPathStateBelief mtaPathStateBelief = (MtaPathStateBelief) mtaInferredPath.getStateBeliefOnPath(
              predEdgeResults.getLocationPrediction().getGlobalStateBelief().clone());
          EdgePredictiveResults predEdgeResultsCopy = new EdgePredictiveResults(
              predEdgeResults.getBeliefPrediction().clone(), mtaPathStateBelief, 
              predEdgeResults.getEdgePredMarginalLogLik(),
              predEdgeResults.getEdgePredTransLogLik(), 
              predEdgeResults.getMeasurementPredLogLik());
          mtaPathStateBelief.setBlockState(mtaGraph.getBlockState(instance, distanceAlongBlock));
          
          final MtaPathEdge mtaPathEdge = (MtaPathEdge) mtaPathStateBelief.getEdge();
          mtaPathEdge.setBlockTripEntry(blockTripEntry);
          mtaPathEdge.setServiceDate(serviceDate);
              
          org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState currOldTypeVehicleState
            = mtaGraph.createOldTypeVehicleState((Observation)obs, mtaPathStateBelief, (MtaVehicleState)state);
          
          final MtaEdgePredictiveResults mtaEdgeResults = 
              computeRunPredictiveLogLikelihood(
              predEdgeResultsCopy,
              mtaGraph,
              (Observation)obs, 
              currOldTypeVehicleState,
              prevOldTypeVehicleState);
          
          
          if (mtaEdgeResults.getTotalLogLik() > Double.NEGATIVE_INFINITY) {
            updatedEdgeToBeliefMap.put(mtaPathEdge, mtaEdgeResults);
            
            reweightedPathEdges
                .add(new WrappedWeightedValue<PathEdge>(mtaPathEdge,
                    mtaEdgeResults.getTotalLogLik()));
            pathLogLik =
                LogMath.add(pathLogLik,
                    mtaEdgeResults.getTotalLogLik());
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
          
          if (blockStateObs != null && JourneyStateTransitionModel.isLocationOnATrip(blockStateObs.getBlockState()))
            continue;
          
          MtaInferredPath mtaInferredPath = MtaInferredPath.deepCopyPath(
              (MtaInferredPath) predEdgeResults.getLocationPrediction().getPath());
          final MtaPathStateBelief mtaPathStateBelief = (MtaPathStateBelief) mtaInferredPath.getStateBeliefOnPath(
              predEdgeResults.getLocationPrediction().getGlobalStateBelief().clone());
          EdgePredictiveResults predEdgeResultsCopy = new EdgePredictiveResults(
              predEdgeResults.getBeliefPrediction().clone(), mtaPathStateBelief, 
              predEdgeResults.getEdgePredMarginalLogLik(),
              predEdgeResults.getEdgePredTransLogLik(), 
              predEdgeResults.getMeasurementPredLogLik());
          
          final Long serviceDate;
          final BlockTripEntry blockTripEntry;
          if (blockStateObs != null) {
            mtaPathStateBelief.setBlockState(blockStateObs.getBlockState());
            blockTripEntry = blockStateObs.getBlockState().getBlockLocation().getActiveTrip();
            serviceDate = blockStateObs.getBlockState().getBlockInstance().getServiceDate();
          } else {
            blockTripEntry = MtaPathEdge.getNullBlockTripEntry();
            serviceDate = null;
          }
          
          final MtaPathEdge mtaPathEdge = (MtaPathEdge) mtaPathStateBelief.getEdge();
          mtaPathEdge.setBlockTripEntry(blockTripEntry);
          mtaPathEdge.setServiceDate(serviceDate);
          
          org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState currOldTypeVehicleState
            = mtaGraph.createOldTypeVehicleState((Observation)obs, mtaPathStateBelief, (MtaVehicleState)state);
          
          Preconditions.checkState(currOldTypeVehicleState.getJourneyState().getPhase() != EVehiclePhase.IN_PROGRESS);
          
          Preconditions.checkState(!pathEdge.isNullEdge() 
              || (currOldTypeVehicleState.getJourneyState() != JourneyState.inProgress()));
          
            final MtaEdgePredictiveResults mtaEdgeResults = 
                computeRunPredictiveLogLikelihood(
                predEdgeResultsCopy,
                mtaGraph,
                (Observation)obs, 
                currOldTypeVehicleState,
                prevOldTypeVehicleState);
          
          if (mtaEdgeResults.getTotalLogLik() > Double.NEGATIVE_INFINITY) {
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
  
  public static MtaInferredPath deepCopyPath(MtaInferredPath path) {
    if (path.isNullPath())
      return (MtaInferredPath) getNullPath();
    Builder<PathEdge> newEdges = ImmutableList.builder();
    for (PathEdge pathEdge : path.getPathEdges()) {
      newEdges.add(((MtaPathEdge)pathEdge).clone());
    }
    return new MtaInferredPath(newEdges.build(), path.isBackward());
  }

  public static MtaEdgePredictiveResults computeRunPredictiveLogLikelihood(
      EdgePredictiveResults baseEdgeResults,
      MtaTrackingGraph graph,
      Observation obs, 
      org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState currentOldTypeVehicleState,
      org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState prevOldTypeVehicleState) {
    final Context context = new Context(prevOldTypeVehicleState, currentOldTypeVehicleState, obs);
    double runTransitionLogLikelihood = Double.NEGATIVE_INFINITY;
    double nullStateLogLikelihood = Double.NEGATIVE_INFINITY;
    double runLogLikelihood = Double.NEGATIVE_INFINITY;
    double schedLogLikelihood = Double.NEGATIVE_INFINITY;
    double dscLogLikelihood = Double.NEGATIVE_INFINITY;
    MtaEdgePredictiveResults result = new MtaEdgePredictiveResults(baseEdgeResults);
    try {
      dscLogLikelihood = graph.getDscLikelihood().likelihood(context).getLogProbability();
      result.setDscLogLikelihood(dscLogLikelihood);
      if (dscLogLikelihood <= Double.NEGATIVE_INFINITY)
        return result;
      schedLogLikelihood = graph.getSchedLikelihood().likelihood(context).getLogProbability();
      result.setSchedLogLikelihood(schedLogLikelihood);
      if (schedLogLikelihood <= Double.NEGATIVE_INFINITY)
        return result;
      runLogLikelihood = graph.getRunLikelihood().likelihood(context).getLogProbability();
      result.setRunLogLikelihood(runLogLikelihood);
      if (runLogLikelihood <= Double.NEGATIVE_INFINITY)
        return result;
      runTransitionLogLikelihood = graph.getRunTransitionLikelihood().likelihood(context).getLogProbability();
      result.setRunTransitionLogLikelihood(runTransitionLogLikelihood);
      if (runTransitionLogLikelihood <= Double.NEGATIVE_INFINITY)
        return result;
      nullStateLogLikelihood = graph.getNullStateLikelihood().likelihood(context).getLogProbability();
      result.setNullStateLogLikelihood(nullStateLogLikelihood);
    } catch (BadProbabilityParticleFilterException e) {
      e.printStackTrace();
    }
    
    return result;
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

  @Override
  public String toString() {
    ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("edges", edges);
    builder.append("totalPathDistance", totalPathDistance);
    return builder.toString();
  }

  @Override
  public InferredPath getPathTo(PathEdge edge) {
    return new MtaInferredPath(super.getPathTo(edge).getPathEdges(), isBackward);
  }

}
