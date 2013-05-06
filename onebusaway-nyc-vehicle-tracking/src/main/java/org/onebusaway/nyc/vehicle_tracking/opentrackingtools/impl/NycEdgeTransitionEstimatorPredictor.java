package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.opentrackingtools.distributions.OnOffEdgeTransPriorDistribution;
import org.opentrackingtools.estimators.OnOffEdgeTransitionEstimatorPredictor;
import org.opentrackingtools.graph.InferenceGraphEdge;
import org.opentrackingtools.model.VehicleStateDistribution;

import java.util.Collection;

public class NycEdgeTransitionEstimatorPredictor extends
    OnOffEdgeTransitionEstimatorPredictor {

  public NycEdgeTransitionEstimatorPredictor(
      VehicleStateDistribution<?> currentState, InferenceGraphEdge currentEdge) {
    super(currentState, currentEdge);
  }

  @Override
  public OnOffEdgeTransPriorDistribution learn(
      Collection<? extends InferenceGraphEdge> data) {
    // return super.learn(data);
    return null;
  }

  @Override
  public void update(OnOffEdgeTransPriorDistribution prior,
      InferenceGraphEdge toEdge) {
    // super.update(prior, toEdge);
  }

  @Override
  public void update(OnOffEdgeTransPriorDistribution target,
      Iterable<? extends InferenceGraphEdge> to) {
    // super.update(target, to);
  }

}
