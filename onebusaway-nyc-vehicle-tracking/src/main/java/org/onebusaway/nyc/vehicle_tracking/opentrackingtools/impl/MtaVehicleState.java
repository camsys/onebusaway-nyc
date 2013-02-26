package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MtaPathStateBelief;

import gov.sandia.cognition.statistics.DataDistribution;

import org.opentrackingtools.GpsObservation;
import org.opentrackingtools.graph.InferenceGraph;
import org.opentrackingtools.graph.paths.states.PathStateBelief;
import org.opentrackingtools.impl.VehicleState;
import org.opentrackingtools.statistics.distributions.impl.DeterministicDataDistribution;
import org.opentrackingtools.statistics.distributions.impl.OnOffEdgeTransDistribution;
import org.opentrackingtools.statistics.filters.vehicles.road.impl.AbstractRoadTrackingFilter;

public class MtaVehicleState extends VehicleState {
  
  private static final long serialVersionUID = 6667209143463134023L;
  
  private DataDistribution<RunState> runStateBelief;

  public MtaVehicleState(InferenceGraph inferredGraph,
      GpsObservation observation, AbstractRoadTrackingFilter updatedFilter,
      PathStateBelief belief, OnOffEdgeTransDistribution edgeTransitionDist,
      VehicleState parentState, 
      DeterministicDataDistribution<RunState> runStateBelief) {
    
    super(inferredGraph, observation, updatedFilter, belief, edgeTransitionDist,
        parentState);
    
    this.runStateBelief = runStateBelief;
  }

  public MtaVehicleState(MtaVehicleState other) {
    super(other);
    this.runStateBelief = other.getRunStateBelief();
  }

  public DataDistribution<RunState> getRunStateBelief() {
    if (runStateBelief == null)
      runStateBelief = ((MtaPathStateBelief) this.getBelief()).getRunStateBelief();
    return runStateBelief;
  }

  public void setRunStateBelief(DataDistribution<RunState> avgRunStateBelief) {
    this.runStateBelief = avgRunStateBelief;
  }

  @Override
  public MtaVehicleState clone() {
    MtaVehicleState clone = (MtaVehicleState) super.clone();
    clone.runStateBelief = this.getRunStateBelief();
    return clone;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (!(obj instanceof MtaVehicleState)) {
      return false;
    }
    MtaVehicleState other = (MtaVehicleState) obj;
    if (this.getRunStateBelief() == null) {
      if (other.runStateBelief != null) {
        return false;
      }
    } else if (!runStateBelief.equals(other.runStateBelief)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result
        + ((this.getRunStateBelief()== null) ? 0 : runStateBelief.hashCode());
    return result;
  }

}
