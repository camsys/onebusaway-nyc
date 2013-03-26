package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.likelihood.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;

import gov.sandia.cognition.util.AbstractCloneableSerializable;

import org.apache.commons.lang3.builder.ToStringBuilder;

public class RunState {

  public static class RunStateEdgePredictiveResults extends AbstractCloneableSerializable {

    protected double nullStateLogLikelihood;
    protected double runTransitionLogLikelihood;
    protected double runLogLikelihood;
    protected double schedLogLikelihood;
    protected double dscLogLikelihood;
    protected double movedLogLikelihood;
    protected Double total = null;

    public RunStateEdgePredictiveResults() {
    }

    public double getTotalLogLik() {
      if (total == null) {
        total = nullStateLogLikelihood
            + runTransitionLogLikelihood + runLogLikelihood
            + schedLogLikelihood + dscLogLikelihood + movedLogLikelihood;
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

    public void setMovedLogLikelihood(double logLikelihood) {
      total = null;
      this.movedLogLikelihood = logLikelihood;
    }

    @Override
    public String toString() {
      final ToStringBuilder builder = new ToStringBuilder(this);
      builder.append("total", total);
      builder.append("nullStateLogLikelihood", nullStateLogLikelihood);
      builder.append("runTransitionLogLikelihood", runTransitionLogLikelihood);
      builder.append("runLogLikelihood", runLogLikelihood);
      builder.append("schedLogLikelihood", schedLogLikelihood);
      builder.append("dscLogLikelihood", dscLogLikelihood);
      builder.append("movedLogLikelihood", movedLogLikelihood);
      return builder.toString();
    }

    @Override
    public RunStateEdgePredictiveResults clone() {
      final RunStateEdgePredictiveResults clone = (RunStateEdgePredictiveResults) super.clone();
      clone.dscLogLikelihood = dscLogLikelihood;
      clone.runLogLikelihood = runLogLikelihood;
      clone.runTransitionLogLikelihood = runTransitionLogLikelihood;
      clone.schedLogLikelihood = schedLogLikelihood;
      clone.nullStateLogLikelihood = nullStateLogLikelihood;
      return clone;
    }

    public double getNullStateLogLikelihood() {
      return nullStateLogLikelihood;
    }

    public double getRunTransitionLogLikelihood() {
      return runTransitionLogLikelihood;
    }

    public double getRunLogLikelihood() {
      return runLogLikelihood;
    }

    public double getSchedLogLikelihood() {
      return schedLogLikelihood;
    }

    public double getDscLogLikelihood() {
      return dscLogLikelihood;
    }

    public double getMovedLogLikelihood() {
      return movedLogLikelihood;
    }

    public Double getTotal() {
      return total;
    }

  }

  protected BlockStateObservation blockStateObs;
  final protected NycTrackingGraph graph;
  final protected boolean vehicleHasNotMoved;
  final protected VehicleState oldTypeParent;
  protected VehicleState oldTypeVehicleState;
  protected Observation obs;
  protected JourneyState journeyState;
  protected RunStateEdgePredictiveResults likelihoodInfo;

  public RunState(NycTrackingGraph graph, Observation obs,
      BlockStateObservation blockStateObs, boolean vehicleHasNotMoved,
      VehicleState oldTypeParent) {
    this.blockStateObs = blockStateObs;
    this.obs = obs;
    this.graph = graph;
    this.vehicleHasNotMoved = vehicleHasNotMoved;
    this.oldTypeParent = oldTypeParent;
  }

  public BlockStateObservation getBlockStateObs() {
    return blockStateObs;
  }

  public VehicleState getVehicleState() {
    if (this.oldTypeVehicleState == null) {

      final CoordinatePoint point;
      if (this.blockStateObs != null) {
        point = this.blockStateObs.getBlockState().getBlockLocation().getLocation();
      } else {
        point = this.obs.getLocation();
      }

      final MotionState motionState = new MotionState(this.obs.getTime(),
          point, this.vehicleHasNotMoved);

      this.oldTypeVehicleState = new org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState(
          motionState, this.blockStateObs, this.getJourneyState(), null,
          this.obs);
    }
    return this.oldTypeVehicleState;
  }

  public RunState.RunStateEdgePredictiveResults computeAnnotatedLogLikelihood() {
    final Context context = new Context(this.oldTypeParent,
        this.getVehicleState(), obs);
    double runTransitionLogLikelihood = Double.NEGATIVE_INFINITY;
    double nullStateLogLikelihood = Double.NEGATIVE_INFINITY;
    double runLogLikelihood = Double.NEGATIVE_INFINITY;
    double schedLogLikelihood = Double.NEGATIVE_INFINITY;
    double dscLogLikelihood = Double.NEGATIVE_INFINITY;
    final RunState.RunStateEdgePredictiveResults result = new RunState.RunStateEdgePredictiveResults();
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
      runTransitionLogLikelihood = graph.getRunTransitionLikelihood().likelihood(
          context).getLogProbability();
      result.setRunTransitionLogLikelihood(runTransitionLogLikelihood);
      if (runTransitionLogLikelihood <= Double.NEGATIVE_INFINITY)
        return result;
      nullStateLogLikelihood = graph.getNullStateLikelihood().likelihood(
          context).getLogProbability();
      result.setNullStateLogLikelihood(nullStateLogLikelihood);
      // result.setNullStateLogLikelihood(0d);
    } catch (final BadProbabilityParticleFilterException e) {
      e.printStackTrace();
    }

    return result;
  }

  public JourneyState getJourneyState() {
    if (this.journeyState == null) {
      this.journeyState = graph.getJourneyStateTransitionModel().getJourneyState(
          this.blockStateObs, this.oldTypeParent, this.obs,
          this.vehicleHasNotMoved);
    }
    return this.journeyState;
  }

  @Override
  public String toString() {
    final ToStringBuilder builder = new ToStringBuilder(this);
    builder.append("oldTypeVehicleState", oldTypeVehicleState);
    builder.append("oldTypeParent", oldTypeParent);
    return builder.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((oldTypeParent == null) ? 0 : oldTypeParent.hashCode());
    result = prime * result
        + ((getVehicleState() == null) ? 0 : oldTypeVehicleState.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof RunState)) {
      return false;
    }
    final RunState other = (RunState) obj;
    if (oldTypeParent == null) {
      if (other.oldTypeParent != null) {
        return false;
      }
    } else if (!oldTypeParent.equals(other.oldTypeParent)) {
      return false;
    }
    if (getVehicleState() == null) {
      if (other.oldTypeVehicleState != null) {
        return false;
      }
    } else if (!oldTypeVehicleState.equals(other.oldTypeVehicleState)) {
      return false;
    }
    return true;
  }

  public VehicleState getParentVehicleState() {
    return oldTypeParent;
  }

  public boolean getVehicleHasNotMoved() {
    return this.vehicleHasNotMoved;
  }

  public NycTrackingGraph getGraph() {
    return graph;
  }

  public Observation getObs() {
    return obs;
  }

  public void setLikelihoodInfo(
      RunStateEdgePredictiveResults mtaEdgeResultsNotMoved) {
    this.likelihoodInfo = mtaEdgeResultsNotMoved;
  }

  public RunStateEdgePredictiveResults getLikelihoodInfo() {
    return likelihoodInfo;
  }

  public void setBlockStateObs(BlockStateObservation blockStateObs) {
    this.blockStateObs = blockStateObs;
    this.journeyState = null;
    this.oldTypeVehicleState = null;
  }
}
