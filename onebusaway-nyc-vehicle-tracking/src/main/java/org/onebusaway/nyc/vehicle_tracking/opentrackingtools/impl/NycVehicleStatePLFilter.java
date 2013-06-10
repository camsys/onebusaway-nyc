package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;

import com.google.common.base.Preconditions;

import gov.sandia.cognition.math.MutableDouble;
import gov.sandia.cognition.math.matrix.Matrix;
import gov.sandia.cognition.math.matrix.MatrixFactory;
import gov.sandia.cognition.math.matrix.VectorFactory;
import gov.sandia.cognition.statistics.DataDistribution;
import gov.sandia.cognition.statistics.DistributionWithMean;
import gov.sandia.cognition.statistics.distribution.MultivariateGaussian;

import org.opentrackingtools.VehicleStateInitialParameters;
import org.opentrackingtools.VehicleStatePLFilter;
import org.opentrackingtools.VehicleStatePLPathSamplingFilter;
import org.opentrackingtools.distributions.CountedDataDistribution;
import org.opentrackingtools.distributions.DeterministicDataDistribution;
import org.opentrackingtools.distributions.ScaledInverseGammaCovDistribution;
import org.opentrackingtools.estimators.OnOffEdgeTransitionEstimatorPredictor;
import org.opentrackingtools.graph.InferenceGraphEdge;
import org.opentrackingtools.graph.InferenceGraphSegment;
import org.opentrackingtools.model.SimpleBayesianParameter;
import org.opentrackingtools.model.VehicleStateDistribution;
import org.opentrackingtools.model.VehicleStateDistribution.VehicleStateDistributionFactory;
import org.opentrackingtools.paths.PathEdge;
import org.opentrackingtools.updater.VehicleStatePLPathSamplingUpdater;
import org.opentrackingtools.util.model.MutableDoubleCount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map.Entry;
import java.util.Collection;
import java.util.Random;

