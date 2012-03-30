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
package org.onebusaway.nyc.geocoder.impl;

import org.onebusaway.geocoder.model.GeocoderResults;
import org.onebusaway.geocoder.services.GeocoderService;
import org.onebusaway.nyc.geocoder.service.NycGeocoderResult;
import org.onebusaway.nyc.geocoder.service.NycGeocoderService;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

import org.geotools.geometry.jts.JTSFactoryFinder;

import java.util.ArrayList;
import java.util.List;

public abstract class FilteredGeocoderBase implements NycGeocoderService, GeocoderService {

  private static GeometryFactory _geometryFactory = JTSFactoryFinder.getGeometryFactory(null);

  private Polygon _wktFilterPolygon = null;
  
  public void setWktFilterPolygon(String wkt) throws ParseException {
    WKTReader reader = new WKTReader();
    _wktFilterPolygon = (Polygon)reader.read(wkt);
  }

  // (method to make legacy OBA components that use the geocoder happy...)
  public GeocoderResults geocode(String location) {
    return null;
  }
  
  public abstract List<NycGeocoderResult> nycGeocode(String location);

  protected List<NycGeocoderResult> filterResultsByWktPolygon(List<NycGeocoderResult> input) {
    if(_wktFilterPolygon == null) {
      return input;
    }
    
    List<NycGeocoderResult> output = new ArrayList<NycGeocoderResult>();
    for(NycGeocoderResult result : input) {
      Coordinate coordinate = new Coordinate(result.getLongitude(), result.getLatitude());
      Geometry point = _geometryFactory.createPoint(coordinate);
      
      if(_wktFilterPolygon.intersects(point)) {
        output.add(result);
      }
    }

    return output;
  }

}
