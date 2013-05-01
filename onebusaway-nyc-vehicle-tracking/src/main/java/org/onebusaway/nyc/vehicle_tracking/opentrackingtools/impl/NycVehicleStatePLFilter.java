package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;

import com.google.common.base.Preconditions;

import gov.sandia.cognition.learning.algorithm.IncrementalLearner;
import gov.sandia.cognition.math.MutableDouble;
import gov.sandia.cognition.statistics.ClosedFormComputableDistribution;
import gov.sandia.cognition.statistics.DataDistribution;

import org.opentrackingtools.VehicleStateInitialParameters;
import org.opentrackingtools.VehicleStatePLFilter;
import org.opentrackingtools.distributions.CountedDataDistribution;
import org.opentrackingtools.distributions.DeterministicDataDistribution;
import org.opentrackingtools.distributions.OnOffEdgeTransDistribution;
import org.opentrackingtools.estimators.OnOffEdgeTransitionEstimatorPredictor;
import org.opentrackingtools.graph.InferenceGraph;
import org.opentrackingtools.graph.InferenceGraphEdge;
import org.opentrackingtools.model.SimpleBayesianParameter;
import org.opentrackingtools.model.VehicleStateDistribution;
import org.opentrackingtools.updater.VehicleStatePLPathGeneratingUpdater;
import org.opentrackingtools.updater.VehicleStatePLUpdater;
import org.opentrackingtools.util.model.MutableDoubleCount;
import org.opentrackingtools.util.model.TransitionProbMatrix;

import java.util.Map.Entry;
import java.util.Random;

public class NycVehicleStatePLFilter extends VehicleStatePLFilter<Observation, NycTrackingGraph> {
  
  private static final long serialVersionUID = 8476483703841751419L;
  protected Long lastProcessedTime = null;

  public NycVehicleStatePLFilter(Observation observation,
      NycTrackingGraph trackingGraph,
      VehicleStateInitialParameters initialParams, Boolean isDebug, Random rng) {
    super(observation, trackingGraph, new NycVehicleStateDistribution.NycVehicleStateDistributionFactory(),
        initialParams, isDebug, rng);
    this.setUpdater(new VehicleStatePLPathGeneratingUpdater<Observation, NycTrackingGraph>(observation, inferredGraph, 
        vehicleStateFactory,
        initialParams, rng));
  }

  public Long getLastProcessedTime() {
    return this.lastProcessedTime;
  }
  
  /**
   * Override so we can updated the run state after the other params are updated.
   */
  @Override
  protected NycVehicleStateDistribution internalUpdate(
      VehicleStateDistribution<Observation> state, Observation obs) {
    
    final NycVehicleStateDistribution newVehicleStateDist = (NycVehicleStateDistribution) super.internalUpdate(state, obs);
    
    RunStateEstimator runStateEstimator = new RunStateEstimator(this.inferredGraph, obs, 
        newVehicleStateDist, 
        newVehicleStateDist.getRunStateParam().getValue().getParentVehicleState(), this.random);
    
    DataDistribution<RunState> newRunStateDist = 
        newVehicleStateDist.getRunStateParam().getParameterPrior().clone();
    runStateEstimator.update(newRunStateDist, newVehicleStateDist.getPathStateParam().getParameterPrior());
    
    /*
     * Again, we use the max weight run state. 
     */
    newVehicleStateDist.setRunStateParam(SimpleBayesianParameter.create(
        newRunStateDist.getMaxValueKey(), newRunStateDist, 
          ((DataDistribution<RunState>)
              new DeterministicDataDistribution<RunState>(newRunStateDist.getMaxValueKey()))));
    
   return newVehicleStateDist; 
  }

  @Override
  public void update(
      DataDistribution<VehicleStateDistribution<Observation>> target,
      Observation obs) {
    super.update(target, obs);
    this.lastProcessedTime = obs.getTime(); 
  }

  @Override
  protected CountedDataDistribution<VehicleStateDistribution<Observation>> internalPriorPrediction(
      VehicleStateDistribution<Observation> predictedState, Observation obs) {
    CountedDataDistribution<VehicleStateDistribution<Observation>> priorPredictiveDist = 
        super.internalPriorPrediction(predictedState, obs);
    
    CountedDataDistribution<VehicleStateDistribution<Observation>> updatedPriorPredictiveDist = 
        new CountedDataDistribution<VehicleStateDistribution<Observation>>(true);
    
    for (Entry<VehicleStateDistribution<Observation>, MutableDouble> vehicleStateEntry : priorPredictiveDist.asMap().entrySet()) {
      
      // TODO FIXME fix CountedDataDistribution.  this casting is gross.
      MutableDoubleCount mCount = (MutableDoubleCount) vehicleStateEntry.getValue();
      NycVehicleStateDistribution nycVehicleStateDist = (NycVehicleStateDistribution)vehicleStateEntry.getKey();
      
      RunStateEstimator runStateEstimator = new RunStateEstimator(this.inferredGraph, obs, 
          nycVehicleStateDist, 
          nycVehicleStateDist.getRunStateParam().getValue().getParentVehicleState(), 
          this.random);
      
      DataDistribution<RunState> predictedRunStateDist = 
        runStateEstimator.createPredictiveDistribution(nycVehicleStateDist.getRunStateParam().getParameterPrior());
      
      /*
       * Could sample, could use the max value.  Either way, we keep
       * the entire distribution around, in case we want to consider the run state space more carefully.
       */
      nycVehicleStateDist.setRunStateParam(SimpleBayesianParameter.create(
          predictedRunStateDist.getMaxValueKey(), predictedRunStateDist, 
          ((DataDistribution<RunState>)
              new DeterministicDataDistribution<RunState>(predictedRunStateDist.getMaxValueKey()))));
      
      updatedPriorPredictiveDist.increment(nycVehicleStateDist, 
         predictedRunStateDist.getTotal() + mCount.doubleValue(), mCount.count);
    }
    
    return updatedPriorPredictiveDist;
  }

  public void setInitialObservation(Observation observation) {
    ((VehicleStatePLPathGeneratingUpdater<Observation, NycTrackingGraph>) this.updater).setInitialObservation(observation);
  }

  /**
   * We expect to be "off-road" quite a bit, so we can get rid of 
   * updates on the transitions.
   */
  @Override
  protected OnOffEdgeTransitionEstimatorPredictor getEdgeTransitionEstimatorPredictor(
      VehicleStateDistribution<Observation> updatedState,
      InferenceGraphEdge graphEdge) {
    return new NycEdgeTransitionEstimatorPredictor(updatedState, graphEdge);
  }

}
