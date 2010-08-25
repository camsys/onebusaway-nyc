package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import org.onebusaway.transit_data_federation.impl.ProjectedPointFactory;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.walkplanner.WalkEdgeEntry;

import edu.washington.cs.rse.geospatial.PointVector;

/**
 * Simple bean for holding information about an edge, a point on that edge, and
 * the distance from that point to the observed starting point.
 * 
 * @author bdferris
 */
class EdgeState {

  private final WalkEdgeEntry edge;

  private final ProjectedPoint pointOnEdge;

  private final double distanceAlongEdge;

  public EdgeState(WalkEdgeEntry edge, ProjectedPoint pointOnEdge,
      double distanceAlongEdge) {
    this.edge = edge;
    this.pointOnEdge = pointOnEdge;
    this.distanceAlongEdge = distanceAlongEdge;
  }

  public EdgeState(WalkEdgeEntry edge, ProjectedPoint pointOnEdge) {
    this(edge, pointOnEdge, edge.getNodeFrom().getLocation().distance(
        pointOnEdge));
  }

  public EdgeState(WalkEdgeEntry edge) {
    this.edge = edge;
    this.pointOnEdge = edge.getNodeFrom().getLocation();
    this.distanceAlongEdge = 0.0;
  }

  public WalkEdgeEntry getEdge() {
    return edge;
  }

  public ProjectedPoint getPointOnEdge() {
    return pointOnEdge;
  }

  public double getDistanceAlongEdge() {
    return distanceAlongEdge;
  }

  public EdgeState moveAlongEdge(double remainingDistance) {
    ProjectedPoint from = pointOnEdge;
    ProjectedPoint to = edge.getNodeTo().getLocation();
    PointVector v = new PointVector(to.getX() - from.getX(), to.getY()
        - from.getY());
    v = v.getAsLength(remainingDistance);
    double x = from.getX() + v.getX();
    double y = from.getY() + v.getY();
    ProjectedPoint p = ProjectedPointFactory.reverse(x, y, from.getSrid());
    return new EdgeState(edge, p, distanceAlongEdge + remainingDistance);
  }
}