package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.MtaInferredPath.MtaEdgePredictiveResults;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.MtaTrackingGraph.TripInfo;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MtaPathStateBelief;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.InstanceState;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockTripEntry;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import gov.sandia.cognition.math.LogMath;
import gov.sandia.cognition.statistics.DataDistribution;
import gov.sandia.cognition.util.Pair;

import org.opentrackingtools.GpsObservation;
import org.opentrackingtools.graph.InferenceGraph;
import org.opentrackingtools.graph.paths.edges.PathEdge;
import org.opentrackingtools.graph.paths.edges.impl.EdgePredictiveResults;
import org.opentrackingtools.graph.paths.impl.InferredPathPrediction;
import org.opentrackingtools.graph.paths.states.PathStateBelief;
import org.opentrackingtools.impl.VehicleState;
import org.opentrackingtools.impl.VehicleStateInitialParameters;
import org.opentrackingtools.impl.WrappedWeightedValue;
import org.opentrackingtools.statistics.filters.vehicles.particle_learning.impl.VehicleTrackingPLFilter;
import org.opentrackingtools.statistics.impl.StatisticsUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
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
    
    org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState prevOldTypeVehicleState = null;
    
    final MtaTrackingGraph mtaGraph = (MtaTrackingGraph) this.inferredGraph;
    final Observation obs = (Observation) this.initialObservation;
    
    List<WrappedWeightedValue<VehicleState>> reweightedPathEdges = Lists.newArrayList();
    final long time = obs.getTimestamp().getTime();       
    final Date timeFrom = new Date(time - MtaInferredPath.getTripSearchTimeAfterLastStop());
    final Date timeTo = new Date(time + MtaInferredPath.getTripSearchTimeBeforeFirstStop());
    
    for (VehicleState state : initialDist.getDomain()) {
      final PathEdge pathEdge = state.getBelief().getEdge();
      final TripInfo tripInfo = mtaGraph.getTripInfo(pathEdge.getInferredEdge());
      final PathStateBelief pathStateBelief = state.getBelief();
      
      if (tripInfo != null) {
        
        for (BlockTripIndex index: tripInfo.getIndices()) {
          for (Pair<BlockTripEntry, Date> blockTripPair : mtaGraph.getActiveBlockTripEntries(index, timeFrom, timeTo)) {
            final BlockTripEntry blockTripEntry = blockTripPair.getFirst();
            
            final double distanceAlongBlock = blockTripEntry.getDistanceAlongBlock() 
                + pathStateBelief.getGlobalState().getElement(0);
            
            InstanceState instState = new InstanceState(blockTripPair.getSecond().getTime());
            BlockInstance instance = new BlockInstance(blockTripEntry.getBlockConfiguration(), 
                instState);
            
            final MtaPathStateBelief mtaPathStateBelief = (MtaPathStateBelief)pathStateBelief.clone();
            mtaPathStateBelief.setBlockState(mtaGraph.getBlockState(instance, distanceAlongBlock));
            
            org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState currOldTypeVehicleState
              = mtaGraph.createOldTypeVehicleState(obs, mtaPathStateBelief, null);
            
            final double runPredLogLik = MtaInferredPath.computeRunPredictiveLogLikelihood(
                mtaGraph,
                obs, 
                currOldTypeVehicleState,
                prevOldTypeVehicleState);
            
            final MtaVehicleState newState = (MtaVehicleState) this.inferredGraph.createVehicleState(state.getObservation(),
                state.getMovementFilter().clone(), 
                mtaPathStateBelief.clone(), 
                state.getEdgeTransitionDist().clone(), null);
            
            newState.setOldTypeVehicleState(currOldTypeVehicleState);
            
            reweightedPathEdges
                .add(new WrappedWeightedValue<VehicleState>(newState, runPredLogLik));
          }
        }
      } else {
        Collection<BlockStateObservation> blockStates = mtaGraph.getBlockStateObs(obs);
        blockStates.add(null);
        for (BlockStateObservation blockStateObs : blockStates) {
          
          final MtaPathStateBelief mtaPathStateBelief = (MtaPathStateBelief)pathStateBelief.clone();
          
          if (blockStateObs != null)
            mtaPathStateBelief.setBlockState(blockStateObs.getBlockState());
          
          org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState currOldTypeVehicleState
            = mtaGraph.createOldTypeVehicleState((Observation)obs, mtaPathStateBelief, null);
          
          final MtaVehicleState newState = (MtaVehicleState) this.inferredGraph.createVehicleState(state.getObservation(),
              state.getMovementFilter().clone(), 
              mtaPathStateBelief.clone(), 
              state.getEdgeTransitionDist().clone(), null);
          
          newState.setOldTypeVehicleState(currOldTypeVehicleState);
          
          final double runPredLogLik = MtaInferredPath.computeRunPredictiveLogLikelihood(
              mtaGraph,
              (Observation)obs, 
              currOldTypeVehicleState,
              prevOldTypeVehicleState);
          
          reweightedPathEdges
              .add(new WrappedWeightedValue<VehicleState>(newState, runPredLogLik));
        }
      }
    }
    return StatisticsUtil.getLogNormalizedDistribution(reweightedPathEdges);
  }

  @Override
  public void update(DataDistribution<VehicleState> target, GpsObservation obs) {
    super.update(target, obs);
    
    for (VehicleState state : target.getDomain()) {
      MtaVehicleState mtaState = (MtaVehicleState) state;
      mtaState.setOldTypeVehicleState(((MtaTrackingGraph)this.inferredGraph).createOldTypeVehicleState(
          ((Observation)mtaState.getObservation()), 
          ((MtaPathStateBelief)mtaState.getBelief()),
          ((MtaVehicleState) mtaState.getParentState())));
    }
  }

}
