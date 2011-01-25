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
package org.onebusaway.nyc.vehicle_tracking.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;

import org.junit.Test;
import org.onebusaway.geospatial.model.CoordinatePoint;
import org.onebusaway.gtfs.csv.exceptions.CsvEntityIOException;
import org.onebusaway.nyc.vehicle_tracking.model.NycTransitDataBundle;

public class BaseLocationServiceImplTest {

  @Test
  public void test() throws CsvEntityIOException, IOException {

    NycTransitDataBundle bundle = new NycTransitDataBundle();
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
