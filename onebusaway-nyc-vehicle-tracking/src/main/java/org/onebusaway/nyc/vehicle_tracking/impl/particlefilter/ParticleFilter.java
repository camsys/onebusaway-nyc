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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Core particle filter implementation
 * 
 * @author bdferris
 * 
 * @param <OBS>
 */
public class ParticleFilter<OBS> {

  private ParticleFactory<OBS> _particleFactory;

  private SensorModel<OBS> _sensorModel;

  private MotionModel<OBS> _motionModel = null;

  private List<Particle> _particles = new ArrayList<Particle>();

  private List<Particle> _weightedParticles = new ArrayList<Particle>();

  private double _timeOfLastUpdate = 0L;

  private boolean _seenFirst;

  private Particle _mostLikelyParticle;

  private Particle _leastLikelyParticle;

  /***************************************************************************
   * Public Methods
   **************************************************************************/

  public ParticleFilter(ParticleFilterModel<OBS> model) {
    this(model.getParticleFactory(), model.getSensorModel(),
        model.getMotionModel());
  }

  public ParticleFilter(ParticleFactory<OBS> particleFactory,
      SensorModel<OBS> sensorModel, MotionModel<OBS> motionModel) {
    _particleFactory = particleFactory;
    _sensorModel = sensorModel;
    _motionModel = motionModel;
  }

  public void reset() {
    if (_particles != null) {
      _particles.clear();
      _particles = null;
    }
    _timeOfLastUpdate = 0L;
    _seenFirst = false;
  }

  public List<Particle> getParticleList() {
    return Collections.unmodifiableList(_particles);
  }

  public List<Particle> getWeightedParticles() {
    return Collections.unmodifiableList(_weightedParticles);
  }

  public List<Particle> getSampledParticles() {
    return Collections.unmodifiableList(_particles);
  }

  public Particle getMostLikelyParticle() {
    return _mostLikelyParticle;
  }

  public Particle getLeastLikelyParticle() {
    return _leastLikelyParticle;
  }

  /**
   * @return the last time at which updateFilter was called, in milliseconds
   */
  public double getTimeOfLastUpdated() {
    return _timeOfLastUpdate;
  }

  /**
   * This is what you can use to initialize the filter if you have a particular
   * set of particles you want to use
   */
  public void initializeFilter(Collection<Particle> initialParticles) {
    if (_seenFirst)
      throw new IllegalStateException(
          "Error: particle filter has already been initialized.");
    _particles = new ArrayList<Particle>(initialParticles);
    _seenFirst = true;
  }

  /**
   * This is the major method that will be called repeatedly by an outside
   * client in order to drive the filter. Each call runs a single timestep.
   */
  public void updateFilter(double timestamp, OBS observation)
      throws ParticleFilterException {

    boolean firstTime = checkFirst(timestamp, observation);
    runSingleTimeStep(timestamp, observation, !firstTime);
    _timeOfLastUpdate = timestamp;
  }

  /***************************************************************************
   * Abstract Methods
   **************************************************************************/

  /*
   * Returns an ArrayList, each entry of which is a Particle. This allows
   * subclasses to determine what kind of particles to create, and where to
   * place them.
   */
  protected List<Particle> createInitialParticlesFromObservation(
      double timestamp, OBS observation) {
    return _particleFactory.createParticles(timestamp, observation);
  }

  /***************************************************************************
   * Private Methods
   **************************************************************************/

  /**
   * @return true if this is the initial entry for these particles
   */
  private boolean checkFirst(double timestamp, OBS observation) {

    if (!_seenFirst) {
      _particles = createInitialParticlesFromObservation(timestamp, observation);
      _seenFirst = true;
      _timeOfLastUpdate = timestamp;
      return true;
    }

    return false;
  }

  /**
   * This runs a single time-step of the particle filter, given a single
   * timestep's worth of sensor readings. Julie's note to self: see
   * KLDParticleFilter's implementation for hints on what existing functions you
   * can use.
   * 
   * @param firstTime
   */
  private void runSingleTimeStep(double timestamp, OBS obs,
      boolean moveParticles) throws ParticleFilterException {

    int numberOfParticles = _particles.size();
    List<Particle> particles = _particles;

    /**
     * 1. apply the motion model to each particle
     */
    if (moveParticles) {
      particles = new ArrayList<Particle>(numberOfParticles);
      double elapsed = timestamp - _timeOfLastUpdate;
      for (int i = 0; i < _particles.size(); i++)
        _motionModel.move(_particles.get(i), timestamp, elapsed, obs, particles);
    }

    /**
     * 2. apply the sensor model(likelihood) to each particle
     */
    CategoricalDist<Particle> cdf = applySensorModel(particles, obs);

    /**
     * 3. track the weighted particles (before resampling and normalization)
     */
    _weightedParticles = particles;

    /**
     * 4. store the most likely particle's information
     */
    double highestWeight = Double.NEGATIVE_INFINITY;
    double lowestWeight = Double.POSITIVE_INFINITY;

    int index = 0;
    for (Particle p : particles) {
      double w = p.getWeight();
      if (w > highestWeight) {
        _mostLikelyParticle = p.cloneParticle();
        highestWeight = w;
      }
      if (w < lowestWeight) {
        _leastLikelyParticle = p.cloneParticle();
        lowestWeight = w;
      }
      p.setIndex(index++);
    }

    if (highestWeight == 0)
      throw new ZeroProbabilityParticleFilterException();

    /**
     * 5. resample (use the CDF of unevenly weighted particles to create an
     * equal number of equally-weighted ones)
     */
    List<Particle> resampled = cdf.sample(numberOfParticles);
    List<Particle> reweighted = new ArrayList<Particle>(resampled.size());
    for (Particle p : resampled) {
      p = p.cloneParticle();
      p.setWeight(1.0 / resampled.size());
      reweighted.add(p);
    }

    _particles = reweighted;
  }

  /**
   * Applies the sensor model (as set by setSensorModel) to each particle in the
   * filter, according to the given observable.
   */
  private CategoricalDist<Particle> applySensorModel(List<Particle> particles, OBS obs)
      throws ParticleFilterException {

    CategoricalDist<Particle> cdf = new CategoricalDist<Particle>();

    for (Particle particle : particles) {
      SensorModelResult result = getParticleLikelihood(particle, obs);
      double likelihood = result.getProbability();

      if (Double.isNaN(likelihood))
        throw new IllegalStateException("NaN likehood");

      particle.setWeight(likelihood);
      particle.setResult(result);

      cdf.put(likelihood, particle);

    }

    return cdf;
  }

  private SensorModelResult getParticleLikelihood(Particle particle, OBS obs) {
    return _sensorModel.likelihood(particle, obs);
  }
}
