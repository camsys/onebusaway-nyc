package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.Particle;
import org.onebusaway.nyc.vehicle_tracking.model.NycVehicleLocationRecord;
import org.onebusaway.transit_data_federation.impl.ProjectedPointFactory;
import org.onebusaway.transit_data_federation.impl.walkplanner.offline.WalkNodeEntryImpl;
import org.onebusaway.transit_data_federation.impl.walkplanner.offline.WalkPlannerGraphImpl;
import org.onebusaway.transit_data_federation.model.ProjectedPoint;

/**
 * Note that this test is non-deterministic, so there is some slim chance that
 * it will fail if the location distribution swings in an unexpected direction.
 * We try to take a lot of samples to better model the underlying distribution,
 * but it could always happen.
 * 
 * @author bdferris
 * 
 */
public class ParticleFactoryImplTest {

  @Test
  public void blah() {
    
  }
  
  //@Test
  public void test() {

    ParticleFactoryImpl factory = new ParticleFactoryImpl();

    // We create a lot of particles so that variations in particle
    // distribution
    // will average out
    int numberOfParticles = 1000;
    factory.setInitialNumberOfParticles(numberOfParticles);

    factory.setDistanceSamplingFactor(0.5);

    WalkPlannerGraphImpl graph = new WalkPlannerGraphImpl();

    // It's the block around OpenPlan's offices in NYC
    WalkNodeEntryImpl nodeA = createNode(graph, 1, 40.71921714832292,
        -73.9998185634613);
    WalkNodeEntryImpl nodeB = createNode(graph, 2, 40.71955867263016,
        -74.00059103965759);
    WalkNodeEntryImpl nodeC = createNode(graph, 3, 40.72045313274981,
        -73.99985074996948);
    WalkNodeEntryImpl nodeD = createNode(graph, 4, 40.720054692908285,
        -73.9990782737732);

    createEdge(nodeA, nodeB);
    createEdge(nodeB, nodeC);
    createEdge(nodeC, nodeD);
    createEdge(nodeD, nodeA);

    graph.initialize();

    EdgeStateLibrary lib = new EdgeStateLibrary();
    lib.setStreetGraph(graph);
    factory.setEdgeStateLibrary(lib);

    NycVehicleLocationRecord record = new NycVehicleLocationRecord();
    record.setLatitude(40.71951801506643);
    record.setLongitude(-73.99994730949402);
    Observation obs = new Observation(record);

    List<Particle> particles = factory.createParticles(
        System.currentTimeMillis(), obs);

    // We expect four possible points
    ProjectedPoint pA = point(40.71933564808389, -74.0000865897433);
    ProjectedPoint pB = point(40.71975040403674, -74.00043235738961);
    ProjectedPoint pC = point(40.72019281190702, -73.99934605014543);
    ProjectedPoint pD = point(40.719378961656126, -73.99967554154742);
    List<ProjectedPoint> points = Arrays.asList(pA, pB, pC, pD);

    double[] counts = new double[4];

    for (Particle particle : particles) {

      VehicleState state = particle.getData();
      EdgeState edgeState = state.getEdgeState();
      ProjectedPoint p = edgeState.getPointOnEdge();

      int indexOfClosestPoint = getIndexOfClosestPoint(points, p);

      // Should map to one of our target points
      assertTrue(0 <= indexOfClosestPoint && indexOfClosestPoint < 4);

      // The distance should be close
      assertTrue(points.get(indexOfClosestPoint).distance(p) < 1);

      // Count the number of hits at that location
      counts[indexOfClosestPoint]++;
    }

    // Even though we're randomly sampling possible locations on the graph,
    // the
    // relative counts should be stable, especially as we're sampling a large
    // number of particles. The general idea is that locations closer to our
    // starting point should have more samples than other locations.

    for (int i = 0; i < counts.length; i++)
      counts[i] /= numberOfParticles;

    assertEquals(0.365, counts[0], 0.08);
    assertEquals(0.235, counts[1], 0.07);
    assertEquals(0.059, counts[2], 0.06);
    assertEquals(0.342, counts[3], 0.08);
  }

  private ProjectedPoint point(double lat, double lon) {
    ProjectedPoint p = ProjectedPointFactory.forward(lat, lon);
    return p;
  }

  private WalkNodeEntryImpl createNode(WalkPlannerGraphImpl graph, int id,
      double lat, double lon) {
    ProjectedPoint p = point(lat, lon);
    return graph.addNode(id, p);
  }

  private void createEdge(WalkNodeEntryImpl nodeA, WalkNodeEntryImpl nodeB) {
    double d = nodeA.getLocation().distance(nodeB.getLocation());
    nodeA.addEdge(nodeB, d);
  }

  private int getIndexOfClosestPoint(List<ProjectedPoint> points,
      ProjectedPoint p) {

    double minD = Double.POSITIVE_INFINITY;
    int minIndex = -1;
    int index = 0;

    for (ProjectedPoint possiblePoint : points) {
      double d = possiblePoint.distance(p);
      if (d < minD) {
        minD = d;
        minIndex = index;
      }
      index++;
    }

    return minIndex;
  }
}
