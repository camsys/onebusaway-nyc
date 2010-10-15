package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Map;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.motion_model.JourneyMotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EJourneyPhase;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Motion model implementation for vehicle location inference.
 * 
 * @author bdferris
 */
public class MotionModelImpl implements MotionModel<Observation> {

  private EdgeStateLibrary _edgeStateLibrary;

  private Map<EJourneyPhase, JourneyMotionModel> _motionModels;

  /**
   * Distance, in meters, that a bus has to travel to be considered "in motion"
   */
  private double _motionThreshold = 20;

  @Autowired
  public void setEdgeStateLibrary(EdgeStateLibrary edgeStateLibrary) {
    _edgeStateLibrary = edgeStateLibrary;
  }

  public void setMotionModels(
      Map<EJourneyPhase, JourneyMotionModel> motionModels) {
    _motionModels = motionModels;
  }

  public void setMotionThreshold(double motionThreshold) {
    _motionThreshold = motionThreshold;
  }

  @Override
  public Particle move(Particle parent, double timestamp, double timeElapsed,
      Observation obs) {

    // First snap to the street grid
    CDFMap<EdgeState> edges = _edgeStateLibrary.calculatePotentialEdgeStates(obs.getPoint());
    EdgeState edgeState = edges.sample();
    
    VehicleState parentState = parent.getData();
    MotionState motionState = parentState.getMotionState();

    JourneyState parentJourneyState = parentState.getJourneyState();
    JourneyMotionModel motionModel = _motionModels.get(parentJourneyState.getPhase());

    JourneyState js = motionModel.move(parent, parentState, edgeState,
        motionState, obs, parentJourneyState);

    CoordinatePoint locationOnEdge = edgeState.getLocationOnEdge();
    double d = SphericalGeometryLibrary.distance(
        motionState.getLastInMotionLocation(), locationOnEdge);
    if (d > _motionThreshold)
      motionState = new MotionState(obs.getTime(), locationOnEdge);

    VehicleState vs = new VehicleState(edgeState, motionState, js);
    return new Particle(timestamp, parent, 1.0, vs);
  }
}
