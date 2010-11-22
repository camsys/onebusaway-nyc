package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.MotionState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Motion model implementation for vehicle location inference.
 * 
 * @author bdferris
 */
public class MotionModelImpl implements MotionModel<Observation> {

  private EdgeStateLibrary _edgeStateLibrary;

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

    VehicleState parentState = parent.getData();

    // First snap to the street grid
    EdgeState edgeState = parentState.getEdgeState();
    if (gpsHasChanged(obs)) {
      CDFMap<EdgeState> edges = _edgeStateLibrary.calculatePotentialEdgeStates(obs.getPoint());
      edgeState = edges.sample();
    }

    MotionState motionState = updateMotionState(parentState, edgeState, obs);

    VehicleState vs = _journeyMotionModel.move(parentState, edgeState,
        motionState, obs);

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

    // System.out.println(d);

    return motionState;
  }

  private boolean gpsHasChanged(Observation obs) {

    NycVehicleLocationRecord record = obs.getRecord();
    NycVehicleLocationRecord prevRecord = obs.getPreviousRecord();

    if (prevRecord == null)
      return true;

    return prevRecord.getLatitude() != record.getLatitude()
        || prevRecord.getLongitude() != record.getLongitude();
  }
}
