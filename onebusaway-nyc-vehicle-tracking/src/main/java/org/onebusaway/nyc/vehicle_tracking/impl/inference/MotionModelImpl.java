package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;

/**
 * Motion model implementation for vehicle location inference.
 * 
 * @author bdferris
 */
public class MotionModelImpl implements
    MotionModel<NycVehicleLocationRecord> {

  @Override
  public Particle move(Particle parent, double timestamp, double timeElapsed,
      NycVehicleLocationRecord obs) {

    Particle particle = new Particle(timestamp, parent);

    VehicleState state = new VehicleState((VehicleState) parent.getData());
    state.setLat(obs.getLatitude());
    state.setLon(obs.getLongitude());
    particle.setData(state);

    return particle;
  }

}
