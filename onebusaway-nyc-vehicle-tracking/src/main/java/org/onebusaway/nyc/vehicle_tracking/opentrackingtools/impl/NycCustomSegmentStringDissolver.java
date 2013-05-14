package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl.NycTrackingGraph.SegmentInfo;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.noding.OrientedCoordinateArray;
import com.vividsolutions.jts.noding.SegmentString;
import com.vividsolutions.jts.noding.SegmentStringDissolver;
import com.vividsolutions.jts.noding.SegmentStringDissolver.SegmentStringMerger;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

public class NycCustomSegmentStringDissolver extends SegmentStringDissolver {
  
  public final static class NycCustomSegmentStringMerger implements
      SegmentStringMerger {
    @Override
    public void merge(SegmentString mergeTarget, SegmentString ssToMerge,
        boolean isSameOrientation) {
  
      final List<NycTrackingGraph.SegmentInfo> newSegmentInfos = Lists.newArrayList();
  
      for (final NycTrackingGraph.SegmentInfo si : (List<NycTrackingGraph.SegmentInfo>) ssToMerge.getData()) {
        newSegmentInfos.add(new NycTrackingGraph.SegmentInfo(si.getShapeId(),
            si.getGeomNum(), si.getIsSameOrientation().equals(
                new Boolean(isSameOrientation))));
      }
      newSegmentInfos.addAll((List<NycTrackingGraph.SegmentInfo>) mergeTarget.getData());
      mergeTarget.setData(newSegmentInfos);
    }
  }

  private SegmentStringMerger merger;
  private TreeMap<ScaledOrientedCoordinateArray, SegmentString> ocaMap = Maps.newTreeMap(); 
  private double scale;
  
  public NycCustomSegmentStringDissolver(double scale) {
    this.scale = scale;
    this.merger = null;
  }
  
  public NycCustomSegmentStringDissolver(double scale, SegmentStringMerger merger) {
    this.scale = scale;
    this.merger = merger;
  }

  @Override
  public void dissolve(Collection segStrings) {
    for (Iterator i = segStrings.iterator(); i.hasNext(); ) {
      dissolve((SegmentString) i.next());
    }
  }

  @Override
  public void dissolve(SegmentString segString) {
    ScaledOrientedCoordinateArray oca = new ScaledOrientedCoordinateArray(segString.getCoordinates(), scale);
    SegmentString existing = this.ocaMap.get(oca);
    if (existing == null) {
      this.ocaMap.put(oca, segString);
    }
    else {
      if (merger != null) {
        boolean isSameOrientation
            = CoordinateArrays.equals(existing.getCoordinates(), segString.getCoordinates());
        merger.merge(existing, segString, isSameOrientation);
      }
    }

  }

  @Override
  public Collection getDissolved() {
    return this.ocaMap.values();
  }
  
}