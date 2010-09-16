package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Gaussian;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.ParticleFactory;
import org.onebusaway.transit_data_federation.impl.walkplanner.StreetGraphLibrary;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.walkplanner.WalkEdgeEntry;
import org.onebusaway.transit_data_federation.services.walkplanner.WalkNodeEntry;
import org.onebusaway.transit_data_federation.services.walkplanner.WalkPlannerGraph;
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

  private WalkPlannerGraph _streetGraph;

  private BlocksFromObservationService _blocksFromObservationService;

  private int _initialNumberOfParticles = 50;

  private double _initialSearchRadius = 200;

  private double _maxSearchRadius = 800;

  public double _distanceSamplingFactor = 1.0;

  @Autowired
  public void setStreetGraph(WalkPlannerGraph streetGraph) {
    _streetGraph = streetGraph;
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
   * 
   * @param initialSearchRadius distance, in meters
   */
  public void setInitialSearchRadius(double initialSearchRadius) {
    _initialSearchRadius = initialSearchRadius;
  }

  /**
   * 
   * @param maxSearchRadius distance, in meters
   */
  public void setMaxSearchRadius(double maxSearchRadius) {
    _maxSearchRadius = maxSearchRadius;
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

    Collection<WalkNodeEntry> nodes = determineNearbyStreetNodes(point);

    List<EdgeState> potentialEdgeLocations = new ArrayList<EdgeState>();

    double maxDistance = determinePotentialStreetEdges(point, nodes,
        potentialEdgeLocations);

    if (potentialEdgeLocations.isEmpty())
      throw new IllegalStateException(
          "no nearby edges for the specified observation: " + obs);

    Gaussian g = new Gaussian(0, maxDistance * _distanceSamplingFactor);

    CDFMap<EdgeState> cdf = constructCDFForPotentialEdges(
        potentialEdgeLocations, g, point);

    return sampleParticlesFromPotentialEdges(timestamp, cdf, blocks);
  }

  private Collection<WalkNodeEntry> determineNearbyStreetNodes(
      ProjectedPoint location) {
    Collection<WalkNodeEntry> nodes = StreetGraphLibrary.getNodesNearLocation(
        _streetGraph, location.getLat(), location.getLon(),
        _initialSearchRadius, _maxSearchRadius);

    if (nodes.isEmpty())
      throw new IllegalStateException(
          "no nearby nodes for the specified observation");
    return nodes;
  }

  private double determinePotentialStreetEdges(ProjectedPoint point,
      Collection<WalkNodeEntry> nodes, List<EdgeState> potentialEdgeLocations) {

    double maxDistanceFromObservation = Double.NEGATIVE_INFINITY;

    for (WalkNodeEntry node : nodes) {
      for (WalkEdgeEntry edge : node.getEdges()) {
        ProjectedPoint pointOnEdge = StreetGraphLibrary.computeClosestPointOnEdge(
            edge, point);
        double distanceFromObservation = pointOnEdge.distance(point);
        potentialEdgeLocations.add(new EdgeState(edge, pointOnEdge));
        maxDistanceFromObservation = Math.max(maxDistanceFromObservation,
            distanceFromObservation);
      }
    }

    return maxDistanceFromObservation;
  }

  private CDFMap<EdgeState> constructCDFForPotentialEdges(
      List<EdgeState> potentialEdgeLocations, Gaussian g, ProjectedPoint point) {

    CDFMap<EdgeState> cdf = new CDFMap<EdgeState>();

    for (EdgeState edgeLocation : potentialEdgeLocations) {
      double d = point.distance(edgeLocation.getPointOnEdge());
      double p = g.getProbability(d);
      cdf.put(p, edgeLocation);
    }

    return cdf;
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
