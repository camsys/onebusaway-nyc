package org.onebusaway.nyc.vehicle_tracking.opentrackingtools.impl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateArrays;

import org.geotools.graph.util.geom.Coordinate2D;

public class ScaledOrientedCoordinateArray
    implements Comparable
{
  private Coordinate[] pts;
  private boolean orientation;
  private double scale;

  /**
   * Creates a new {@link OrientedCoordinateArray}
   * for the given {@link Coordinate} array.
   *
   * @param pts the coordinates to orient
   */
  public ScaledOrientedCoordinateArray(Coordinate[] pts, double scale)
  {
    this.scale = scale;
    this.pts = pts;
    orientation = orientation(pts);
  }

  /**
   * Computes the canonical orientation for a coordinate array.
   *
   * @param pts the array to test
   * @return <code>true</code> if the points are oriented forwards
   * @return <code>false</code if the points are oriented in reverse
   */
  private static boolean orientation(Coordinate[] pts)
  {
    return CoordinateArrays.increasingDirection(pts) == 1;
  }

  /**
   * Compares two {@link OrientedCoordinateArray}s for their relative order
   *
   * @return -1 this one is smaller
   * @return 0 the two objects are equal
   * @return 1 this one is greater
   */

  public int compareTo(Object o1) {
    ScaledOrientedCoordinateArray oca = (ScaledOrientedCoordinateArray) o1;
    int comp = compareOriented(pts, orientation,
                               oca.pts, oca.orientation, this.scale);
    return comp;
  }

  private static int compareOriented(Coordinate[] pts1,
                                     boolean orientation1,
                                     Coordinate[] pts2,
                                     boolean orientation2,
                                     double scale)
  {
    int dir1 = orientation1 ? 1 : -1;
    int dir2 = orientation2 ? 1 : -1;
    int limit1 = orientation1 ? pts1.length : -1;
    int limit2 = orientation2 ? pts2.length : -1;

    int i1 = orientation1 ? 0 : pts1.length - 1;
    int i2 = orientation2 ? 0 : pts2.length - 1;
    int comp = 0;
    while (true) {
      int compPt = pts1[i1].distance(pts2[i2]) < 1d/scale ? 0 : pts1[i1].compareTo(pts2[i2]);
      if (compPt != 0)
        return compPt;
      i1 += dir1;
      i2 += dir2;
      boolean done1 = i1 == limit1;
      boolean done2 = i2 == limit2;
      if (done1 && ! done2) return -1;
      if (! done1 && done2) return 1;
      if (done1 && done2) return 0;
    }
  }

  public Coordinate[] getPts() {
    return pts;
  }

  public boolean isOrientation() {
    return orientation;
  }


}