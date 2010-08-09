package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.services.VehicleLocationInferenceRecord;

/**
 * Motion model implementation for vehicle location inference.
 * 
 * @author bdferris
 */
public class MotionModelImpl implements
    MotionModel<VehicleLocationInferenceRecord> {

  @Override
  public Particle move(Particle parent, double timestamp, double timeElapsed,
      VehicleLocationInferenceRecord obs) {

    Particle particle = new Particle(timestamp, parent);

    VehicleState state = new VehicleState((VehicleState) parent.getData());
    state.setLat(obs.getLat());
    state.setLon(obs.getLon());
    particle.setData(state);

    return particle;
  }

}
