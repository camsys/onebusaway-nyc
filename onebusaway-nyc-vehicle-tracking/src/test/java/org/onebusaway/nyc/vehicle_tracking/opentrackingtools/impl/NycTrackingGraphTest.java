package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import static org.junit.Assert.assertTrue;

import com.google.common.collect.Sets;
import com.vividsolutions.jts.algorithm.LineIntersector;
import com.vividsolutions.jts.algorithm.RobustLineIntersector;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.noding.MCIndexNoder;
import com.vividsolutions.jts.noding.NodedSegmentString;
import com.vividsolutions.jts.noding.SegmentStringDissolver;
import com.vividsolutions.jts.noding.snapround.MCIndexSnapRounder;
import com.vividsolutions.jts.noding.snapround.SimpleSnapRounder;
import com.vividsolutions.jts.operation.overlay.snap.GeometrySnapper;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

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
    
//    final PrecisionModel pm = new PrecisionModel(1d/15d);
//    final MCIndexSnapRounder noder = new MCIndexSnapRounder(pm);
//    LineIntersector li = new RobustLineIntersector();
//    li.setPrecisionModel(pm);
//    noder.setSegmentIntersector(new NycCustomIntersectionAdder(li));
    
    WKTReader parser = new WKTReader();//new GeometryFactory(pm));
    
    final String line1 = "LINESTRING (7 1, 101 103)";
    final String line2 = "LINESTRING (2 5, 52 50)";
    final String line3 = "LINESTRING (50 51, 0 0)";
    final String line4 = "LINESTRING (0 105, 95 3, 1 100)";
    final String line5 = "LINESTRING (20 21, 30 35, 42 21)";
    
    final LineString l1 = (LineString) parser.read(line1); 
    final LineString l2 = (LineString) parser.read(line2); 
    final LineString l3 = (LineString) parser.read(line3); 
    final LineString l4 = (LineString) parser.read(line4); 
    final LineString l5 = (LineString) parser.read(line5); 
//    List<NodedSegmentString> segmentStrings = Lists.newArrayList(
//        new NodedSegmentString(l1.getCoordinates(), null),
//        new NodedSegmentString(l2.getCoordinates(), null),
//        new NodedSegmentString(l3.getCoordinates(), null),
//        new NodedSegmentString(l4.getCoordinates(), null)
//        );
//    noder.computeNodes(segmentStrings);
    
    GeometryCollection gc = JTSFactoryFinder.getGeometryFactory().createGeometryCollection(
        new Geometry[] {l1, l2, l3, l4, l5});
    Geometry snappedGeoms = GeometrySnapper.snapToSelf(gc, 10d, true);
    Geometry simpleGeoms = DouglasPeuckerSimplifier.simplify(snappedGeoms, 10d);
    
    for (int i = 0; i < simpleGeoms.getNumGeometries(); i++) {
      System.out.println(simpleGeoms.getGeometryN(i));
    }
    
//    final NycCustomSegmentStringDissolver dissolver = new NycCustomSegmentStringDissolver();
//    dissolver.dissolve(noder.getNodedSubstrings());
//    final List<LineString> result = Lists.newArrayList();
//    for (final Object obj : dissolver.getDissolved()) {
//      final NodedSegmentString ns1 = (NodedSegmentString) obj;
//      System.out.println(ns1);
//      if (!ns1.isClosed())
//        result.add(JTSFactoryFinder.getGeometryFactory().createLineString(ns1.getCoordinates()));
//    }
//
//    final Set<LineString> expectedResult = Sets.newHashSet();
//    expectedResult.addAll(Lists.newArrayList(    
//        JTSFactoryFinder.getGeometryFactory().createLineString(
//        new Coordinate[] {new Coordinate(0, 0), new Coordinate(0, 100)}),
//        JTSFactoryFinder.getGeometryFactory().createLineString(
//        new Coordinate[] {new Coordinate(0, 100), new Coordinate(0, 200)})
//        ));

//    Set<LineString> diff = Sets.symmetricDifference(expectedResult, Sets.newHashSet(result));
//    assertTrue(diff.isEmpty());
  }
}
