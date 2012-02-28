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

import org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions.CategoricalDist;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.realtime.api.EVehiclePhase;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Ordering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

  public class SensorModelParticleResult {
    public Particle _particle;
    public SensorModelResult _result;

    SensorModelParticleResult(Particle particle, SensorModelResult result) {
      _particle = particle;
      _result = result;
    }

  }

  /**
   * Flag for operations that keep particle trajectory information, etc.
   */
  final private static boolean _debugEnabled = false;

  /**
   * Flag for option to use the maximum likelihood particle as reported result.
   */
  final private static boolean _maxLikelihoodParticle = false;

  /**
   * Flag for option to perform move and likelihood operation in the same
   * thread.
   */
  final private static boolean _moveAndWeight = true;

  /**
   * Flag for random number generation operations that provide reproducibility .
   */
  private static boolean _testingEnabled = true;

  private int _threads = 1;

  // Runtime.getRuntime().availableProcessors();

  public static boolean getDebugEnabled() {
    return _debugEnabled;
  }

  public static boolean getTestingEnabled() {
    return _testingEnabled;
  }

  private ExecutorService _executorService;

  volatile private Particle _leastLikelyParticle;

  volatile private Particle _mostLikelyParticle;

  private MotionModel<OBS> _motionModel = null;

  private ParticleFactory<OBS> _particleFactory;

  private Multiset<Particle> _particles = HashMultiset.create(200);

  private boolean _seenFirst;

  private SensorModel<OBS> _sensorModel;

  private final Map<Entry<VehicleState, VehicleState>, SensorModelResult> _sensorModelResultCache = 
      new ConcurrentHashMap<Entry<VehicleState, VehicleState>, SensorModelResult>();

  private final Multimap<VehicleState, VehicleState> _stateTransitionCache = HashMultimap.create();

  private double _timeOfLastUpdate = 0L;

  private Multiset<Particle> _weightedParticles = HashMultiset.create();

  @SuppressWarnings("unused")
  public ParticleFilter(ParticleFactory<OBS> particleFactory,
      SensorModel<OBS> sensorModel, MotionModel<OBS> motionModel) {
    if (_threads > 1) {
      _executorService = Executors.newFixedThreadPool(_threads);
    }

    _particleFactory = particleFactory;
    _sensorModel = sensorModel;
    _motionModel = motionModel;
  }

  public ParticleFilter(ParticleFilterModel<OBS> model) {
    this(model.getParticleFactory(), model.getSensorModel(),
        model.getMotionModel());
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
    _particles = HashMultiset.create();
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
      double timestamp, OBS observation) {
    return _particleFactory.createParticles(timestamp, observation);
  }

  private Multiset<Particle> applyMotionAndSensorModel(final OBS obs,
      final double timestamp, final CategoricalDist<Particle> cdf)
      throws ParticleFilterException {
    final double elapsed = timestamp - _timeOfLastUpdate;
    final Multiset<Particle> results = ConcurrentHashMultiset.create();
    final Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
    for (final Multiset.Entry<Particle> pEntry : _particles.entrySet()) {
      tasks.add(Executors.callable(new Runnable() {
        class LocalMoveCache extends ThreadLocal<Multiset<Particle>> {
          @Override
          protected Multiset<Particle> initialValue() {
            return HashMultiset.create();
          }
        }

        ThreadLocal<Multiset<Particle>> threadLocal = new LocalMoveCache();

        @Override
        public void run() {
          final Multiset<Particle> moveCache = threadLocal.get();
          cachedParticleMove(pEntry, timestamp, elapsed, obs,
              moveCache);
          for (final Multiset.Entry<Particle> pEntry : moveCache.entrySet()) {
            final Particle p = pEntry.getElement();
            results.add(p);
            final SensorModelResult smr = getCachedParticleLikelihood(p, obs);
            final double likelihood = smr.getProbability() * pEntry.getCount();
            if (Double.isNaN(likelihood))
              throw new IllegalStateException("NaN likehood");
            p.setWeight(likelihood);
            p.setResult(smr);
          }
          moveCache.clear();
        }
      }));
    }
    try {
      _executorService.invokeAll(tasks);

      for (final Multiset.Entry<Particle> pEntry : results.entrySet()) {
        final Particle particle = pEntry.getElement();
        final double likelihood = particle.getResult().getProbability()
            * pEntry.getCount();
        cdf.put(likelihood, particle);
      }
    } catch (final InterruptedException e) {
      throw new ParticleFilterException(
          "particle motion & sensor apply task interrupted: " + e.getMessage());
    }
    _stateTransitionCache.clear();
    _sensorModelResultCache.clear();
    final Multiset<Particle> particles = HashMultiset.create(results);
    return particles;
  }

  @SuppressWarnings("unused")
  private Multiset<Particle> applyMotionModel(final OBS obs,
      final double timestamp) throws ParticleFilterException {
    Multiset<Particle> particles;
    final double elapsed = timestamp - _timeOfLastUpdate;
    if (_threads > 1) {
      final Multiset<Particle> results = ConcurrentHashMultiset.create();
      final Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
      for (final Multiset.Entry<Particle> pEntry : _particles.entrySet()) {
        tasks.add(Executors.callable(new Runnable() {
          @Override
          public void run() {
            cachedParticleMove(pEntry, timestamp, elapsed, obs,
                results);
          }
        }));
      }
      _stateTransitionCache.clear();
      try {
        _executorService.invokeAll(tasks);
      } catch (final InterruptedException e) {
        throw new ParticleFilterException(
            "particle sensor apply task interrupted: " + e.getMessage());
      }
      particles = HashMultiset.create(results);
    } else {
      particles = HashMultiset.create(_particles.entrySet().size());
      for (final Multiset.Entry<Particle> pEntry : _particles.entrySet()) {
        cachedParticleMove(pEntry, timestamp, elapsed, obs,
            particles);
      }
      _stateTransitionCache.clear();
    }
    return particles;
  }

  private static final Ordering<Particle> _nullBlockStateComparator = new Ordering<Particle>() {

    @Override
    public int compare(Particle o1, Particle o2) {
      final VehicleState v1 = o1.getData();
      final VehicleState v2 = o2.getData();

      if (v1.getBlockState() == null) {
        if (v2.getBlockState() != null)
          return 1;
        else
          return 0;
      } else if (v2.getBlockState() == null) {
        return -1;
      }

      if (!EVehiclePhase.isActiveDuringBlock(v1.getJourneyState().getPhase())) {
        if (EVehiclePhase.isActiveDuringBlock(v2.getJourneyState().getPhase()))
          return 1;
      } else if (!EVehiclePhase.isActiveDuringBlock(v2.getJourneyState().getPhase())) {
        return -1;
      }

      return 0;
    }

  };

  /**
   * Applies the sensor model (as set by setSensorModel) to each particle in the
   * filter, according to the given observable.
   */
  @SuppressWarnings("unused")
  private CategoricalDist<Particle> applySensorModel(
      Multiset<Particle> particles, final OBS obs)
      throws ParticleFilterException {

    final CategoricalDist<Particle> cdf = new CategoricalDist<Particle>();

    /*
     * Sort particles in block-state "null order". Used to determine maximum
     * probabilities for non-null states.
     */
    final List<Particle> nullSortedParticles = _nullBlockStateComparator.immutableSortedCopy(particles.elementSet());

    if (_threads > 1) {

      final Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
      final Collection<SensorModelParticleResult> results = new ConcurrentLinkedQueue<SensorModelParticleResult>();
      for (final Particle p : nullSortedParticles) {
        tasks.add(Executors.callable(new Runnable() {
          @Override
          public void run() {
            results.add(new SensorModelParticleResult(p,
                getCachedParticleLikelihood(p, obs)));
          }
        }));
      }
      _sensorModelResultCache.clear();

      try {
        _executorService.invokeAll(tasks);
        for (final SensorModelParticleResult res : results) {
          final Particle particle = res._particle;
          final SensorModelResult result = res._result;
          final double likelihood = result.getProbability()
              * particles.count(particle);

          if (Double.isNaN(likelihood))
            throw new IllegalStateException("NaN likehood");

          particle.setWeight(likelihood);
          particle.setResult(result);

          cdf.put(likelihood, particle);
        }
      } catch (final InterruptedException e) {
        throw new ParticleFilterException(
            "particle sensor apply task interrupted: " + e.getMessage());
      }
    } else {
      for (final Particle particle : nullSortedParticles) {
        final SensorModelResult result = getCachedParticleLikelihood(particle,
            obs);

//        if (result == null)
//          continue;

        final double likelihood = result.getProbability()
            * particles.count(particle);

        if (Double.isNaN(likelihood))
          throw new IllegalStateException("NaN likehood");

        particle.setWeight(likelihood);
        particle.setResult(result);

        cdf.put(likelihood, particle);
      }
      _sensorModelResultCache.clear();
    }

    return cdf;
  }

  /**
   * XXX remember to clear the local cache after using this. FIXME again,
   * assumes particles' data are VehicleState type.
   * 
   * @param particle
   * @param timestamp
   * @param elapsed
   * @param obs
   * @param particles
   */
  private void cachedParticleMove(Multiset.Entry<Particle> particle, final double timestamp,
      final double elapsed, final OBS obs, Multiset<Particle> particles) {
    _motionModel.move(particle, timestamp, elapsed, obs, particles,
        _stateTransitionCache);
  }

  /**
   * @return true if this is the initial entry for these particles
   */
  private boolean checkFirst(double timestamp, OBS observation) {

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

        final double w = p.getWeight() * pEntry.getCount();

        if (w == 0.0)
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

  /**
   * XXX Only returns new results, with already performed calculations signified
   * by a null return result. TODO Returning null is kinda lame.
   * 
   * @param particle
   * @param obs
   * @return
   */
  private SensorModelResult getCachedParticleLikelihood(Particle particle,
      OBS obs) {
    return _sensorModel.likelihood(particle, obs, _sensorModelResultCache);
  }

  @SuppressWarnings("unused")
  private SensorModelResult getParticleLikelihood(Particle particle, OBS obs) {
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
  @SuppressWarnings("unused")
  private void runSingleTimeStep(double timestamp, OBS obs,
      boolean moveParticles) throws ParticleFilterException {

    final int numberOfParticles = _particles.size();
    Multiset<Particle> particles = _particles;

    /**
     * 1. apply the motion model to each particle 2. apply the sensor
     * model(likelihood) to each particle
     */
    CategoricalDist<Particle> cdf;
    if (_moveAndWeight && _threads > 1) {

      if (moveParticles) {
        cdf = new CategoricalDist<Particle>();
        particles.addAll(applyMotionAndSensorModel(obs, timestamp, cdf));
      } else {
        cdf = applySensorModel(particles, obs);
      }

    } else {

      /*
       * perform movement and weighing separately
       */
      if (moveParticles) {
        particles = applyMotionModel(obs, timestamp);
      }

      cdf = applySensorModel(particles, obs);
    }

    /**
     * 3. track the weighted particles (before resampling and normalization)
     */
    _weightedParticles = particles;

    /**
     * 4. store the most likely particle's information
     */

    if (!cdf.canSample())
      throw new ZeroProbabilityParticleFilterException();

    computeBestState(particles);

    /**
     * 5. resample (use the CDF of unevenly weighted particles to create an
     * equal number of equally-weighted ones)
     */
    final Multiset<Particle> resampled = cdf.sample(numberOfParticles);
    // TODO why create a new multiset?
    final Multiset<Particle> reweighted = HashMultiset.create(resampled.size());
    for (final Multiset.Entry<Particle> pEntry : resampled.entrySet()) {
      final Particle p = pEntry.getElement().cloneParticle();
      p.setWeight(((double) pEntry.getCount()) / resampled.size());
      reweighted.add(p, pEntry.getCount());
    }

    _particles = reweighted;
  }

  public int getThreads() {
    return _threads;
  }

  public void setThreads(int threads) {
    _threads = threads;
    if (_threads > 1) {
      if (_executorService != null)
        _executorService.shutdownNow();
      _executorService = Executors.newFixedThreadPool(_threads);
    } else
      _executorService = null;
  }
}
