package org.onebusaway.nyc.vehicle_tracking.webapp.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeMap;

import com.google.common.collect.Maps;
import com.vividsolutions.jts.geom.CoordinateArrays;
import com.vividsolutions.jts.noding.OrientedCoordinateArray;
import com.vividsolutions.jts.noding.SegmentString;
import com.vividsolutions.jts.noding.SegmentStringDissolver;

public class CustomSegmentStringDissolver extends SegmentStringDissolver {
  
  public final static class CustomSegmentStringMerger implements SegmentStringMerger {

	@Override
    public void merge(SegmentString mergeTarget, SegmentString ssToMerge, boolean isSameOrientation) {
  
      //final List<NycTrackingGraph.SegmentInfo> newSegmentInfos = Lists.newArrayList();
      //final UserData userData = new UserData();
      //for (final NycTrackingGraph.SegmentInfo si : (List<NycTrackingGraph.SegmentInfo>) ssToMerge.getData()) {
      //  newSegmentInfos.add(new NycTrackingGraph.SegmentInfo(si.getShapeId(), si.getGeomNum(), si.getIsSameOrientation().equals(new Boolean(isSameOrientation))));
      //}
      //newSegmentInfos.addAll((List<NycTrackingGraph.SegmentInfo>) mergeTarget.getData());
      //mergeTarget.setData(newSegmentInfos);
    }
  }

  private SegmentStringMerger merger;
  private TreeMap<OrientedCoordinateArray, SegmentString> ocaMap = Maps.newTreeMap(); 
  
  public CustomSegmentStringDissolver() {
    this.merger = null;
  }
  
  public CustomSegmentStringDissolver(SegmentStringMerger merger) {
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
    OrientedCoordinateArray oca = new OrientedCoordinateArray(segString.getCoordinates());
    SegmentString existing = this.ocaMap.get(oca);
    if (existing == null) {
      this.ocaMap.put(oca, segString);
    } else {
      if (merger != null) {
        boolean isSameOrientation = CoordinateArrays.equals(existing.getCoordinates(), segString.getCoordinates());
        merger.merge(existing, segString, isSameOrientation);
      }
    }
  }

  @Override
  public Collection getDissolved() {
    return this.ocaMap.values();
  }
  
}