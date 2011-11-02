/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.onebusaway.nyc.presentation.impl.sort;

import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.presentation.model.search.LocationResult;
import org.onebusaway.nyc.presentation.model.search.StopResult;
import org.onebusaway.nyc.presentation.service.search.SearchResult;

import com.vividsolutions.jts.geom.Coordinate;

import java.util.Comparator;

public class LocationSensitiveSearchResultComparator implements Comparator<SearchResult> {

  private Coordinate userLocation = null;
  
  public LocationSensitiveSearchResultComparator(CoordinatePoint userLocation) {
    if(userLocation != null)
      this.userLocation = new Coordinate(userLocation.getLat(), userLocation.getLon());
  }
  
  @Override
  public int compare(SearchResult o1, SearchResult o2) {
    if(o1 instanceof LocationResult && o2 instanceof LocationResult) {
      if(userLocation == null)
        return 0;
      
      LocationResult item1 = (LocationResult)o1;
      LocationResult item2 = (LocationResult)o2;

      Coordinate p1 = new Coordinate(item1.getLatitude(), item1.getLongitude());
      Coordinate p2 = new Coordinate(item2.getLatitude(), item2.getLongitude());
      
      Double d1 = p1.distance(userLocation);
      Double d2 = p2.distance(userLocation);
      
      return d1.compareTo(d2);
      
    } else if(o1 instanceof StopResult && o2 instanceof StopResult) {
      if(userLocation == null)
        return 0;

      StopResult item1 = (StopResult)o1;
      StopResult item2 = (StopResult)o2;

      Coordinate p1 = new Coordinate(item1.getLatitude(), item1.getLongitude());
      Coordinate p2 = new Coordinate(item2.getLatitude(), item2.getLongitude());
      
      Double d1 = p1.distance(userLocation);
      Double d2 = p2.distance(userLocation);
      
      return d1.compareTo(d2);

    } else {
      return o1.getName().compareTo(o2.getName());
    }
  }

}
