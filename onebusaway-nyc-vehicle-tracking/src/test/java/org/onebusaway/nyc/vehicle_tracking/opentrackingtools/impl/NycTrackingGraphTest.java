package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.noding.MCIndexNoder;
import com.vividsolutions.jts.noding.NodedSegmentString;
import com.vividsolutions.jts.noding.SegmentStringDissolver;
import com.vividsolutions.jts.noding.snapround.MCIndexSnapRounder;
import com.vividsolutions.jts.noding.snapround.SimpleSnapRounder;

import org.geotools.factory.Hints;
import org.geotools.geometry.GeometryBuilder;
import org.geotools.geometry.GeometryFactoryFinder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.geometry.text.WKTParser;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeocentricCRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.GeographicCRS;
import org.opentrackingtools.util.GeoUtils;
import org.testng.collections.Lists;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NycTrackingGraphTest {

  @Test
  public void testIntersector1() {
    final MCIndexNoder noder = new MCIndexNoder();
    noder.setSegmentIntersector(new NycCustomIntersectionAdder(
        new RobustLineIntersector()));
    final LineString l1 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 0), new Coordinate(0, 1000)});
    final LineString l2 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 200), new Coordinate(0, 700)});
    final LineString l3 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 800), new Coordinate(0, 100)});
    final LineString l4 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {
            new Coordinate(0, 100), new Coordinate(0, 400), new Coordinate(100, 100)});
    final LineString l5 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {
            new Coordinate(0, 350), new Coordinate(0, 400), new Coordinate(-100, 100)});
    final LineString l6 = JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 0), new Coordinate(0, 1050)});
    noder.computeNodes(Lists.newArrayList(
        new NodedSegmentString(l1.getCoordinates(), null),
        new NodedSegmentString(l2.getCoordinates(), null),
        new NodedSegmentString(l3.getCoordinates(), null),
        new NodedSegmentString(l4.getCoordinates(), null),
        new NodedSegmentString(l5.getCoordinates(), null),
        new NodedSegmentString(l6.getCoordinates(), null)));

    final SegmentStringDissolver dissolver = new SegmentStringDissolver();
    dissolver.dissolve(noder.getNodedSubstrings());
    final List<LineString> result = Lists.newArrayList();
    for (final Object obj : dissolver.getDissolved()) {
      final NodedSegmentString ns1 = (NodedSegmentString) obj;
      result.add(JTSFactoryFinder.getGeometryFactory().createLineString(ns1.getCoordinates()));
      System.out.println(ns1);
    }

    final Set<LineString> expectedResult = Sets.newHashSet();
    expectedResult.addAll(Lists.newArrayList(    
        JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 0), new Coordinate(0, 100)}),
        JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 100), new Coordinate(0, 200)}),
        JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 200), new Coordinate(0, 350)}),
        JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 400), new Coordinate(-100, 100)}),
        JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 350), new Coordinate(0, 400)}),
        JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 400), new Coordinate(0, 700)}),
        JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 800), new Coordinate(0, 100)}),
        JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 700), new Coordinate(0, 1000)}),
        JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 1000), new Coordinate(0, 1050)}),
        JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 400), new Coordinate(100, 100)})));

    Set<LineString> diff = Sets.symmetricDifference(expectedResult, Sets.newHashSet(result));
    assertTrue(diff.isEmpty());
  }

  @Test
  public void testIntersector2() throws FactoryException, ParseException, com.vividsolutions.jts.io.ParseException {
    final MCIndexNoder noder = new MCIndexNoder();
    noder.setSegmentIntersector(new NycCustomIntersectionAdder(
        new RobustLineIntersector()));
    
//    final double scale = 1d / 7d; // work within a 7m grid
//    final GeometryFactory gf = new GeometryFactory(new PrecisionModel(scale));
    WKTReader parser = new WKTReader();//gf);
    
    final String line1 = "LINESTRING (564991 4484291, 564823 4484221, 563444 4483843, 563430 4483843, 563346 4484151, 563367 4484333, 563360 4484368, 563444 4484389, 564074 4484641, 564368 4484823, 564452 4484886, 564571 4485005, 564753 4485131, 564949 4485306, 565096 4485481, 565166 4485600, 565222 4485649, 565327 4485670, 565502 4485670, 565600 4485600, 565726 4485565, 565782 4485572, 565894 4485628, 566293 4485873, 566356 4486062, 566440 4486209, 566433 4486454, 566363 4486650, 566825 4486825, 566818 4486916, 566622 4486839, 566412 4486727, 565969.8 4486559.1)";
    final String line2 = "LINESTRING (566344.8 4486701.5, 566412 4486727)";
    
    final LineString l1 = (LineString) parser.read(line1); 
    final LineString l2 = (LineString) parser.read(line2); 
    List<NodedSegmentString> segmentStrings = Lists.newArrayList(
        new NodedSegmentString(l1.getCoordinates(), null),
        new NodedSegmentString(l2.getCoordinates(), null)
        );
    noder.computeNodes(segmentStrings);
    
    final NycCustomSegmentStringDissolver dissolver = new NycCustomSegmentStringDissolver(1d);
    dissolver.dissolve(noder.getNodedSubstrings());
    final List<LineString> result = Lists.newArrayList();
    for (final Object obj : dissolver.getDissolved()) {
      final NodedSegmentString ns1 = (NodedSegmentString) obj;
      System.out.println(ns1);
      if (!ns1.isClosed())
        result.add(JTSFactoryFinder.getGeometryFactory().createLineString(ns1.getCoordinates()));
    }

    final Set<LineString> expectedResult = Sets.newHashSet();
    expectedResult.addAll(Lists.newArrayList(    
        JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 0), new Coordinate(0, 100)}),
        JTSFactoryFinder.getGeometryFactory().createLineString(
        new Coordinate[] {new Coordinate(0, 100), new Coordinate(0, 200)})
        ));

    Set<LineString> diff = Sets.symmetricDifference(expectedResult, Sets.newHashSet(result));
    assertTrue(diff.isEmpty());
  }
}
