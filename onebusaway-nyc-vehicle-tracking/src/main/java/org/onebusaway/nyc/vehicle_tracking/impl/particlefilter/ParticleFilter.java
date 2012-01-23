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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions.CategoricalDist;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;

import com.google.common.collect.HashMultimap;

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

  /**
   * Callable MotionModel task that operates on chunks of particles.
   * 
   */
  public class ParticleMoveTask implements Callable<List<Particle>> {

    private double _elapsed;
    private int _endIdx;
    private List<Particle> _newParticles;
    private OBS _obs;
    private int _startIdx;
    private double _timestamp;

    ParticleMoveTask(int startIdx, int endIdx, OBS obs, double elapsed,
        double timestamp) {
      _newParticles = new ArrayList<Particle>(endIdx - startIdx);
      _obs = obs;
      _elapsed = elapsed;
      _startIdx = startIdx;
      _endIdx = endIdx;
      _timestamp = timestamp;
    }

    @Override
    public List<Particle> call() {
      for (int i = _startIdx; i < _endIdx; i++) {
        _motionModel.move(_particles.get(i), _timestamp, _elapsed, _obs,
            _newParticles);
      }
      return _newParticles;
    }

  }

  public class SensorModelParticleResult {
    public Particle _particle;
    public SensorModelResult _result;

    SensorModelParticleResult(Particle particle, SensorModelResult result) {
      _particle = particle;
      _result = result;
    }

  }

  /**
   * Callable SensorModel task that operates on chunks of particles.
   * 
   */
  public class SensorModelTask implements
      Callable<List<SensorModelParticleResult>> {

    private int _endIdx;
    private List<Particle> _newParticles;
    private OBS _obs;
    private int _startIdx;

    SensorModelTask(List<Particle> newParticles, int startIdx, int endIdx,
        OBS obs) {
      _newParticles = newParticles;
      _obs = obs;
      _startIdx = startIdx;
      _endIdx = endIdx;
    }

    @Override
    public List<SensorModelParticleResult> call() {
      List<SensorModelParticleResult> results = new ArrayList<SensorModelParticleResult>();
      for (int i = _startIdx; i < _endIdx; i++) {
        Particle p = _newParticles.get(i);
        results.add(new SensorModelParticleResult(p, getParticleLikelihood(p,
            _obs)));
      }
      return results;
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
   * Flag for option to perform move and likelihood operation in the same thread.
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
  
  
  /**
   * Simply returns an integer sequence of step size totalTasks/_threads, within
   * [0, totalTasks-1]
   * 
   * @param totalTasks
   * @return
   */
  @SuppressWarnings("unused")
  private List<Integer[]> getTaskSequence(int totalTasks) {
    int stepSize = Math.max(totalTasks / _threads, 1);
    List<Integer[]> segments = new ArrayList<Integer[]>();
    int runAmt = 0;
    for (int i = 0; i < _threads; ++i) {
      Integer[] interval = new Integer[2];
      interval[0] = runAmt;
      runAmt += stepSize;
      interval[1] = runAmt;
      segments.add(interval);
    }

    /*
     * add remainder of tasks to last group
     */
    if (segments.get(segments.size() - 1)[1] < totalTasks - 1) {
      segments.get(segments.size() - 1)[1] = totalTasks - 1;
    }

    return segments;
  }

  private ExecutorService _executorService;

  private Particle _leastLikelyParticle;

  private Particle _mostLikelyParticle;

  private MotionModel<OBS> _motionModel = null;

  private ParticleFactory<OBS> _particleFactory;

  private List<Particle> _particles = new ArrayList<Particle>();

  private boolean _seenFirst;

  private SensorModel<OBS> _sensorModel;

  private Map<Entry<VehicleState, VehicleState>, SensorModelResult> _sensorModelResultCache = 
      new ConcurrentHashMap<Entry<VehicleState, VehicleState>, SensorModelResult>();

  private Map<VehicleState, Set<VehicleState>> _stateTransitionCache = 
      new ConcurrentHashMap<VehicleState, Set<VehicleState>>();

  private double _timeOfLastUpdate = 0L;

  private List<Particle> _weightedParticles = new ArrayList<Particle>();

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

  public List<Particle> getParticleList() {
    return Collections.unmodifiableList(_particles);
  }

  public List<Particle> getSampledParticles() {
    return Collections.unmodifiableList(_particles);
  }

  /**
   * @return the last time at which updateFilter was called, in milliseconds
   */
  public double getTimeOfLastUpdated() {
    return _timeOfLastUpdate;
  }

  public List<Particle> getWeightedParticles() {
    return Collections.unmodifiableList(_weightedParticles);
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

  public void reset() {
    if (_particles != null) {
      _particles.clear();
      _particles = null;
    }
    _timeOfLastUpdate = 0L;
    _seenFirst = false;
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

  /*
   * Returns an ArrayList, each entry of which is a Particle. This allows
   * subclasses to determine what kind of particles to create, and where to
   * place them.
   */
  protected List<Particle> createInitialParticlesFromObservation(
      double timestamp, OBS observation) {
    return _particleFactory.createParticles(timestamp, observation);
  }
  
  private List<Particle> applyMotionAndSensorModel(final OBS obs,
      final double timestamp, final CategoricalDist<Particle> cdf)
      throws ParticleFilterException {
    final double elapsed = timestamp - _timeOfLastUpdate;
    final Collection<Particle> results = new ConcurrentLinkedQueue<Particle>();
    final Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
    for (final Particle p : _particles) {
      tasks.add(Executors.callable(new Runnable() {
        class LocalMoveCache extends ThreadLocal<Collection<Particle>> {
          @Override
          protected Collection<Particle> initialValue() {
            return new ArrayList<Particle>();
          }
        }

        ThreadLocal<Collection<Particle>> threadLocal = new LocalMoveCache();

        @Override
        public void run() {
          final Collection<Particle> moveCache = threadLocal.get();
          cachedParticleMove(p, timestamp, elapsed, obs, moveCache);
          for (Particle pm : moveCache) {
            results.add(pm);
            SensorModelResult smr = getCachedParticleLikelihood(pm, obs);
            double likelihood = smr.getProbability();
            if (Double.isNaN(likelihood))
              throw new IllegalStateException("NaN likehood");
            pm.setWeight(likelihood);
            pm.setResult(smr);
          }
          moveCache.clear();
          _stateTransitionCache.clear();
          _sensorModelResultCache.clear();
        }
      }));
    }
    try {
      _executorService.invokeAll(tasks);
      for (Particle particle : results) {
        double likelihood = particle.getResult().getProbability();
        cdf.put(likelihood, particle);
      }
    } catch (InterruptedException e) {
      throw new ParticleFilterException(
          "particle motion & sensor apply task interrupted: " + e.getMessage());
    }
    List<Particle> particles = new ArrayList<Particle>(results);
    return particles;
  }
  
  @SuppressWarnings("unused")
  private List<Particle> applyMotionModel(final OBS obs, final double timestamp)
      throws ParticleFilterException {
    List<Particle> particles;
    final double elapsed = timestamp - _timeOfLastUpdate;
    if (_threads > 1) {
      final Collection<Particle> results = new ConcurrentLinkedQueue<Particle>();
      final Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
      for (final Particle p : _particles) {
        tasks.add(Executors.callable(new Runnable() {
          @Override
          public void run() {
            cachedParticleMove(p, timestamp, elapsed, obs, results);
          }
        }));
      }
      _stateTransitionCache.clear();
      try {
        _executorService.invokeAll(tasks);
      } catch (InterruptedException e) {
        throw new ParticleFilterException(
            "particle sensor apply task interrupted: " + e.getMessage());
      }
      particles = new ArrayList<Particle>(results);
    } else {
      particles = new ArrayList<Particle>(_particles.size());
      for (int i = 0; i < _particles.size(); i++) {
        cachedParticleMove(_particles.get(i), timestamp, elapsed, obs, particles);
      }
      _stateTransitionCache.clear();
    }
    return particles;
  }

  /**
   * Applies the sensor model (as set by setSensorModel) to each particle in the
   * filter, according to the given observable.
   */
  @SuppressWarnings("unused")
  private CategoricalDist<Particle> applySensorModel(List<Particle> particles,
      final OBS obs) throws ParticleFilterException {

    CategoricalDist<Particle> cdf = new CategoricalDist<Particle>();

    if (_threads > 1) {

      final Collection<Callable<Object>> tasks = new ArrayList<Callable<Object>>();
      final Collection<SensorModelParticleResult> results = new ConcurrentLinkedQueue<SensorModelParticleResult>();
      for (final Particle p : particles) {
        tasks.add(Executors.callable(new Runnable() {
          @Override
          public void run() {
            results.add(new SensorModelParticleResult(p, getCachedParticleLikelihood(
                p, obs)));
          }
        }));
      }
      _sensorModelResultCache.clear();

      try {
        _executorService.invokeAll(tasks);
        for (SensorModelParticleResult res : results) {
          Particle particle = res._particle;
          SensorModelResult result = res._result;
          double likelihood = result.getProbability();

          if (Double.isNaN(likelihood))
            throw new IllegalStateException("NaN likehood");

          particle.setWeight(likelihood);
          particle.setResult(result);

          cdf.put(likelihood, particle);
        }
      } catch (InterruptedException e) {
        throw new ParticleFilterException(
            "particle sensor apply task interrupted: " + e.getMessage());
      }
    } else {
      for (Particle particle : particles) {
        SensorModelResult result = getCachedParticleLikelihood(particle, obs);
        double likelihood = result.getProbability();

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
   * XXX remember to clear the local cache after using this.
   * FIXME again, assumes particles' data are VehicleState type.
   * 
   * @param particle
   * @param timestamp
   * @param elapsed
   * @param obs
   * @param particles
   */
  private void cachedParticleMove(Particle particle, final double timestamp, 
      final double elapsed, final OBS obs, Collection<Particle> particles) {
    _motionModel.move(particle, timestamp, elapsed, obs, particles, _stateTransitionCache);
  }

  /**
   * @return true if this is the initial entry for these particles
   */
  private boolean checkFirst(double timestamp, OBS observation) {

    if (!_seenFirst) {
      _particles = createInitialParticlesFromObservation(timestamp, observation);
      Collections.sort(_particles);
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
  private void computeBestState(List<Particle> particles) {
    /**
     * We choose the "most likely" particle over the entire distribution w.r.t
     * the inferred trip.
     */
    Map<String, Double> tripPhaseToProb = new HashMap<String, Double>();

    HashMultimap<String, Particle> particlesIdMap = HashMultimap.create();
    SortedSet<Particle> bestParticles = new TreeSet<Particle>();
    String bestId = null;

    if (!_maxLikelihoodParticle) {
      double highestTripProb = Double.MIN_VALUE;
      int index = 0;
      Collections.sort(particles);
      for (Particle p : particles) {
        p.setIndex(index++);

        double w = p.getWeight();

        if (w == 0.0)
          continue;

        VehicleState vs = p.getData();
        String tripId = vs.getBlockState() == null
            ? "NA"
            : vs.getBlockState().getBlockLocation().getActiveTrip().getTrip().toString();
        String phase = vs.getJourneyState().toString();
        String id = tripId + "." + phase;

        Double tripPhaseProb = tripPhaseToProb.get(id);

        double newProb;
        if (tripPhaseProb == null) {
          newProb = w;
        } else {
          newProb = (tripPhaseProb == null ? 0.0 : tripPhaseProb) + w;
        }
        tripPhaseToProb.put(id, newProb);
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
    // Particle bestParticle = null;
    // for (Particle p : bestParticles) {
    // if (bestParticle == null || p.getWeight() > bestParticle.getWeight()) {
    // bestParticle = p;
    // }
    // }
    Particle bestParticle = bestParticles.last();

    _mostLikelyParticle = bestParticle.cloneParticle();

  }
  
  private SensorModelResult getCachedParticleLikelihood(Particle particle, OBS obs) {
    return _sensorModel.likelihood(particle, obs, _sensorModelResultCache);
  }

  private SensorModelResult getParticleLikelihood(Particle particle, OBS obs) {
    return _sensorModel.likelihood(particle, obs);
  }

  /**
   * 
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

    int numberOfParticles = _particles.size();
    List<Particle> particles = _particles;

    /**
     * 1. apply the motion model to each particle 
     * 2. apply the sensor model(likelihood) to each particle
     */
    CategoricalDist<Particle> cdf;
    if (_moveAndWeight && _threads > 1) {

      if (moveParticles) {
        cdf = new CategoricalDist<Particle>();
        particles = applyMotionAndSensorModel(obs, timestamp, cdf);
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
    List<Particle> resampled = cdf.sample(numberOfParticles);
    List<Particle> reweighted = new ArrayList<Particle>(resampled.size());
    for (Particle p : resampled) {
      p = p.cloneParticle();
      p.setWeight(1.0 / resampled.size());
      reweighted.add(p);
    }

    _particles = reweighted;
    Collections.sort(_particles);
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
