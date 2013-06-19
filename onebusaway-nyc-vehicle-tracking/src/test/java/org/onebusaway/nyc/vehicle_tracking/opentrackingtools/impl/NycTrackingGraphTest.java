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
import com.vividsolutions.jts.noding.IntersectionAdder;
import com.vividsolutions.jts.noding.MCIndexNoder;
import com.vividsolutions.jts.noding.NodedSegmentString;
import com.vividsolutions.jts.noding.SegmentStringDissolver;
import com.vividsolutions.jts.noding.snapround.MCIndexSnapRounder;
import com.vividsolutions.jts.noding.snapround.SimpleSnapRounder;
import com.vividsolutions.jts.operation.linemerge.LineMergeGraph;
import com.vividsolutions.jts.operation.linemerge.LineMerger;
import com.vividsolutions.jts.operation.linemerge.LineSequencer;
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
    
    WKTReader parser = new WKTReader();//new GeometryFactory(pm));
    
    final String line1 = "LINESTRING (0 3, 50 51, 100 102.5, 100 200, 100 102.5)";
    final String line2 = "LINESTRING (0 0, 100 100, 200 100)";
    final String line3 = "LINESTRING (100 200, 100 100, 0 0)";
    
    final LineString l1 = (LineString) parser.read(line1); 
    final LineString l2 = (LineString) parser.read(line2); 
    final LineString l3 = (LineString) parser.read(line3); 
    List<NodedSegmentString> segmentStrings = Lists.newArrayList(
        new NodedSegmentString(l1.getCoordinates(), null),
        new NodedSegmentString(l2.getCoordinates(), null),
        new NodedSegmentString(l3.getCoordinates(), null)
        );
    
//    GeometryCollection gc = JTSFactoryFinder.getGeometryFactory().createGeometryCollection(
//        new Geometry[] {l1, l2, l3});
//    Geometry snappedGeoms = GeometrySnapper.snapToSelf(gc, 10d, true);
//    Geometry simpleGeoms = DouglasPeuckerSimplifier.simplify(snappedGeoms, 10d);
    
//    final MCIndexNoder gn = new MCIndexNoder();
//    LineIntersector li = new RobustLineIntersector();
//    li.setPrecisionModel(JTSFactoryFinder.getGeometryFactory().getPrecisionModel());
//    gn.setSegmentIntersector(new IntersectionAdder(li));
//    gn.computeNodes(segmentStrings);
    SimpleSnapRounder gn = new SimpleSnapRounder(new PrecisionModel(1d/5d));
    gn.computeNodes(segmentStrings);
    
    final List<LineString> result = Lists.newArrayList();
    for (final Object obj : gn.getNodedSubstrings()) {
      final NodedSegmentString ns1 = (NodedSegmentString) obj;
      result.add(JTSFactoryFinder.getGeometryFactory().createLineString(ns1.getCoordinates()));
    }
    
    GeometrySnapper gs = new GeometrySnapper(JTSFactoryFinder.getGeometryFactory().createGeometryCollection(
        result.toArray(new Geometry[result.size()])));
    Geometry snappedResult = gs.snapToSelf(7d, true);
    List<NodedSegmentString> snappedSegmentStrings = Lists.newArrayList();
    for (int i = 0; i < snappedResult.getNumGeometries(); i++) {
      snappedSegmentStrings.add(new NodedSegmentString(snappedResult.getGeometryN(i).getCoordinates(), null));
    }
    
    final NycCustomSegmentStringDissolver dissolver = new NycCustomSegmentStringDissolver();
    dissolver.dissolve(snappedSegmentStrings);
    
    SegmentStringDissolver ssd = new SegmentStringDissolver();
    ssd.dissolve(snappedSegmentStrings);
    for (final Object obj : ssd.getDissolved()) {
      final NodedSegmentString ns1 = (NodedSegmentString) obj;
      System.out.println("snappedDissolved:" + ns1);
    }
    
    

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
