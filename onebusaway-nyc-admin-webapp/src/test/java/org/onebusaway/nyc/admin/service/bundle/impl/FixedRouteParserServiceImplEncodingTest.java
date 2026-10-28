/**
 * Copyright (C) 2026 Metropolitan Transportation Authority
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

package org.onebusaway.nyc.admin.service.bundle.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.onebusaway.nyc.admin.model.ui.DataValidationMode;
import org.onebusaway.nyc.admin.model.ui.DataValidationRouteCounts;

/**
 * Characterization test for how {@link FixedRouteParserServiceImpl} decodes
 * non-ASCII route names and headsigns in a fixed route validation CSV.
 *
 * parseRouteReportFile opens the CSV with {@code new FileReader(File)}, which
 * uses the JVM default charset. Up to Java 17 that default comes from the OS
 * locale; from Java 18 on (JEP 400) it is always UTF-8. This test pins the
 * Java 11 baseline on a UTF-8 host: a UTF-8 report decodes to the correct
 * accented text.
 */
public class FixedRouteParserServiceImplEncodingTest {

  // Unicode escapes keep the expected values independent of the source file encoding.
  private static final String ROUTE_NAME = "Caf\u00e9 \u00d1and\u00fa";
  private static final String HEADSIGN = "CAF\u00c9 \u00d1AND\u00da via 5 AV";

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void testUtf8RouteNameAndHeadsignDecodeUnchanged() throws Exception {
    // The code under test decodes with the JVM default charset, so on a non-UTF-8
    // Java 11 host the expected text cannot match. Skip there instead of failing.
    assumeTrue("needs a UTF-8 default charset, got " + Charset.defaultCharset(),
        StandardCharsets.UTF_8.equals(Charset.defaultCharset()));
    String csv = "Mode,Route,Headsign,Direction,Stops,Weekday,Saturday,Sunday\n"
        + "LOCAL,M1-" + ROUTE_NAME + "," + HEADSIGN + ",N,25,10,8,6\n";
    File csvFile = tmp.newFile("fixed_route_report.csv");
    Files.write(csvFile.toPath(), csv.getBytes(StandardCharsets.UTF_8));

    List<DataValidationMode> modes =
        new FixedRouteParserServiceImpl().parseRouteReportFile(csvFile);

    String charsetInfo = "default charset=" + Charset.defaultCharset();
    assertEquals(charsetInfo, 1, modes.size());
    DataValidationRouteCounts route = modes.get(0).getRoutes().first();
    assertEquals(charsetInfo, "M1", route.getRouteNum());
    assertEquals(charsetInfo, ROUTE_NAME, route.getRouteName());
    assertEquals(charsetInfo, HEADSIGN,
        route.getHeadsignCounts().first().getHeadsign());
  }
}
