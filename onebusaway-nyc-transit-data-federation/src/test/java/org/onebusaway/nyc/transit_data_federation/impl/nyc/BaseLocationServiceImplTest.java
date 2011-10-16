package org.onebusaway.nyc.transit_data_federation.impl.nyc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import org.onebusaway.csv_entities.exceptions.CsvEntityIOException;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.nyc.transit_data_federation.bundle.model.NycFederatedTransitDataBundle;

public class BaseLocationServiceImplTest {

  @Test
  public void test() throws CsvEntityIOException, IOException {

    NycFederatedTransitDataBundle bundle = new NycFederatedTransitDataBundle();
    File path = new File("src/test/resources/example-bundle");
    bundle.setPath(path);

    BaseLocationServiceImpl service = new BaseLocationServiceImpl();
    service.setBundle(bundle);

    service.setup();

    assertEquals("Jackie Gleason",
        service.getBaseNameForLocation(new CoordinatePoint(40.651089,
            -74.001405)));
    assertNull(service.getTerminalNameForLocation(new CoordinatePoint(
        40.651089, -74.001405)));

    assertNull(service.getBaseNameForLocation(new CoordinatePoint(40.612025,
        -74.035552)));
    assertEquals("Bay Ridge",
        service.getTerminalNameForLocation(new CoordinatePoint(40.612025,
            -74.035552)));

    assertNull(service.getBaseNameForLocation(new CoordinatePoint(40.691782,
        -74.000192)));
    assertEquals("Cobble Hill",
        service.getTerminalNameForLocation(new CoordinatePoint(40.691782,
            -74.000192)));

    assertNull(service.getBaseNameForLocation(new CoordinatePoint(40.646290,
        -74.008958)));
    assertNull(service.getTerminalNameForLocation(new CoordinatePoint(
        40.646290, -74.008958)));

    assertNull(service.getBaseNameForLocation(new CoordinatePoint(40.612044,
        -74.036096)));
    assertNull(service.getTerminalNameForLocation(new CoordinatePoint(
        40.612044, -74.036096)));
  }
}