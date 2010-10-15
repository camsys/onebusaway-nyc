package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Map;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EJourneyPhase;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;

/**
 * Sensor model implementation for vehicle location inference
 * 
 * @author bdferris
 */
public class SensorModelImpl implements SensorModel<Observation> {

  private DeviationModel _snapToStreetDeviationModel = new DeviationModel(30);

  private Map<EJourneyPhase, SensorModel<Observation>> _sensorModels;

  public void setSensorModels(
      Map<EJourneyPhase, SensorModel<Observation>> sensorModels) {
    _sensorModels = sensorModels;
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
    JourneyState js = state.getJourneyState();

    SensorModel<Observation> sm = _sensorModels.get(js.getPhase());
    return sm.likelihood(particle, observation);
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