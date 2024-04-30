/**
 * Copyright (C) 2011 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.util.impl;


import org.locationtech.jts.geom.Coordinate;

public class DistanceLibrary {
    public static final double RADIUS_OF_EARTH_IN_KM = 6371.01D;
    public static final double RADIUS_OF_EARTH_IN_METERS = 6371010.0D;

    public DistanceLibrary() {
    }


    public static final double distance(Coordinate from, Coordinate to) {
        return distance(from.y, from.x, to.y, to.x);
    }

    public static final double distance(double lat1, double lon1, double lat2, double lon2) {
        return distance(lat1, lon1, lat2, lon2, RADIUS_OF_EARTH_IN_METERS);
    }

    public static final double distance(double lat1, double lon1, double lat2, double lon2, double radius) {
        lat1 = Math.toRadians(lat1);
        lon1 = Math.toRadians(lon1);
        lat2 = Math.toRadians(lat2);
        lon2 = Math.toRadians(lon2);
        double deltaLon = lon2 - lon1;
        double y = Math.sqrt(p2(Math.cos(lat2) * Math.sin(deltaLon)) + p2(Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLon)));
        double x = Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(deltaLon);
        return radius * Math.atan2(y, x);
    }

    private static final double p2(double a) {
        return a * a;
    }

}