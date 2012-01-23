/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

/**
 * Convenience object collecting all the things that are needed to run a
 * {@link ParticleFilter}.
 * 
 * @author bdferris
 * @see ParticleFilter
 * @see ParticleFactory
 * @see MotionModel
 * @see SensorModel
 * @param <OBS>
 */
public class ParticleFilterModel<OBS> {

  private ParticleFactory<OBS> _particleFactory;

  private MotionModel<OBS> _motionModel;

  private SensorModel<OBS> _sensorModel;

  public static <O> ParticleFilterModel<O> create(
      ParticleFactory<O> particleFactory, MotionModel<O> motionModel,
      SensorModel<O> sensorModel) {

    ParticleFilterModel<O> pfm = new ParticleFilterModel<O>();
    pfm.setParticleFactory(particleFactory);
    pfm.setMotionModel(motionModel);
    pfm.setSensorModel(sensorModel);
    return pfm;
  }

  public void setParticleFactory(ParticleFactory<OBS> particleFactory) {
    _particleFactory = particleFactory;
  }

  public void setMotionModel(MotionModel<OBS> motionModel) {
    _motionModel = motionModel;
  }

  public void setSensorModel(SensorModel<OBS> sensorModel) {
    _sensorModel = sensorModel;
  }

  public ParticleFactory<OBS> getParticleFactory() {
    return _particleFactory;
  }

  public MotionModel<OBS> getMotionModel() {
    return _motionModel;
  }

  public SensorModel<OBS> getSensorModel() {
    return _sensorModel;
  }
}
