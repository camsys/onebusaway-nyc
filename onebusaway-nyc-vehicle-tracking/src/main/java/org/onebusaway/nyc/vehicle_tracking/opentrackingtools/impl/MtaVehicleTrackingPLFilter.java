package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.JourneyStateTransitionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MtaPathStateBelief;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.MtaTrackingGraph.BlockTripEntryAndDate;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.MtaTrackingGraph.TripInfo;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.RunState.RunStateEdgePredictiveResults;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.InstanceState;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import gov.sandia.cognition.math.LogMath;
import gov.sandia.cognition.statistics.DataDistribution;
import gov.sandia.cognition.util.Pair;

import org.opentrackingtools.GpsObservation;
import org.opentrackingtools.graph.InferenceGraph;
import org.opentrackingtools.graph.paths.edges.PathEdge;
import org.opentrackingtools.graph.paths.edges.impl.EdgePredictiveResults;
import org.opentrackingtools.graph.paths.impl.PathEdgeDistributionWrapper;
import org.opentrackingtools.graph.paths.states.PathStateBelief;
import org.opentrackingtools.impl.MutableDoubleCount;
import org.opentrackingtools.impl.VehicleState;
import org.opentrackingtools.impl.VehicleStateInitialParameters;
import org.opentrackingtools.impl.WrappedWeightedValue;
import org.opentrackingtools.statistics.distributions.impl.DefaultCountedDataDistribution;
import org.opentrackingtools.statistics.distributions.impl.DeterministicDataDistribution;
import org.opentrackingtools.statistics.filters.vehicles.particle_learning.impl.VehicleTrackingPLFilter;
import org.opentrackingtools.statistics.impl.StatisticsUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;

public class MtaVehicleTrackingPLFilter extends VehicleTrackingPLFilter {

  public MtaVehicleTrackingPLFilter(GpsObservation obs,
      InferenceGraph inferredGraph, VehicleStateInitialParameters parameters,
      Boolean isDebug, Random rng) throws SecurityException,
      IllegalArgumentException, ClassNotFoundException, NoSuchMethodException,
      InstantiationException, IllegalAccessException, InvocationTargetException {
    super(obs, inferredGraph, parameters, isDebug, rng);
  }

  @Override
  public DataDistribution<VehicleState> createInitialLearnedObject() {
    DataDistribution<VehicleState> initialDist = super.createInitialLearnedObject();
    
    final MtaTrackingGraph mtaGraph = (MtaTrackingGraph) this.inferredGraph;
    final Observation obs = (Observation) this.initialObservation;
    
    DefaultCountedDataDistribution<VehicleState> newDist = 
        new DefaultCountedDataDistribution<VehicleState>(initialDist.getDomainSize(), true);
    for (Entry<VehicleState, ? extends Number> entry  : initialDist.asMap().entrySet()) {
      final MtaPathStateBelief mtaPathStateBelief = (MtaPathStateBelief) entry.getKey().getBelief();
      
      RunStateEstimator runEstimator = new RunStateEstimator(mtaGraph, 
          obs, mtaPathStateBelief, null, this.getRandom());
      
      DataDistribution<RunState> runDistribution = 
          runEstimator.createPredictiveDistribution(null);
      
      final RunState newRunState = runDistribution.sample(mtaGraph.getRng());
      
      mtaPathStateBelief.setRunStateBelief(
          new DeterministicDataDistribution<RunState>(newRunState));
      
      MutableDoubleCount value = (MutableDoubleCount) entry.getValue();
      newDist.set(entry.getKey(), 
          value.value, value.count);
//          initialDist.getLogFraction(state) + 
//          newRunState.computeAnnotatedLogLikelihood().getTotalLogLik());
    }
    
    return newDist;
  }

  @Override
  protected VehicleState propagateStates(VehicleState state,
      GpsObservation obs, EdgePredictiveResults predictionResults,
      int pathSupportSize) {
    final VehicleState newState = super.propagateStates(state, obs, predictionResults, pathSupportSize);
    /*
     * FIXME more ridiculous hackery that demands an api redesign.
     */
    final DeterministicDataDistribution<RunState> runStateBelief = 
        ((MtaPathStateBelief) predictionResults.getLocationPrediction()).getRunStateBelief();
    
    final RunState priorPredRunState = runStateBelief.getMaxValueKey();
    
    /*
     * We must update also update the run state, since the path belief has been updated. 
     */
    final DeterministicDataDistribution<RunState> updatedRunStateBelief;
    if (priorPredRunState.getBlockStateObs() != null) {
      final ScheduledBlockLocation priorSchedLoc = priorPredRunState.getBlockStateObs().getBlockState().getBlockLocation();
      MtaTrackingGraph mtaGraph = (MtaTrackingGraph)this.inferredGraph;
      final BlockStateObservation newBlockStateObs = mtaGraph.getBlockStateObs((Observation)obs, 
          newState.getBelief(), 
          priorSchedLoc.getActiveTrip(), 
          priorPredRunState.getBlockStateObs().getBlockState().getBlockInstance().getServiceDate());
      updatedRunStateBelief = 
          new DeterministicDataDistribution<RunState>(new RunState(mtaGraph,
              (Observation)obs, newBlockStateObs, priorPredRunState.getVehicleHasNotMoved(), 
              priorPredRunState.getParentVehicleState()));
    } else {
      updatedRunStateBelief = runStateBelief;
    }
    
    Preconditions.checkNotNull(updatedRunStateBelief);
    ((MtaPathStateBelief) newState.getBelief()).setRunStateBelief(updatedRunStateBelief);
    return newState;
  }
  
  
  
}
