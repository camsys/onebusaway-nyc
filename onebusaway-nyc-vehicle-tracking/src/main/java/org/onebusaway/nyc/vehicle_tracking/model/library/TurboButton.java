/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.nyc.vehicle_tracking.model.library;

import static org.apache.commons.math.util.FastMath.atan2;
import static org.apache.commons.math.util.FastMath.cos;
import static org.apache.commons.math.util.FastMath.sin;
import static org.apache.commons.math.util.FastMath.sqrt;
import static org.apache.commons.math.util.FastMath.toRadians;

import org.onebusaway.geospatial.model.CoordinatePoint;

public class TurboButton {

  public static final double RADIUS_OF_EARTH_IN_KM = 6371.01;

  public static final double COS_MAX_LAT = Math.cos(46 * Math.PI / 180);

  public static final double METERS_PER_DEGREE_AT_EQUATOR = 111319.9;

  public static final double distance(double lat1, double lon1, double lat2,
      double lon2) {
    return distance(lat1, lon1, lat2, lon2, RADIUS_OF_EARTH_IN_KM * 1000);
  }

  public static final double distance(CoordinatePoint a, CoordinatePoint b) {
    return distance(a.getLat(), a.getLon(), b.getLat(), b.getLon());
  }

  public static final double distance(double lat1, double lon1, double lat2,
      double lon2, double radius) {

    // http://en.wikipedia.org/wiki/Great-circle_distance
    lat1 = toRadians(lat1); // Theta-s
    lon1 = toRadians(lon1); // Lambda-s
    lat2 = toRadians(lat2); // Theta-f
    lon2 = toRadians(lon2); // Lambda-f

    double deltaLon = lon2 - lon1;

    double y = sqrt(p2(cos(lat2) * sin(deltaLon))
        + p2(cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)));
    double x = sin(lat1) * sin(lat2) + cos(lat1) * cos(lat2) * cos(deltaLon);

    return radius * atan2(y, x);
  }

  /****
   * Private Methods
   ****/

  private static final double p2(double a) {
    return a * a;
  }
}