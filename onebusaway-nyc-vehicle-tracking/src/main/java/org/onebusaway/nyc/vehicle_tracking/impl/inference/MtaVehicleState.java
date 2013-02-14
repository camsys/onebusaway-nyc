package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;

import org.opentrackingtools.GpsObservation;
import org.opentrackingtools.graph.InferenceGraph;
import org.opentrackingtools.graph.paths.states.PathStateBelief;
import org.opentrackingtools.impl.VehicleState;
import org.opentrackingtools.statistics.distributions.impl.OnOffEdgeTransDirMulti;
import org.opentrackingtools.statistics.filters.vehicles.road.impl.AbstractRoadTrackingFilter;

public class MtaVehicleState extends VehicleState {
  
  private static final long serialVersionUID = 6667209143463134023L;
  
  private org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState oldTypeVehicleState;

  public MtaVehicleState(InferenceGraph inferredGraph,
      GpsObservation observation, AbstractRoadTrackingFilter updatedFilter,
      PathStateBelief belief, OnOffEdgeTransDirMulti edgeTransitionDist,
      VehicleState parentState, 
      org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState oldTypeVehicleState) {
    
    super(inferredGraph, observation, updatedFilter, belief, edgeTransitionDist,
        parentState);
    
    this.oldTypeVehicleState = oldTypeVehicleState;
  }

  public MtaVehicleState(MtaVehicleState other) {
    super(other);
    this.oldTypeVehicleState = other.oldTypeVehicleState;
  }

  public org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState getOldTypeVehicleState() {
    return oldTypeVehicleState;
  }

  public void setOldTypeVehicleState(
      org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState oldTypeVehicleState) {
    this.oldTypeVehicleState = oldTypeVehicleState;
  }

}
