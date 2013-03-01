package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.gtfs.model.calendar.ServiceInterval;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.JourneyStateTransitionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
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
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.MtaTrackingGraph.BlockTripEntryAndDate;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.MtaTrackingGraph.TripInfo;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.RunState.RunStateEdgePredictiveResults;
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
import gov.sandia.cognition.statistics.DataDistribution;
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
import org.opentrackingtools.graph.paths.edges.impl.SimplePathEdge;
import org.opentrackingtools.graph.paths.impl.PathEdgeDistributionWrapper;
import org.opentrackingtools.graph.paths.impl.SimpleInferredPath;
import org.opentrackingtools.graph.paths.states.PathState;
import org.opentrackingtools.graph.paths.states.PathStateBelief;
import org.opentrackingtools.impl.VehicleState;
import org.opentrackingtools.impl.WrappedWeightedValue;
import org.opentrackingtools.statistics.distributions.impl.DefaultCountedDataDistribution;
import org.opentrackingtools.statistics.distributions.impl.DeterministicDataDistribution;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MtaInferredPath extends SimpleInferredPath {

  public static InferredPath getNullPath() {
    return new MtaInferredPath(SimplePathEdge.getNullPathEdge());
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
  public PathEdgeDistributionWrapper getPriorPredictionResults(
      InferenceGraph graph,
      GpsObservation obs,
      VehicleState state,
      Map<PathEdge, EdgePredictiveResults> edgeToPreBeliefAndLogLik) {
    
    PathEdgeDistributionWrapper generalPathPred = super.getPriorPredictionResults(graph, obs, state, edgeToPreBeliefAndLogLik);
    
    if (generalPathPred == null)
      return null;
    
    final MtaTrackingGraph mtaGraph = (MtaTrackingGraph) graph;
    
//    Map<PathEdge, EdgePredictiveResults> updatedEdgeToBeliefMap = Maps.newHashMap();
//    List<WrappedWeightedValue<PathEdge>> reweightedPathEdges = Lists.newArrayList();
    DeterministicDataDistribution<RunState> prevRunStateDist = 
        ((MtaPathStateBelief)state.getBelief()).getRunStateBelief();
    
//    double pathLogLik = Double.NEGATIVE_INFINITY;
    for (PathEdge pathEdge : generalPathPred.getPath().getPathEdges()) {
      final EdgePredictiveResults predEdgeResults = 
          Preconditions.checkNotNull(generalPathPred.getEdgeToPredictiveBelief().get(pathEdge));
      final MtaPathStateBelief locPredBelief = (MtaPathStateBelief) predEdgeResults.getLocationPrediction();
      final MtaPathStateBelief predBelief = (MtaPathStateBelief) predEdgeResults.getBeliefPrediction();
      
      RunStateEstimator runEstimator = new RunStateEstimator(mtaGraph, 
          (Observation)obs, locPredBelief, prevRunStateDist.getMaxValueKey().getVehicleState(), 
          mtaGraph.getRng());
      
      DataDistribution<RunState> runDistribution = 
          runEstimator.createPredictiveDistribution(prevRunStateDist);
      
      final RunState newRunState = runDistribution.sample(mtaGraph.getRng());
      
      final DeterministicDataDistribution<RunState> newRunDist = new DeterministicDataDistribution<RunState>(newRunState);
      locPredBelief.setRunStateBelief(newRunDist);
      predBelief.setRunStateBelief(newRunDist);
      
//      final RunStateEdgePredictiveResults newEdgeResults = newRunState.computeAnnotatedLogLikelihood();
//      newEdgeResults.setBeliefPrediction(predEdgeResults.getBeliefPrediction());
//      newEdgeResults.setLocationPrediction(predEdgeResults.getLocationPrediction());
//      newEdgeResults.setEdgePredMarginalLogLik(predEdgeResults.getEdgePredMarginalLogLik());
//      newEdgeResults.setEdgePredTransLogLik(predEdgeResults.getEdgePredTransLogLik());
//      newEdgeResults.setMeasurementPredLogLik(predEdgeResults.getMeasurementPredLogLik());
      
//      updatedEdgeToBeliefMap.put(pathEdge, newEdgeResults);
//      reweightedPathEdges.add(new WrappedWeightedValue<PathEdge>(pathEdge, newEdgeResults.getTotalLogLik()));
//      pathLogLik = LogMath.add(pathLogLik, newEdgeResults.getTotalLogLik());
    }
    
    /*
     * Total of zero likelihood
     */
//    if (Double.isInfinite(pathLogLik))
//      return null;
//
//    assert !Double.isNaN(pathLogLik);
    
//    final InferredPathPrediction updatedPathPrediction = new InferredPathPrediction(this, 
//        updatedEdgeToBeliefMap, 
//        generalPathPred.getFilter(), 
//        reweightedPathEdges, 
//        pathLogLik);
    
//    return updatedPathPrediction;
    // TODO remove.  debug
    if (!this.isNullPath()
        && ((MtaPathStateBelief)generalPathPred.getEdgeToPredictiveBelief().get(
            generalPathPred.getPathEdgeDisribution().getMaxValueKey()).getLocationPrediction())
            .getRunStateBelief().getMaxValueKey().getJourneyState().getPhase() != EVehiclePhase.IN_PROGRESS) {
      Preconditions.checkState(true);
    }
    return generalPathPred;
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
