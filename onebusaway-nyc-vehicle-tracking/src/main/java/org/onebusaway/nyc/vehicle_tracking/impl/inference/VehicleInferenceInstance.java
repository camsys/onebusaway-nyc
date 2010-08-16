package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilter;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFilterModel;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;

public class VehicleInferenceInstance {

  private static ParticleFilterModel<NycVehicleLocationRecord> _model = new ParticleFilterModel<NycVehicleLocationRecord>();

  static {
    _model.setParticleFactory(new ParticleFactoryImpl());
    _model.setMotionModel(new MotionModelImpl());
    _model.setSensorModel(new SensorModelImpl());
  }

  private ParticleFilter<NycVehicleLocationRecord> _particleFilter;

  /**
   * Note: We should keep creation of {@link VehicleInferenceInstance} cheap
   * because we create these frequently in
   * {@link VehicleLocationInferenceServiceImpl}.
   */
  public VehicleInferenceInstance() {

  }

  public synchronized void handleUpdate(NycVehicleLocationRecord record) {

    ensureInitialized();

    // If this record occurs BEFORE the most recent update, we ignore it
    if (record.getTime() < _particleFilter.getTimeOfLastUpdated())
      return;

    _particleFilter.updateFilter(record.getTime(), record);
  }

  public VehicleState getCurrentState() {
    Particle particle = _particleFilter.getMostLikelyParticle();
    return particle.getData();
  }

  /****
   * Private Methods
   ****/

  /**
   * We do lazy initialization because creation of
   * {@link VehicleInferenceInstance} should be cheap.
   */
  private void ensureInitialized() {

    if (_particleFilter != null)
      return;

    _particleFilter = new ParticleFilter<NycVehicleLocationRecord>(_model);
  }
}
