package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Gaussian;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.MotionModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.walkplanner.WalkEdgeEntry;
import org.onebusaway.transit_data_federation.services.walkplanner.WalkNodeEntry;

import edu.washington.cs.rse.geospatial.PointVector;

/**
 * Motion model implementation for vehicle location inference.
 * 
 * @author bdferris
 */
public class MotionModelImpl implements MotionModel<Observation> {

  /**
   * Default variance is 10 mph = 4.4704 meters/sec
   */
  private Gaussian _velocity = new Gaussian(0, 4.4704);

  /**
   * Generally speaking, 66% of our samples will be in the range of left-turn
   * through right-turn
   */
  private Gaussian _direction = new Gaussian(0, Math.PI / 2);

  /**
   * 
   * @param velocityVariance meters/sec
   */
  public void setVelocityVariance(double velocityVariance) {
    _velocity = new Gaussian(0, velocityVariance);
  }

  /**
   * 
   * @param directionVariance radians
   */
  public void setDirectionVariance(double directionVariance) {
    _direction = new Gaussian(0, directionVariance);
  }

  @Override
  public Particle move(Particle parent, double timestamp, double timeElapsed,
      Observation obs) {
    
    // Let's move! We need:
    // A velocity

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

    // In meters
    double remainingDistance = straightLineDistance + _velocity.drawSample()
        * ellapsedTime;

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

    VehicleState.Builder state = VehicleState.builder();
    state.setEdgeState(edgeState);

    Particle particle = new Particle(timestamp, parent);
    particle.setData(state.create());
    return particle;
  }

  private PointVector vector(ProjectedPoint a, ProjectedPoint b) {
    return new PointVector(b.getX() - a.getX(), b.getY() - a.getY());
  }
}
