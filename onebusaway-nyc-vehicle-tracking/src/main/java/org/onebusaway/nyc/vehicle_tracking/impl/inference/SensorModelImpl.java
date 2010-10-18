package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sensor model implementation for vehicle location inference
 * 
 * @author bdferris
 */
public class SensorModelImpl implements SensorModel<Observation> {

  private DeviationModel _snapToStreetDeviationModel = new DeviationModel(30);

  private VehicleStateSensorModel _vehicleStateSensorModel;

  @Autowired
  public void setVehicleSensorModel(
      VehicleStateSensorModel vehicleStateSensorModel) {
    _vehicleStateSensorModel = vehicleStateSensorModel;
  }

  @Override
  public double likelihood(Particle particle, Observation observation) {

    VehicleState state = particle.getData();

    double pSnapToStreet = getSnapToStreetProbability(state, observation);

    double pJourney = getJourneySensorModelProbability(particle, state,
        observation);

    return pSnapToStreet * pJourney;
  }

  public double getJourneySensorModelProbability(Particle particle,
      VehicleState state, Observation observation) {

    VehicleState parentState = null;
    Particle parent = particle.getParent();

    if (parent != null)
      parentState = parent.getData();

    return _vehicleStateSensorModel.likelihood(parentState, state, observation);
  }

  public double getSnapToStreetProbability(VehicleState state,
      Observation observation) {
    EdgeState edgeState = state.getEdgeState();
    CoordinatePoint edgeLocation = edgeState.getLocationOnEdge();
    CoordinatePoint obsLocation = observation.getLocation();
    double snapToStreetDistance = SphericalGeometryLibrary.distance(
        edgeLocation, obsLocation);
    return _snapToStreetDeviationModel.probability(snapToStreetDistance);
  }
}