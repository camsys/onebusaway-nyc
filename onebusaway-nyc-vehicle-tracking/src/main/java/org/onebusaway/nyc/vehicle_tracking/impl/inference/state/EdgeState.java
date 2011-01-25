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
package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.geospatial.model.PointVector;
import org.onebusaway.transit_data_federation.impl.ProjectedPointFactory;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.walkplanner.WalkEdgeEntry;

/**
 * Simple bean for holding information about an edge, a point on that edge, and
 * the distance from that point to the observed starting point.
 * 
 * @author bdferris
 */
public class EdgeState {

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

  public CoordinatePoint getLocationOnEdge() {
    return pointOnEdge.toCoordinatePoint();
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
  
  @Override
  public String toString() {
    return pointOnEdge.getLat() + " " + pointOnEdge.getLon() + " " + edge + " " + distanceAlongEdge;
  }
}