package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFactory;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationInferenceRecord;

/**
 * Create initial particles from an initial observation.
 * 
 * @author bdferris
 */
public class ParticleFactoryImpl implements
    ParticleFactory<VehicleLocationInferenceRecord> {

  private int _initialNumberOfParticles = 50;

  public void setInitialNumberOfParticles(int initialNumberOfParticles) {
    _initialNumberOfParticles = initialNumberOfParticles;
  }

  @Override
  public List<Particle> createParticles(double timestamp,
      VehicleLocationInferenceRecord obs) {

    List<Particle> particles = new ArrayList<Particle>(
        _initialNumberOfParticles);

    for (int i = 0; i < _initialNumberOfParticles; i++) {
      Particle p = new Particle(timestamp);
      VehicleState state = new VehicleState();
      state.setLat(obs.getLat());
      state.setLon(obs.getLon());
      p.setData(state);
      particles.add(p);
    }

    return particles;
  }
}
