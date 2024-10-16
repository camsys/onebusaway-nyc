/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.nyc.vehicle_tracking.impl.particlefilter;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gov.sandia.cognition.math.LogMath;

import java.util.Collection;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.math.util.FastMath;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ParticleFactoryImpl;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Multisets;

/**
 * Core particle filter implementation.<br>
 * Note: This filter has an experimental multi-threading option that is enabled
 * by setting _threads > 1.
 * 
 * @author bwillard, bdferris
 * 
 * @param <OBS>
 */
public class ParticleFilter<OBS> {

  private static Logger _log = LoggerFactory.getLogger(ParticleFilter.class);

  public class SensorModelParticleResult {
    public Particle _particle;
    public SensorModelResult _result;

    SensorModelParticleResult(Particle particle, SensorModelResult result) {
      _particle = particle;
      _result = result;
    }
  }

  /**
   * Effective sample size, below which resampling will occur.
   */
  final private static double _resampleThreshold = 0.75;

  /**
   * Flag for operations that keep particle trajectory information, transitions,
   * etc.
   */
  private static boolean _debugEnabled = false;

  /**
   * Flag for option to use the maximum likelihood particle as reported result.
   *  (as opposed to the most likely particle within the most likely phase/trip pair)
   */
  final private static boolean _maxLikelihoodParticle = false;

  /**
   * Flag for random number generation operations that provide reproducibility .
   */
  private static boolean _enabledReproducibility = true;

  public static boolean getDebugEnabled() {
    return _debugEnabled;
  }

  public static boolean getReproducibilityEnabled() {
    return _enabledReproducibility;
  }

  volatile private Particle _leastLikelyParticle;

  volatile private Particle _mostLikelyParticle;

  private MotionModel<OBS> _motionModel = null;

  private ParticleFactory<OBS> _particleFactory;

  private Multiset<Particle> _particles = HashMultiset.create(150);

  private boolean _seenFirst;

  private SensorModel<OBS> _sensorModel;

  private double _timeOfLastUpdate = 0L;

  private Multiset<Particle> _weightedParticles = HashMultiset.create();

  private boolean _previouslyResampled;

  public ParticleFilter(ParticleFactory<OBS> particleFactory,
      SensorModel<OBS> sensorModel, MotionModel<OBS> motionModel) {

    _particleFactory = particleFactory;
    _sensorModel = sensorModel;
    _motionModel = motionModel;
    if (_debugEnabled)
      _log.warn("Debug is enabled!  All particle history/information will be kept in memory!");
  }

  public ParticleFilter(ParticleFilterModel<OBS> model) {
    this(model.getParticleFactory(), model.getSensorModel(),
        model.getMotionModel());
  }

  public static void setDebugEnabled(Boolean debugEnabled) {
    _debugEnabled = debugEnabled;
  }

  public Particle getLeastLikelyParticle() {
    return _leastLikelyParticle;
  }

  public Particle getMostLikelyParticle() {
    return _mostLikelyParticle;
  }

  public Multiset<Particle> getParticleList() {
    return Multisets.unmodifiableMultiset(_particles);
  }

  public Multiset<Particle> getSampledParticles() {
    return Multisets.unmodifiableMultiset(_particles);
  }

  public Multiset<Particle> getWeightedParticles() {
    return Multisets.unmodifiableMultiset(_weightedParticles);
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
    _particles.clear();
    _particles.addAll(initialParticles);
    _seenFirst = true;
  }

  public void reset() {
    _particles.clear();
    _particles = HashMultiset.create(200);
    _timeOfLastUpdate = 0L;
    _seenFirst = false;
  }

  /**
   * This is the major method that will be called repeatedly by an outside
   * client in order to drive the filter. Each call runs a single timestep.
   */
  public void updateFilter(double timestamp, OBS observation)
      throws ParticleFilterException {
    final boolean firstTime = checkFirst(timestamp, observation);
    runSingleTimeStep(timestamp, observation, !firstTime);
    _timeOfLastUpdate = timestamp;
  }

  /*
   * Returns an ArrayList, each entry of which is a Particle. This allows
   * subclasses to determine what kind of particles to create, and where to
   * place them.
   */
  protected Multiset<Particle> createInitialParticlesFromObservation(
      double timestamp, OBS observation) throws ParticleFilterException {
    return _particleFactory.createParticles(timestamp, observation);
  }

  public static double getEffectiveSampleSize(Multiset<Particle> particles) 
      throws BadProbabilityParticleFilterException {
    double Wnorm = 0.0;
    for (final Multiset.Entry<Particle> p : particles.entrySet()) {
      final double weight = p.getElement().getWeight();
      Wnorm += weight * p.getCount();
    }

    if (Wnorm == 0)
      return 0d;

    double Wvar = 0.0;
    for (final Multiset.Entry<Particle> p : particles.entrySet()) {
      final double weight = p.getElement().getWeight();
      Wvar += FastMath.pow(weight / Wnorm, 2) * p.getCount();
    }

    if (Double.isInfinite(Wvar) || Double.isNaN(Wvar))
      throw new BadProbabilityParticleFilterException("effective sample size numerical error: Wvar=" + Wvar);
    
    return 1 / Wvar;
  }

