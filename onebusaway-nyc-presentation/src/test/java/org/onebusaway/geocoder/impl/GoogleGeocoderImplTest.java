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
package org.onebusaway.geocoder.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.onebusaway.geocoder.model.GeocoderResult;
import org.onebusaway.geocoder.model.GeocoderResults;

import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

public class GoogleGeocoderImplTest {

  @Test
  public void testZipCodeSearch() {

    GoogleGeocoderImpl geocoder = new GoogleGeocoderImpl();

    GeocoderResults results = geocoder.geocode("10009");

    List<GeocoderResult> records = results.getResults();

    assertEquals(1, records.size());

    GeocoderResult result = records.get(0);

    assertEquals(40.7275043, result.getLatitude(), 0.1);
    assertEquals(-73.9800645, result.getLongitude(), 0.1);
    assertEquals("",result.getAddress());
    assertEquals("New York", result.getCity());
    assertEquals("NY", result.getAdministrativeArea());
    assertEquals("US", result.getCountry());
  }
  
  @Test
  public void testBoroughSearch() {

    GoogleGeocoderImpl geocoder = new GoogleGeocoderImpl();

    GeocoderResults results = geocoder.geocode("Staten Island");

    List<GeocoderResult> records = results.getResults();

    assertTrue(records.size() > 0);    

//    GeocoderResult result = records.get(0);
//
//    assertEquals(40.5795317, result.getLatitude(), 0.1);
//    assertEquals(-74.1502007, result.getLongitude(), 0.1);
//    assertEquals("",result.getAddress());
//    assertTrue(result.getCity() == null || result.getCity().equals("New York"));
//    assertEquals("NY", result.getAdministrativeArea());
//    assertEquals("US", result.getCountry());
  }
  
  @Test
  @Ignore
  public void testAmbiguousIntersectionSearch() {

    GoogleGeocoderImpl geocoder = new GoogleGeocoderImpl();

    GeocoderResults results = geocoder.geocode("Atlantic and Hoyt Street");

    List<GeocoderResult> records = results.getResults();

    assertTrue(records.size() > 1);
  }
  
  @Test
  public void testUnAmbiguousIntersectionSearch() {

    GoogleGeocoderImpl geocoder = new GoogleGeocoderImpl();

    GeocoderResults results = geocoder.geocode("Atlantic and Hoyt Street, Brooklyn, NY");

    List<GeocoderResult> records = results.getResults();

    assertEquals(1, records.size());
    
    GeocoderResult result = records.get(0);

    assertEquals(40.6877758, result.getLatitude(), 0.1);
    assertEquals(-73.9869853, result.getLongitude(), 0.1);
    assertEquals("",result.getAddress());
    //assertEquals("New York", result.getCity()); 20140228 Google drops city results?
    assertEquals("NY", result.getAdministrativeArea());
    assertEquals("US", result.getCountry());
  }
}
