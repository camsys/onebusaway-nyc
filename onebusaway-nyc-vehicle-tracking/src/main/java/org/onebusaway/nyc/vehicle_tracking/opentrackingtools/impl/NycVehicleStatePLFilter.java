package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;

import gov.sandia.cognition.math.MutableDouble;
import gov.sandia.cognition.statistics.DataDistribution;

import org.opentrackingtools.VehicleStateInitialParameters;
import org.opentrackingtools.VehicleStatePLFilter;
import org.opentrackingtools.VehicleStatePLPathSamplingFilter;
import org.opentrackingtools.distributions.CountedDataDistribution;
import org.opentrackingtools.distributions.DeterministicDataDistribution;
import org.opentrackingtools.estimators.OnOffEdgeTransitionEstimatorPredictor;
import org.opentrackingtools.graph.InferenceGraphEdge;
import org.opentrackingtools.model.SimpleBayesianParameter;
import org.opentrackingtools.model.VehicleStateDistribution;
import org.opentrackingtools.updater.VehicleStatePLPathSamplingUpdater;
import org.opentrackingtools.util.model.MutableDoubleCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map.Entry;
import java.util.Random;

public class NycVehicleStatePLFilter extends
    VehicleStatePLPathSamplingFilter<Observation, NycTrackingGraph> {

  private static final long serialVersionUID = 8476483703841751419L;
  private static final double MAX_COVARIANCE = 2000d;

  protected static Logger _log = LoggerFactory
			.getLogger(NycVehicleStatePLFilter.class);
  protected Long lastProcessedTime = null;

  public NycVehicleStatePLFilter(Observation observation,
      NycTrackingGraph trackingGraph,
      VehicleStateInitialParameters initialParams, Boolean isDebug, Random rng) {
    super(observation, trackingGraph,
        new NycVehicleStateDistribution.NycVehicleStateDistributionFactory(),
        initialParams, isDebug, rng);
    this.setUpdater(new VehicleStatePLPathSamplingUpdater<Observation, NycTrackingGraph>(
        observation, inferredGraph, vehicleStateFactory, initialParams, isDebug, rng));
  }

  public Long getLastProcessedTime() {
    return this.lastProcessedTime;
  }

  /**
   * Override so we can updated the run state after the other params are
   * updated.
   */
  @Override
  protected NycVehicleStateDistribution internalUpdate(
      VehicleStateDistribution<Observation> state, Observation obs) {

    final NycVehicleStateDistribution newVehicleStateDist = (NycVehicleStateDistribution) super.internalUpdate(
        state, obs);

    final RunStateEstimator runStateEstimator = new RunStateEstimator(
        this.inferredGraph, obs, (NycVehicleStateDistribution) state);

    final DataDistribution<RunState> newRunStateDist = newVehicleStateDist.getRunStateParam().getParameterPrior().clone();
    runStateEstimator.update(newRunStateDist,
        newVehicleStateDist.getPathStateParam().getParameterPrior());
    
    if (newVehicleStateDist.getPathStateParam().getParameterPrior().getCovariance().getElement(0, 0) > MAX_COVARIANCE) {
			_log.error("covariance["
					+ obs.getRecord().getVehicleId()
					+ "]:"
					+ newVehicleStateDist.getPathStateParam()
							.getParameterPrior().getCovariance().toString());
		}

    /*
     * Again, we use the max weight run state.
     */
    newVehicleStateDist.setRunStateParam(SimpleBayesianParameter.create(
        newRunStateDist.getMaxValueKey(),
        newRunStateDist,
        ((DataDistribution<RunState>) new DeterministicDataDistribution<RunState>(
            newRunStateDist.getMaxValueKey()))));

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
    final CountedDataDistribution<VehicleStateDistribution<Observation>> priorPredictiveDist = super.internalPriorPrediction(
        predictedState, obs);

    final CountedDataDistribution<VehicleStateDistribution<Observation>> updatedPriorPredictiveDist = new CountedDataDistribution<VehicleStateDistribution<Observation>>(
        true);

    for (final Entry<VehicleStateDistribution<Observation>, MutableDouble> vehicleStateEntry : priorPredictiveDist.asMap().entrySet()) {

      // TODO FIXME fix CountedDataDistribution. this casting is gross.
      final MutableDoubleCount mCount = (MutableDoubleCount) vehicleStateEntry.getValue();
      final NycVehicleStateDistribution nycVehicleStateDist = (NycVehicleStateDistribution) vehicleStateEntry.getKey();

      final RunStateEstimator runStateEstimator = new RunStateEstimator(
          this.inferredGraph, obs, nycVehicleStateDist);

      final DataDistribution<RunState> predictedRunStateDist = runStateEstimator.createPredictiveDistribution(nycVehicleStateDist.getRunStateParam().getParameterPrior());

      /*
       * Could sample, could use the max value. Either way, we keep the entire
       * distribution around, in case we want to consider the run state space
       * more carefully.
       */
      nycVehicleStateDist.setRunStateParam(SimpleBayesianParameter.create(
          predictedRunStateDist.getMaxValueKey(),
          predictedRunStateDist,
          ((DataDistribution<RunState>) new DeterministicDataDistribution<RunState>(
              predictedRunStateDist.getMaxValueKey()))));

      updatedPriorPredictiveDist.increment(nycVehicleStateDist,
          predictedRunStateDist.getTotal() + mCount.doubleValue(), mCount.count);
    }

    return updatedPriorPredictiveDist;
  }

  public void setInitialObservation(Observation observation) {
    ((VehicleStatePLPathSamplingUpdater<Observation, NycTrackingGraph>) this.updater).setInitialObservation(observation);
  }

  /**
   * We expect to be "off-road" quite a bit, so we can get rid of updates on the
   * transitions.
   */
  @Override
  protected OnOffEdgeTransitionEstimatorPredictor getEdgeTransitionEstimatorPredictor(
      VehicleStateDistribution<Observation> updatedState,
      InferenceGraphEdge graphEdge) {
    return new NycEdgeTransitionEstimatorPredictor(updatedState, graphEdge);
  }

}