  /**
   * @return true if this is the initial entry for these particles
   * @throws ParticleFilterException 
   */
  private boolean checkFirst(double timestamp, OBS observation) throws ParticleFilterException {

    if (!_seenFirst) {
      _particles.addAll(createInitialParticlesFromObservation(timestamp,
          observation));
      _seenFirst = true;
      _timeOfLastUpdate = timestamp;
      return true;
    }

    return false;
  }

  /**
   * Finds the most likely/occurring trip & phase combination among the
   * particles, then chooses the particle with highest likelihood of that pair. <br>
   * 
   * FIXME violates generic particle contract by assuming data is of type
   * VehicleState
   * 
   * @param particles
   */
  private void computeBestState(Multiset<Particle> particles) {
    /**
     * We choose the "most likely" particle over the entire distribution w.r.t
     * the inferred trip.
     */
    final TObjectDoubleMap<String> tripPhaseToProb = new TObjectDoubleHashMap<String>();

    final HashMultimap<String, Particle> particlesIdMap = HashMultimap.create();
    final SortedSet<Particle> bestParticles = new TreeSet<Particle>();
    String bestId = null;

    if (!_maxLikelihoodParticle) {
      double highestTripProb = Double.MIN_VALUE;
      int index = 0;
      for (final Multiset.Entry<Particle> pEntry : particles.entrySet()) {
        final Particle p = pEntry.getElement();
        p.setIndex(index++);

        final double w = FastMath.exp(p.getLogWeight()
            + FastMath.log(pEntry.getCount()));

        if (Double.isInfinite(w))
          continue;

        final VehicleState vs = p.getData();
        final String tripId = vs.getBlockState() == null
            ? "NA"
            : vs.getBlockState().getBlockLocation().getActiveTrip().getTrip().toString();
        final String phase = vs.getJourneyState().toString();
        final String id = tripId + "." + phase;

        final double newProb = tripPhaseToProb.adjustOrPutValue(id, w, w);

        particlesIdMap.put(id, p);

        /**
         * Check most likely new trip & phase pairs, then find the most likely
         * particle(s) within those.
         */
        if (bestId == null || newProb > highestTripProb) {
          bestId = id;
          highestTripProb = newProb;
        }
      }
      bestParticles.addAll(particlesIdMap.get(bestId));
    } else {
      bestParticles.addAll(particles);
    }

    /**
     * after we've found the best trip & phase pair, we choose the highest
     * likelihood particle among those.
     */
    final Particle bestParticle = bestParticles.first();

    _mostLikelyParticle = bestParticle.cloneParticle();

  }

  @SuppressWarnings("unused")
  private SensorModelResult getParticleLikelihood(Particle particle, OBS obs) throws BadProbabilityParticleFilterException {
    return _sensorModel.likelihood(particle, obs);
  }

  /**
   * This runs a single time-step of the particle filter, given a single
   * timestep's worth of sensor readings.
   * 
   * @param timestamp
   * @param obs
   * @param moveParticles
   * @throws ParticleFilterException
   */
  private void runSingleTimeStep(double timestamp, OBS obs,
      boolean moveParticles) throws ParticleFilterException {

    Multiset<Particle> particles = _particles;

    /*
     * perform movement and weighing separately
     */
    if (moveParticles) {
      final double elapsed = timestamp - _timeOfLastUpdate;
      particles = _motionModel.move(_particles, timestamp, elapsed, obs,
          _previouslyResampled);
    }

    /**
     * 3. track the weighted particles (before resampling and normalization)
     */
    _weightedParticles = particles;

    /**
     * 4. store the most likely particle's information
     */
    computeBestState(particles);

    if (getEffectiveSampleSize(particles)
        / ParticleFactoryImpl.getInitialNumberOfParticles() < _resampleThreshold) {

      /**
       * 5. resample
       */
      final Multiset<Particle> resampled = lowVarianceSampler(particles,
          particles.size());

      final Multiset<Particle> reweighted = HashMultiset.create(resampled.size());
      for (final Entry<Particle> pEntry : resampled.entrySet()) {
        final Particle p = pEntry.getElement().cloneParticle();
        p.setWeight(((double) pEntry.getCount()) / resampled.size());
        reweighted.add(p, pEntry.getCount());
      }
      _particles = reweighted;
      _previouslyResampled = true;
    } else {
      _particles = particles;
      _previouslyResampled = false;
    }

  }

  /**
   * Low variance sampler. Follows Thrun's example in Probabilistic Robots.
   * @throws ParticleFilterException 
   */
  public static Multiset<Particle> lowVarianceSampler(
      Multiset<Particle> particles, double M) throws BadProbabilityParticleFilterException {
    Preconditions.checkArgument(particles.size() > 0);
    Preconditions.checkArgument(M > 0);

    final Multiset<Particle> resampled = HashMultiset.create((int) M);
    final double r = ParticleFactoryImpl.getLocalRng().nextDouble() / M;
    final Iterator<Particle> pIter = particles.iterator();
    Particle p = pIter.next();
    double c = p.getLogNormedWeight() - FastMath.log(particles.count(p));
    for (int m = 0; m < M; ++m) {
      final double U = FastMath.log(r + m / M);
      while (U > c && pIter.hasNext()) {
        p = pIter.next();
        c = LogMath.add(
            p.getLogNormedWeight() - FastMath.log(particles.count(p)), c);
      }
      resampled.add(p);
    }

    if (resampled.size() != M)
      throw new BadProbabilityParticleFilterException("low variance sampler did not return a valid sample");
    
    return resampled;
  }

}
