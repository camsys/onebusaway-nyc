package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFactory;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Create initial particles from an initial observation.
 * 
 * The general idea here is that we:
 * 
 * <ul>
 * <li>look for nearby street nodes</li>
 * <li>snap to the edges connected to those nodes</li>
 * <li>sample particles from those edges, as weighted by their distance from our
 * start point</li>
 * </ul>
 * 
 * @author bdferris
 */
public class ParticleFactoryImpl implements ParticleFactory<Observation> {

  private static Logger _log = LoggerFactory.getLogger(ParticleFactoryImpl.class);

  private BlocksFromObservationService _blocksFromObservationService;

  private int _initialNumberOfParticles = 50;

  public double _distanceSamplingFactor = 1.0;

  private EdgeStateLibrary _edgeStateLibrary;

  @Autowired
  public void setEdgeStateLibrary(EdgeStateLibrary edgeStateLibrary) {
    _edgeStateLibrary = edgeStateLibrary;
  }

  @Autowired
  public void setBlocksFromObservationService(
      BlocksFromObservationService blocksFromObservationService) {
    _blocksFromObservationService = blocksFromObservationService;
  }

  public void setInitialNumberOfParticles(int initialNumberOfParticles) {
    _initialNumberOfParticles = initialNumberOfParticles;
  }

  /**
   * When we sample from potential nearby locations, this controls how likely we
   * are to pick a location nearby versus one far-away. Make this number smaller
   * to keep the samples closer to our start locations. A value of 1.0 is a
   * reasonable start.
   * 
   * @param distanceSamplingFactor
   */
  public void setDistanceSamplingFactor(double distanceSamplingFactor) {
    _distanceSamplingFactor = distanceSamplingFactor;
  }

  @Override
  public List<Particle> createParticles(double timestamp, Observation obs) {

    CDFMap<BlockState> blocks = _blocksFromObservationService.determinePotentialBlocksForObservation(obs);

    ProjectedPoint point = obs.getPoint();

    CDFMap<EdgeState> cdf = _edgeStateLibrary.calculatePotentialEdgeStates(point);

    return sampleParticlesFromPotentialEdges(timestamp, cdf, blocks);
  }

  private List<Particle> sampleParticlesFromPotentialEdges(double timestamp,
      CDFMap<EdgeState> cdf, CDFMap<BlockState> blocks) {

    List<Particle> particles = new ArrayList<Particle>(
        _initialNumberOfParticles);

    if (blocks.isEmpty())
      _log.warn("no blocks to sample!");

    for (int i = 0; i < _initialNumberOfParticles; i++) {

      EdgeState edgeLocation = cdf.sample();
      BlockState blockState = null;

      if (!blocks.isEmpty()) {
        blockState = blocks.sample();
      } else {
        blockState = new BlockState(null, null, null);
      }

      VehicleState state = new VehicleState(edgeLocation, blockState);

      Particle p = new Particle(timestamp);
      p.setData(state);
      particles.add(p);
    }

    return particles;
  }

}
