package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
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

  private BlockStateTransitionModel _blockStateTransitionModel;

  private JourneyStateTransitionModel _journeyMotionModel;

  /**
   * Distance, in meters, that a bus has to travel to be considered "in motion"
   */
  private double _motionThreshold = 20;

  @Autowired
  public void setEdgeStateLibrary(EdgeStateLibrary edgeStateLibrary) {
    _edgeStateLibrary = edgeStateLibrary;
  }

  @Autowired
  public void setBlockStateTransitionModel(
      BlockStateTransitionModel blockStateTransitionModel) {
    _blockStateTransitionModel = blockStateTransitionModel;
  }

  @Autowired
  public void setJourneyMotionModel(
      JourneyStateTransitionModel journeyMotionModel) {
    _journeyMotionModel = journeyMotionModel;
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

    MotionState motionState = updateMotionState(parentState, edgeState, obs);

    BlockState blockState = _blockStateTransitionModel.transitionBlockState(
        parentState, edgeState, obs);

    JourneyState js = _journeyMotionModel.move(parentState, edgeState,
        motionState, blockState, obs);

    VehicleState vs = new VehicleState(edgeState, motionState, blockState, js);
    return new Particle(timestamp, parent, 1.0, vs);
  }

  public MotionState updateMotionState(VehicleState parentState,
      EdgeState edgeState, Observation obs) {
    
    MotionState motionState = parentState.getMotionState();

    CoordinatePoint locationOnEdge = edgeState.getLocationOnEdge();
    double d = SphericalGeometryLibrary.distance(
        motionState.getLastInMotionLocation(), locationOnEdge);

    if (d > _motionThreshold)
      motionState = new MotionState(obs.getTime(), locationOnEdge);

    return motionState;
  }
}
