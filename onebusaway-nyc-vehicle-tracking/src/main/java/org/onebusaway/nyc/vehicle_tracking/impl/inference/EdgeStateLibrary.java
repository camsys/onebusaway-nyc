package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.CDFMap;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Gaussian;
import org.onebusaway.transit_data_federation.impl.walkplanner.StreetGraphLibrary;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;
import org.onebusaway.transit_data_federation.services.walkplanner.WalkEdgeEntry;
import org.onebusaway.transit_data_federation.services.walkplanner.WalkNodeEntry;
import org.onebusaway.transit_data_federation.services.walkplanner.WalkPlannerGraph;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EdgeStateLibrary {

  private WalkPlannerGraph _streetGraph;

  private double _initialSearchRadius = 200;

  private double _maxSearchRadius = 800;

  private Gaussian _edgeProbability = new Gaussian(0, 20);

  @Autowired
  public void setStreetGraph(WalkPlannerGraph streetGraph) {
    _streetGraph = streetGraph;
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

  public void setEdgeProbabilitySigma(double edgeProbabilitySigma) {
    _edgeProbability = new Gaussian(0, edgeProbabilitySigma);
  }

  public CDFMap<EdgeState> calculatePotentialEdgeStates(ProjectedPoint location) {

    Collection<WalkNodeEntry> nodes = determineNearbyStreetNodes(location);

    List<EdgeState> potentialEdgeLocations = new ArrayList<EdgeState>();

    determinePotentialStreetEdges(location, nodes, potentialEdgeLocations);

    if (potentialEdgeLocations.isEmpty())
      throw new IllegalStateException(
          "no nearby edges for the specified location: " + location);

    return constructCDFForPotentialEdges(potentialEdgeLocations, location);
  }

  /****
   * Private Methods
   ****/

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
      List<EdgeState> potentialEdgeLocations, ProjectedPoint point) {

    CDFMap<EdgeState> cdf = new CDFMap<EdgeState>();

    for (EdgeState edgeLocation : potentialEdgeLocations) {
      double d = point.distance(edgeLocation.getPointOnEdge());
      double p = _edgeProbability.getProbability(d);
      cdf.put(p, edgeLocation);
    }

    return cdf;
  }
}
