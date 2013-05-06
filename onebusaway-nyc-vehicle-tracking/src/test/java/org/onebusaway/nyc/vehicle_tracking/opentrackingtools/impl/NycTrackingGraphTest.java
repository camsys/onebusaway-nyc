package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import static org.junit.Assert.assertTrue;

import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.noding.MCIndexNoder;
import com.vividsolutions.jts.noding.NodedSegmentString;
import com.vividsolutions.jts.noding.SegmentStringDissolver;

import org.geotools.geometry.jts.JTSFactoryFinder;
import org.junit.Test;
import org.testng.collections.Lists;

import java.util.List;

public class NycTrackingGraphTest {

  @Test
  public void testIntersector1() {
    final MCIndexNoder noder = new MCIndexNoder();
    noder.setSegmentIntersector(new NycCustomIntersectionAdder(
        new RobustLineIntersector()));
    final LineString l1 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 0), new Coordinate(0, 10)});
    final LineString l2 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 2), new Coordinate(0, 7)});
    final LineString l3 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 8), new Coordinate(0, 1)});
    final LineString l4 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {
            new Coordinate(0, 1), new Coordinate(0, 4), new Coordinate(1, 1)});
    final LineString l5 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {
            new Coordinate(0, 3.5), new Coordinate(0, 4), new Coordinate(-1, 1)});
    final LineString l6 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 0), new Coordinate(0, 10.5)});
    final LineString l7 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {
            new Coordinate(0, -1), new Coordinate(0, 0), new Coordinate(0, 6)});
    noder.computeNodes(Lists.newArrayList(
        new NodedSegmentString(l1.getCoordinates(), null),
        new NodedSegmentString(l2.getCoordinates(), null),
        new NodedSegmentString(l3.getCoordinates(), null),
        new NodedSegmentString(l4.getCoordinates(), null),
        new NodedSegmentString(l5.getCoordinates(), null),
        new NodedSegmentString(l6.getCoordinates(), null),
        new NodedSegmentString(l7.getCoordinates(), null)));

    final SegmentStringDissolver dissolver = new SegmentStringDissolver();
    dissolver.dissolve(noder.getNodedSubstrings());
    final List<Coordinate[]> result = Lists.newArrayList();
    for (final Object obj : dissolver.getDissolved()) {
      final NodedSegmentString ns1 = (NodedSegmentString) obj;
      result.add(ns1.getCoordinates());
      System.out.println(ns1);
    }

    final List<Coordinate[]> expectedResult = Lists.newArrayList(
        new Coordinate[] {new Coordinate(0, 0), new Coordinate(0, 1)},
        new Coordinate[] {new Coordinate(0, 1), new Coordinate(0, 2)},
        new Coordinate[] {new Coordinate(0, 2), new Coordinate(0, 3.5)},
        new Coordinate[] {new Coordinate(0, 4), new Coordinate(-1, 1)},
        new Coordinate[] {new Coordinate(0, 3.5), new Coordinate(0, 4)},
        new Coordinate[] {new Coordinate(0, 4), new Coordinate(0, 7)},
        new Coordinate[] {new Coordinate(0, 8), new Coordinate(0, 1)},
        new Coordinate[] {new Coordinate(0, 7), new Coordinate(0, 10)},
        new Coordinate[] {new Coordinate(0, 10), new Coordinate(0, 10.5)},
        new Coordinate[] {new Coordinate(0, 4), new Coordinate(1, 1)});

    assertTrue(result.size() == expectedResult.size()
        && result.containsAll(expectedResult));
  }

  // @Test
  public void testIntersector2() {
    final MCIndexNoder noder = new MCIndexNoder();
    noder.setSegmentIntersector(new NycCustomIntersectionAdder(
        new RobustLineIntersector()));
    final LineString l1 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {
            new Coordinate(594340, 4523460), new Coordinate(594390, 4523430),
            new Coordinate(594460, 4523390)});
    final LineString l2 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {
            new Coordinate(594340, 4523460), new Coordinate(594390, 4523430),
            new Coordinate(594475, 4523380)});
    noder.computeNodes(Lists.newArrayList(
        new NodedSegmentString(l1.getCoordinates(), null),
        new NodedSegmentString(l2.getCoordinates(), null)));

    final SegmentStringDissolver dissolver = new SegmentStringDissolver();
    dissolver.dissolve(noder.getNodedSubstrings());
    final List<Coordinate[]> result = Lists.newArrayList();
    for (final Object obj : dissolver.getDissolved()) {
      final NodedSegmentString ns1 = (NodedSegmentString) obj;
      result.add(ns1.getCoordinates());
      System.out.println(ns1);
    }

    final List<Coordinate[]> expectedResult = Lists.newArrayList(
        new Coordinate[] {new Coordinate(0, 0), new Coordinate(0, 1)},
        new Coordinate[] {new Coordinate(0, 1), new Coordinate(0, 2)},
        new Coordinate[] {new Coordinate(0, 2), new Coordinate(0, 3.5)},
        new Coordinate[] {new Coordinate(0, 4), new Coordinate(-1, 1)},
        new Coordinate[] {new Coordinate(0, 3.5), new Coordinate(0, 4)},
        new Coordinate[] {new Coordinate(0, 4), new Coordinate(0, 7)},
        new Coordinate[] {new Coordinate(0, 8), new Coordinate(0, 1)},
        new Coordinate[] {new Coordinate(0, 7), new Coordinate(0, 10)},
        new Coordinate[] {new Coordinate(0, 10), new Coordinate(0, 10.5)},
        new Coordinate[] {new Coordinate(0, 4), new Coordinate(1, 1)});

    assertTrue(result.size() == expectedResult.size()
        && result.containsAll(expectedResult));
  }
}
