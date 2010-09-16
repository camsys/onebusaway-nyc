package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.Random;

import org.onebusaway.geospatial.model.PointVector;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Gaussian;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.walkplanner.WalkEdgeEntry;
import org.onebusaway.transit_data_federation.services.walkplanner.WalkNodeEntry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Motion model implementation for vehicle location inference.
 * 
 * @author bdferris
 */
public class MotionModelImpl implements MotionModel<Observation> {

  private static Random _random = new Random();

  private BlocksFromObservationService _blocksFromObservationService;

  /**
   * Generally speaking, 66% of our samples will be in the range of left-turn
   * through right-turn
   */
  private Gaussian _direction = new Gaussian(0, Math.PI / 2);

  /**
   * Given a potential distance to travel in physical / street-network space, a
   * fudge factor that determines how far ahead we will look along the block of
   * trips in distance to travel
   */
  private double _blockDistanceTravelScale = 1.5;

  /**
   * At a top speed of 70 mph (31.2928 m/s), we want 20 mph (8.9408 m/s) of
   * variance.
   */
  private double _velocitySigmaScalar = 4.4704 / 31.2928;

  @Autowired
  public void setBlocksFromObservationService(
      BlocksFromObservationService blocksFromObservationService) {
    _blocksFromObservationService = blocksFromObservationService;
  }

  public void setVelocitySigmaScalar(double velocitySigmaScalar) {
    _velocitySigmaScalar = velocitySigmaScalar;
  }

  /**
   * 
   * @param directionVariance radians
   */
  public void setDirectionVariance(double directionVariance) {
    _direction = new Gaussian(0, directionVariance);
  }

  public void setBlockDistanceTravelScale(double blockDistanceTravelScale) {
    _blockDistanceTravelScale = blockDistanceTravelScale;
  }

  /****
   * {@link MotionModel} Interface
   ****/

  @Override
  public Particle move(Particle parent, double timestamp, double timeElapsed,
      Observation obs) {

    double distanceToTravel = sampleDistanceToTravel(parent, timestamp, obs);

    EdgeState edgeState = moveParticle(parent, timestamp, obs, distanceToTravel);
    BlockState blockState = determineBlockInstance(parent, timestamp, obs,
        edgeState, distanceToTravel);

    VehicleState state = new VehicleState(edgeState, blockState);

    Particle particle = new Particle(timestamp, parent);
    particle.setData(state);
    return particle;
  }

  private double sampleDistanceToTravel(Particle parent, double timestamp,
      Observation obs) {

    VehicleState parentState = parent.getData();
    EdgeState edgeState = parentState.getEdgeState();
    ProjectedPoint currentLocation = edgeState.getPointOnEdge();

    // Our strategy is to move towards the next gps point (with some
    // probability)
    ProjectedPoint targetPoint = obs.getPoint();

    // In meters
    double straightLineDistance = currentLocation.distance(targetPoint);

    // In seconds
    double ellapsedTime = (timestamp - parent.getTimestamp()) / 1000.0;

    if (ellapsedTime == 0)
      return 0;

    // In meters / second
    double targetVelocity = straightLineDistance / ellapsedTime;

    double targetNoiseVelocity = targetVelocity * _velocitySigmaScalar;

    double sampledNoiseVelocity = _random.nextGaussian() * targetNoiseVelocity;

    // In meters
    return straightLineDistance + sampledNoiseVelocity * ellapsedTime;
  }

  private EdgeState moveParticle(Particle parent, double timestamp,
      Observation obs, double remainingDistance) {

    VehicleState parentState = parent.getData();
    EdgeState edgeState = parentState.getEdgeState();
    ProjectedPoint currentLocation = edgeState.getPointOnEdge();

    // Our strategy is to move towards the next gps point (with some
    // probability)
    ProjectedPoint targetPoint = obs.getPoint();

    while (remainingDistance > 0) {

      double distanceLeftOnEdge = currentLocation.distance(edgeState.getEdge().getNodeTo().getLocation());

      if (remainingDistance < distanceLeftOnEdge) {

        // Just move along the current edge for the remainingDistance
        edgeState = edgeState.moveAlongEdge(remainingDistance);
        remainingDistance = 0;

      } else {

        // Eat up the remaining distance on the edge
        remainingDistance -= distanceLeftOnEdge;

        WalkEdgeEntry currentEdge = edgeState.getEdge();
        WalkNodeEntry node = currentEdge.getNodeTo();

        PointVector targetDirection = vector(node.getLocation(), targetPoint);

        // Pick an outgoing edge, favoring edges that are pointed in the
        // direction we want to go
        CDFMap<WalkEdgeEntry> edgeSampler = new CDFMap<WalkEdgeEntry>();

        for (WalkEdgeEntry edge : node.getEdges()) {
          PointVector possibleDirection = vector(node.getLocation(),
              edge.getNodeTo().getLocation());
          double angle = targetDirection.getAngle(possibleDirection);
          double weight = _direction.getProbability(angle);
          edgeSampler.put(weight, edge);
        }

        // Pick
        WalkEdgeEntry edge = edgeSampler.sample();

        edgeState = new EdgeState(edge);
      }
    }
    return edgeState;
  }

  private BlockState determineBlockInstance(Particle parent, double timestamp,
      Observation obs, EdgeState edgeState, double distanceToTravel) {

    VehicleState parentState = parent.getData();
    BlockState blockState = parentState.getBlockState();
    String currentDsc = blockState.getDestinationSignCode();

    NycVehicleLocationRecord record = obs.getRecord();
    String observedDsc = record.getDestinationSignCode();

    // Really, we want to compare the previously OBSERVED dsc
    if (currentDsc != null && currentDsc.equals(observedDsc)) {
      return _blocksFromObservationService.advanceState(record.getTime(),
          edgeState.getPointOnEdge(), blockState, distanceToTravel
              * _blockDistanceTravelScale);
    }

    CDFMap<BlockState> blocks = _blocksFromObservationService.determinePotentialBlocksForObservation(obs);
    if (blocks.isEmpty())
      return null;
    return blocks.sample();
  }

  private PointVector vector(ProjectedPoint a, ProjectedPoint b) {
    return new PointVector(b.getX() - a.getX(), b.getY() - a.getY());
  }
}