public class NycVehicleStatePLFilter extends
    VehicleStatePLPathSamplingFilter<Observation, NycTrackingGraph> {

  private static final long serialVersionUID = 8476483703841751419L;
  private static final double MAX_COVARIANCE = 2000d;

  protected static Logger _log = LoggerFactory
			.getLogger(NycVehicleStatePLFilter.class);
  protected Long lastProcessedTime = null;
  
  public static class NycVehicleStatePLUpdater extends VehicleStatePLPathSamplingUpdater<Observation, NycTrackingGraph> {
    
    public NycVehicleStatePLUpdater(
        Observation obs,
        NycTrackingGraph inferencedGraph,
        VehicleStateDistributionFactory<Observation, NycTrackingGraph> vehicleStateFactory,
        VehicleStateInitialParameters parameters, boolean isDebug, Random rng) {
      super(obs, inferencedGraph, vehicleStateFactory, parameters, isDebug, rng);
    }

    @Override
    public DataDistribution<VehicleStateDistribution<Observation>>
        createInitialParticles(int numParticles) {
      final DataDistribution<VehicleStateDistribution<Observation>> retDist =
          new CountedDataDistribution<VehicleStateDistribution<Observation>>(true);

      /*
       * Start by creating an off-road vehicle state with which we can obtain the surrounding
       * edges.
       */
      final NycVehicleStateDistribution nullState =
          (NycVehicleStateDistribution) this.vehicleStateFactory.createInitialVehicleState(
          this.parameters, this.inferenceGraph,
          this.initialObservation, this.random,
          PathEdge.nullPathEdge);
      final MultivariateGaussian initialMotionStateDist =
          nullState.getMotionStateParam().getParameterPrior();
      final Collection<InferenceGraphSegment> edges =
          this.inferenceGraph.getNearbyEdges(initialMotionStateDist,
              initialMotionStateDist.getCovariance());

      for (int i = 0; i < numParticles; i++) {
        /*
         * From the surrounding edges, we create states on those edges.
         */
        final CountedDataDistribution<VehicleStateDistribution<Observation>> statesOnEdgeDistribution =
            new CountedDataDistribution<VehicleStateDistribution<Observation>>(
                true);

        final double nullEdgeLogLikelihood =
            nullState.getEdgeTransitionParam()
                .getConditionalDistribution().getProbabilityFunction()
                .logEvaluate(InferenceGraphEdge.nullGraphEdge);
        final double nullObsLogLikelihood = this.computeLogLikelihood(nullState,
                    this.initialObservation);
        nullState.setEdgeTransitionLogLikelihood(nullEdgeLogLikelihood);
        nullState.setObsLogLikelihood(nullObsLogLikelihood);
        final double nullTotalLogLikelihood = nullState.getEdgeTransitionLogLikelihood()
            + nullState.getPathStateDistLogLikelihood()
            + nullState.getObsLogLikelihood()
            + nullState.getRunStateParam().getValue().getLikelihoodInfo().getTotalLogLik();

        statesOnEdgeDistribution
            .increment(nullState, nullTotalLogLikelihood);
        
        /*
         * Make sure we're fair about the sampled initial location and
         * set it here.  Otherwise, if we don't do this, each call
         * to createInitialVehicleState will sample a new location.
         */
        VehicleStateInitialParameters newParams = new VehicleStateInitialParameters(this.parameters);
        newParams.setInitialMotionState(initialMotionStateDist.sample(this.random));

        for (final InferenceGraphSegment segment : edges) {

          final PathEdge pathEdge = new PathEdge(segment, 0d, false);

          final NycVehicleStateDistribution stateOnEdge =
              (NycVehicleStateDistribution) this.vehicleStateFactory.createInitialVehicleState(
              newParams, this.inferenceGraph,
              this.initialObservation, this.random, pathEdge);

          final double edgeLikelihood =
              stateOnEdge.getEdgeTransitionParam()
                  .getConditionalDistribution()
                  .getProbabilityFunction()
                  .logEvaluate(pathEdge.getInferenceGraphSegment());
          final double obsLikelihood =
                  this.computeLogLikelihood(stateOnEdge,
                      this.initialObservation);
          
          stateOnEdge.setEdgeTransitionLogLikelihood(edgeLikelihood);
          stateOnEdge.setObsLogLikelihood(obsLikelihood);
          
          final double logLikelihood = stateOnEdge.getEdgeTransitionLogLikelihood()
              + stateOnEdge.getPathStateDistLogLikelihood() 
              + stateOnEdge.getObsLogLikelihood()
              + stateOnEdge.getRunStateParam().getValue().getLikelihoodInfo().getTotalLogLik();

          statesOnEdgeDistribution
              .increment(stateOnEdge, logLikelihood);
        }

        VehicleStateDistribution<Observation> sampledDist = statesOnEdgeDistribution.sample(this.random);
        if (this.isDebug)
          sampledDist.setTransitionStateDistribution(statesOnEdgeDistribution);
        retDist.increment(sampledDist);
      }

      Preconditions.checkState(retDist.getDomainSize() > 0);
      return retDist;
    } 
  }

  public NycVehicleStatePLFilter(Observation observation,
      NycTrackingGraph trackingGraph,
      VehicleStateInitialParameters initialParams, Boolean isDebug, Random rng) {
    super(observation, trackingGraph,
        new NycVehicleStateDistribution.NycVehicleStateDistributionFactory(),
        initialParams, isDebug, rng);
    this.setUpdater(new NycVehicleStatePLUpdater(
        observation, inferredGraph, vehicleStateFactory, initialParams, isDebug == Boolean.TRUE, rng));
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
    
    final ScaledInverseGammaCovDistribution currentModelCovDistribution =
        (ScaledInverseGammaCovDistribution) (newVehicleStateDist.getPathStateParam().getValue().isOnRoad()
        ? newVehicleStateDist.getOnRoadModelCovarianceParam()
            .getParameterPrior().clone() : newVehicleStateDist
            .getOffRoadModelCovarianceParam()
            .getParameterPrior().clone());
    
    /**
     *  If the on or off road state covariance is outside its threshold,
     *  then reset both the their priors.
     */
    if (this.parameters.getStateCovarianceThreshold() != null
        && currentModelCovDistribution.getInverseGammaDist().getMean() > this.parameters.getStateCovarianceThreshold()) {
      _log.warn("Estimated state covariance mean (" + 
        currentModelCovDistribution.getInverseGammaDist().getMean() + ") exceeds threshold ("
        + this.parameters.getStateCovarianceThreshold() + ").");
      final int onRoadInitialDof = parameters.getOnRoadCovDof() - 1; 
      final DistributionWithMean<Matrix> onRoadCovDistribution =
          new ScaledInverseGammaCovDistribution(1, 
              parameters.getOnRoadCovDof(), 
              MatrixFactory.getDefault()
              .createDiagonal(parameters.getOnRoadStateCov())
              .scale(onRoadInitialDof).getElement(0, 0) );
      final SimpleBayesianParameter<Matrix, MultivariateGaussian, DistributionWithMean<Matrix>> onRoadCovParam =
          SimpleBayesianParameter
              .create(
                  onRoadCovDistribution.getMean(),
                  new MultivariateGaussian(VectorFactory.getDefault()
                      .createVector1D(), onRoadCovDistribution
                      .getMean()), onRoadCovDistribution);
      newVehicleStateDist.setOnRoadModelCovarianceParam(onRoadCovParam);
      
      final int offRoadInitialDof = parameters.getOffRoadCovDof() - 1;
      final DistributionWithMean<Matrix> offRoadCovDistribution =
          new ScaledInverseGammaCovDistribution(2, 
              parameters.getOffRoadCovDof(),
              MatrixFactory.getDefault().createDiagonal(parameters.getOffRoadStateCov())
              .scale(offRoadInitialDof).getElement(0, 0));
      final SimpleBayesianParameter<Matrix, MultivariateGaussian, DistributionWithMean<Matrix>> offRoadCovParam =
          SimpleBayesianParameter
              .create(
                  offRoadCovDistribution.getMean(),
                  new MultivariateGaussian(VectorFactory.getDefault()
                      .createVector1D(), offRoadCovDistribution
                      .getMean()), offRoadCovDistribution);
      newVehicleStateDist.setOffRoadModelCovarianceParam(offRoadCovParam);
    }
    
    final ScaledInverseGammaCovDistribution currentObsCovDistribution = (ScaledInverseGammaCovDistribution) 
        newVehicleStateDist.getObservationCovarianceParam().getParameterPrior();
    
    /**
     * If the tracked observation covariance goes out of
     * range, reset it to its prior.
     */
    if (this.parameters.getObsCovarianceThreshold() != null
        && currentObsCovDistribution.getInverseGammaDist().getMean() > this.parameters.getObsCovarianceThreshold()) {
      _log.warn("Estimated observation covariance mean (" + 
        currentObsCovDistribution.getInverseGammaDist().getMean() + ") exceeds threshold ("
        + this.parameters.getObsCovarianceThreshold() + ").");
      final int obsInitialDof =
          parameters.getObsCovDof() - 1;
      final DistributionWithMean<Matrix> obsCovDistribution =
          new ScaledInverseGammaCovDistribution(2,
              parameters.getObsCovDof(),
              MatrixFactory.getDefault()
              .createDiagonal(parameters.getObsCov())
              .scale(obsInitialDof).getElement(0, 0));
      final SimpleBayesianParameter<Matrix, MultivariateGaussian, DistributionWithMean<Matrix>> observationCovParam =
          SimpleBayesianParameter.create(
              obsCovDistribution.getMean(), new MultivariateGaussian(
                  VectorFactory.getDefault().createVector2D(),
                  obsCovDistribution.getMean()), obsCovDistribution);
      newVehicleStateDist.setObservationCovarianceParam(observationCovParam);
    }

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
