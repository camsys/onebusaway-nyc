package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel2;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ProbabilityFunction;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Sensor model implementation for vehicle location inference
 * 
 * @author bdferris
 */
public class SensorModelImpl implements SensorModel<Observation> {

  private ProbabilityFunction _snapToStreetDeviationModel = new DeviationModel2(
      20);

  private VehicleStateSensorModel _vehicleStateSensorModel;

  @Autowired
  public void setVehicleSensorModel(
      VehicleStateSensorModel vehicleStateSensorModel) {
    _vehicleStateSensorModel = vehicleStateSensorModel;
  }

  @Override
  public double likelihood(Particle particle, Observation observation) {

    VehicleState state = particle.getData();

    double pStreetLocation = getStreetLocationProbability(state, observation);

    double pJourney = getJourneySensorModelProbability(particle, state,
        observation);

    return pStreetLocation * pJourney;
  }

  public double getJourneySensorModelProbability(Particle particle,
      VehicleState state, Observation observation) {

    VehicleState parentState = null;
    Particle parent = particle.getParent();

    if (parent != null)
      parentState = parent.getData();

    return _vehicleStateSensorModel.likelihood(parentState, state, observation);
  }

  public double getStreetLocationProbability(VehicleState state,
      Observation observation) {
    double pSnapToStreet = getSnapToStreetProbability(state, observation);
    double pNoFlipFlop = 1.0 - getFlipFlopProbability(state, observation);
    return pSnapToStreet * pNoFlipFlop;
  }

  public double getSnapToStreetProbability(VehicleState state,
      Observation observation) {
    EdgeState edgeState = state.getEdgeState();
    CoordinatePoint edgeLocation = edgeState.getLocationOnEdge();
    CoordinatePoint obsLocation = observation.getLocation();
    double snapToStreetDistance = SphericalGeometryLibrary.distance(
        edgeLocation, obsLocation);
    return Math.max(_snapToStreetDeviationModel.probability(snapToStreetDistance),0.001);
  }

  public double getFlipFlopProbability(VehicleState state,
      Observation observation) {

    return 0.0;
  }
}