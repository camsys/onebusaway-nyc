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
  public void updateFilter(double timestamp, double timeReceived,
      OBS observation) {

    boolean firstTime = checkFirst(timestamp, timeReceived, observation);
    runSingleTimeStep(timestamp, observation, !firstTime);
    _timeOfLastUpdate = timeReceived;
  }

  /**
   * This provides a way of updating the filter even when no new observations
   * have been seen.
   */
  public void updateFilterWithoutObservation(double timestamp) throws Exception {

    checkFirst(timestamp, timestamp, null);
    double elapsed = timestamp - _timeOfLastUpdate;
    for (int i = 0; i < _particles.size(); i++) {
      Particle updatedParticle = _motionModel.move(_particles.get(i),
          timestamp, elapsed, null);
      _particles.set(i, updatedParticle);
    }
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
  private boolean checkFirst(double timestamp, double timeReceived,
      OBS observation) {

    if (!_seenFirst) {
      _particles = createInitialParticlesFromObservation(timestamp, observation);
      _seenFirst = true;
      _timeOfLastUpdate = timeReceived;
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
      boolean moveParticles) {

    /**
     * 1. apply the motion model to each particle
     */
    if (moveParticles) {
      double elapsed = timestamp - _timeOfLastUpdate;
      for (int i = 0; i < _particles.size(); i++) {
        Particle updatedParticle = _motionModel.move(_particles.get(i),
            timestamp, elapsed, obs);
        _particles.set(i, updatedParticle);
      }
    }

    /**
     * 2. apply the sensor model to each particle
     */
    CDF cdf = applySensorModel(obs);

    /**
     * 3. track the weighted particles (before resampling and normalization)
     */
    _weightedParticles = _particles;

    /**
     * 4. store the most likely particle's information
     */
    double highestWeight = Double.NEGATIVE_INFINITY;
    double lowestWeight = Double.POSITIVE_INFINITY;

    for (Particle p : _particles) {
      double w = p.getWeight();
      if (w > highestWeight) {
        _mostLikelyParticle = p.cloneParticle();
        highestWeight = w;
      }
      if (w < lowestWeight) {
        _leastLikelyParticle = p.cloneParticle();
        lowestWeight = w;
      }
    }

    if (highestWeight == 0)
      throw new IllegalStateException("Error: All particles have zero weight!");

    /**
     * 5. resample (use the CDF of unevenly weighted particles to create an
     * equal number of equally-weighted ones)
     */
    List<Particle> resampled = cdf.getClonesOfHeavilyWeightedEntries(
        CDF.RANDOM, _particles, _particles.size());

    for (Particle p : resampled)
      p.setWeight(1.0 / _particles.size());

    _particles = resampled;
  }

  /**
   * Applies the sensor model (as set by setSensorModel) to each particle in the
   * filter, according to the given observable.
   */
  private CDF applySensorModel(OBS obs) {
    int count = _particles.size();
    double accumulate[] = new double[count];
    int index[] = new int[count];
    int mostRecentIndex = CDF.INVALID_INDEX;
    double sum = 0.0, curr;

    int i = 0;

    int numParticlesWithLikelihood = 0;
    for (Particle p : _particles) {
      curr = getParticleLikelihood(p, obs);
      if (Double.isNaN(curr)) {
        curr = 0.00000000001;
      } else {
        numParticlesWithLikelihood++;
      }
      p.setWeight(curr);

      sum += curr;
      accumulate[i] = sum;
      if (curr > 0.0) {
        mostRecentIndex = i;
        index[i] = i;
      } else {
        index[i] = mostRecentIndex;
      }
      i++;
    }

    if (mostRecentIndex == CDF.INVALID_INDEX) {
      String message = "Empty CDF Found: No particle has any likelihood! " + i;
      throw new IllegalArgumentException(message);
    }

    return new CDF(accumulate, index, sum);
  }

  private double getParticleLikelihood(Particle particle, OBS obs) {
    return _sensorModel.likelihood(particle, obs);
  }

  /**
   * Takes the distribution currently represented by the particles, and
   * resamples this distribution until 'n' new samples are created, where 'n' is
   * the number of particles currently running in the filter. Overwrites the old
   * set of particles with this new set.
   */
  protected void gatherSameNumberOfSamples(OBS obs, boolean isMoving) {
    if (_particles.size() == 0) {
      throw new IllegalStateException(
          "Can't gather same number of particles when you have none");
    }
    int numClones = _particles.size();
    CDF cdf = applySensorModel(obs);
    _particles = cdf.getClonesOfHeavilyWeightedEntries(CDF.RANDOM, _particles,
        numClones);
  }
}
