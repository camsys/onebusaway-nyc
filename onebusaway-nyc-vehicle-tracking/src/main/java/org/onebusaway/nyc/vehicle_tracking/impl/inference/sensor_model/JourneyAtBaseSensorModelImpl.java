package org.onebusaway.nyc.vehicle_tracking.impl.inference.sensor_model;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.Observation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.EdgeState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModel;
import org.onebusaway.nyc.vehicle_tracking.services.BaseLocationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * 
 * @author bdferris
 * 
 */
public class JourneyAtBaseSensorModelImpl implements SensorModel<Observation> {

  private BaseLocationService _baseLocationService;

  @Autowired
  public void setBaseLocationService(BaseLocationService baseLocationService) {
    _baseLocationService = baseLocationService;
  }

  @Override
  public double likelihood(Particle particle, Observation observation) {

    VehicleState state = particle.getData();
    EdgeState edgeState = state.getEdgeState();

    String baseName = _baseLocationService.getBaseNameForLocation(edgeState.getLocationOnEdge());

    // You're either at a base or you're not
    return (baseName != null) ? 1.0 : 0.0;
  }
}
