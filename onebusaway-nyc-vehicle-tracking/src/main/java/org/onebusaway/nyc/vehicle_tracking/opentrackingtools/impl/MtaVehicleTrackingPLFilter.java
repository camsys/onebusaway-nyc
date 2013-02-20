package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.JourneyStateTransitionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MtaPathStateBelief;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.MtaInferredPath.MtaEdgePredictiveResults;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.MtaTrackingGraph.BlockTripEntryAndDate;
import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.MtaTrackingGraph.TripInfo;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.BlockTripIndex;
import org.onebusaway.transit_data_federation.services.blocks.InstanceState;
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
import org.opentrackingtools.graph.paths.impl.InferredPathPrediction;
import org.opentrackingtools.graph.paths.states.PathStateBelief;
import org.opentrackingtools.impl.VehicleState;
import org.opentrackingtools.impl.VehicleStateInitialParameters;
import org.opentrackingtools.impl.WrappedWeightedValue;
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
    
    org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState prevOldTypeVehicleState = null;
    
    final MtaTrackingGraph mtaGraph = (MtaTrackingGraph) this.inferredGraph;
    final Observation obs = (Observation) this.initialObservation;
    
    List<WrappedWeightedValue<VehicleState>> reweightedPathEdges = Lists.newArrayList();
    final long time = obs.getTimestamp().getTime();       
    final Date timeFrom = new Date(time - MtaInferredPath.getTripSearchTimeAfterLastStop());
    final Date timeTo = new Date(time + MtaInferredPath.getTripSearchTimeBeforeFirstStop());
    
    /*
     * In this setting we can integrate over the block/run dependent
     * space and make valid marginal comparisions with nulls states.  
     */
    double blockStateTotalLogLikelihood = Double.NEGATIVE_INFINITY;
    
    for (VehicleState state : initialDist.getDomain()) {
      final PathEdge pathEdge = state.getBelief().getEdge();
      final TripInfo tripInfo = mtaGraph.getTripInfo(pathEdge.getInferredEdge());
      final PathStateBelief pathStateBelief = state.getBelief();
      
      if (tripInfo != null) {
        
        Collection<BlockTripEntryAndDate> activeEntries = tripInfo.getTimeIndex().query(timeFrom.getTime(), timeTo.getTime());
        for (BlockTripEntryAndDate blockTripEntryAndDate: activeEntries) {
            
          final BlockTripEntry blockTripEntry = blockTripEntryAndDate.getBlockTripEntry();
          
          final double distanceAlongBlock = blockTripEntry.getDistanceAlongBlock() 
              + pathStateBelief.getGlobalState().getElement(0);
          
          InstanceState instState = new InstanceState(blockTripEntryAndDate.getServiceDate().getTime());
          BlockInstance instance = new BlockInstance(blockTripEntry.getBlockConfiguration(), 
              instState);
          
          MtaInferredPath mtaInferredPath = MtaInferredPath.deepCopyPath((MtaInferredPath) pathStateBelief.getPath());
          final MtaPathStateBelief mtaPathStateBelief = (MtaPathStateBelief) mtaInferredPath.getStateBeliefOnPath(
              pathStateBelief.getGlobalStateBelief().clone());
          mtaPathStateBelief.setBlockState(mtaGraph.getBlockState(instance, distanceAlongBlock));
          MtaPathEdge mtaPathEdge = ((MtaPathEdge)mtaPathStateBelief.getEdge());
          mtaPathEdge.setBlockTripEntry(blockTripEntry);
          mtaPathEdge.setServiceDate(blockTripEntryAndDate.getServiceDate().getTime());
          
          org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState currOldTypeVehicleState
            = mtaGraph.createOldTypeVehicleState(obs, mtaPathStateBelief, null);
          
          final EdgePredictiveResults edgeResults = new EdgePredictiveResults(null, null, 
              0d,
              0d,
              initialDist.getLogFraction(state)
              );
          final MtaEdgePredictiveResults mtaEdgeLikelihood = MtaInferredPath.computeRunPredictiveLogLikelihood(
              edgeResults,
              mtaGraph,
              obs, 
              currOldTypeVehicleState,
              prevOldTypeVehicleState);
          blockStateTotalLogLikelihood = LogMath.add(blockStateTotalLogLikelihood, 
              mtaEdgeLikelihood.schedLogLikelihood);
          
          final double runPredLogLik = mtaEdgeLikelihood.getTotalLogLik();
          
          final MtaVehicleState newState = (MtaVehicleState) this.inferredGraph.createVehicleState(state.getObservation(),
              state.getMovementFilter().clone(), 
              mtaPathStateBelief, 
              state.getEdgeTransitionDist().clone(), null);
          
          newState.setOldTypeVehicleState(currOldTypeVehicleState);
          
          reweightedPathEdges
              .add(new WrappedWeightedValue<VehicleState>(newState, runPredLogLik));
        }
      } else {
        Collection<BlockStateObservation> blockStates = mtaGraph.getBlockStateObs(obs);
        blockStates.add(null);
        for (BlockStateObservation blockStateObs : blockStates) {
          if (blockStateObs != null && JourneyStateTransitionModel.isLocationOnATrip(blockStateObs.getBlockState()))
            continue;
          
          /*
           * The entire path needs to be recreated to reflect this block/run
           */
          MtaInferredPath mtaInferredPath = MtaInferredPath.deepCopyPath((MtaInferredPath) pathStateBelief.getPath());
          final MtaPathStateBelief mtaPathStateBelief = (MtaPathStateBelief) mtaInferredPath.getStateBeliefOnPath(
              pathStateBelief.getGlobalStateBelief().clone());
          
          final BlockTripEntry blockTripEntry;
          final Long serviceDate;
          if (blockStateObs != null) {
            mtaPathStateBelief.setBlockState(blockStateObs.getBlockState());
            blockTripEntry = blockStateObs.getBlockState().getBlockLocation().getActiveTrip();
            serviceDate = blockStateObs.getBlockState().getBlockInstance().getServiceDate();
          } else {
            blockTripEntry = MtaPathEdge.getNullBlockTripEntry();
            serviceDate = null;
          }
          MtaPathEdge mtaPathEdge = ((MtaPathEdge)mtaPathStateBelief.getEdge());
          mtaPathEdge.setBlockTripEntry(blockTripEntry);
          mtaPathEdge.setServiceDate(serviceDate);
          
          org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState currOldTypeVehicleState
            = mtaGraph.createOldTypeVehicleState((Observation)obs, mtaPathStateBelief, null);
          
          Preconditions.checkState(currOldTypeVehicleState.getJourneyState().getPhase() 
              != EVehiclePhase.IN_PROGRESS);
          
          final MtaVehicleState newState = (MtaVehicleState) this.inferredGraph.createVehicleState(state.getObservation(),
              state.getMovementFilter().clone(), 
              mtaPathStateBelief, 
              state.getEdgeTransitionDist().clone(), null);
          
          newState.setOldTypeVehicleState(currOldTypeVehicleState);
            
          final EdgePredictiveResults edgeResults = new EdgePredictiveResults(null, null, 
              0d,
              0d,
              initialDist.getLogFraction(state));
          MtaEdgePredictiveResults mtaEdgeLikelihood = MtaInferredPath.computeRunPredictiveLogLikelihood(
              edgeResults,
              mtaGraph,
              obs, 
              currOldTypeVehicleState,
              prevOldTypeVehicleState);
          
          if (blockStateObs != null)
            blockStateTotalLogLikelihood = LogMath.add(blockStateTotalLogLikelihood, 
                mtaEdgeLikelihood.schedLogLikelihood);
          
          reweightedPathEdges 
              .add(new WrappedWeightedValue<VehicleState>(newState, 
                  mtaEdgeLikelihood.getTotalLogLik()));
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
