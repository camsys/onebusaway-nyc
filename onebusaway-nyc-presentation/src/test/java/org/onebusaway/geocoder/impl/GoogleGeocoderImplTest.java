package org.onebusaway.geocoder.impl;

import static org.junit.Assert.assertEquals;

import org.onebusaway.geocoder.model.GeocoderResult;
import org.onebusaway.geocoder.model.GeocoderResults;

import org.junit.Test;

import java.util.List;

public class GoogleGeocoderImplTest {

  @Test
  public void testGeocoder() {

    GoogleGeocoderImpl geocoder = new GoogleGeocoderImpl();

    GeocoderResults results = geocoder.geocode("98105");

    List<GeocoderResult> records = results.getResults();

    assertEquals(1, records.size());

    GeocoderResult result = records.get(0);

    assertEquals(47.663355, result.getLatitude(), 0.1);
    assertEquals(-122.298259, result.getLongitude(), 0.1);
    assertEquals("",result.getAddress());
    assertEquals("Seattle", result.getCity());
    assertEquals("WA", result.getAdministrativeArea());
    assertEquals("98105", result.getPostalCode());
    assertEquals("US", result.getCountry());
  }
}
