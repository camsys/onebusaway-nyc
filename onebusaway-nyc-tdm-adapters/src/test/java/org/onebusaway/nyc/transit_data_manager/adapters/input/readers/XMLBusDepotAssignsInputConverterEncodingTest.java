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
package org.onebusaway.nyc.transit_data_manager.adapters.input.readers;

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
import org.onebusaway.nyc.transit_data_manager.adapters.input.model.MtaBusDepotAssignment;

/**
 * Characterization test for how {@link XMLBusDepotAssignsInputConverter}
 * decodes non-ASCII text in a depot assignment XML file.
 *
 * The converter hands the XML parser a {@code new FileReader(File)}, so the
 * file is decoded with the JVM default charset and the encoding named in the
 * XML declaration is ignored. Up to Java 17 that default comes from the OS
 * locale; from Java 18 on (JEP 400) it is always UTF-8. This test pins the
 * Java 11 baseline on a UTF-8 host. If it fails after the upgrade, or on a
 * host whose locale is not UTF-8, the charset in the failure message shows
 * which default was in effect.
 */
public class XMLBusDepotAssignsInputConverterEncodingTest {

  // Unicode escapes keep the expected value independent of the source file encoding.
  // Upper case only, because the converter upper-cases depots with lower-case letters.
  private static final String DEPOT = "CAFÉ";

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void testUtf8DepotDecodesUnchanged() throws Exception {
    // The code under test decodes with the JVM default charset, so on a non-UTF-8
    // Java 11 host the expected text cannot match. Skip there instead of failing.
    assumeTrue("needs a UTF-8 default charset, got " + Charset.defaultCharset(),
        StandardCharsets.UTF_8.equals(Charset.defaultCharset()));
    String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
        + "<NewDataSet>\n"
        + "  <Table>\n"
        + "    <BUS_NUMBER>1009</BUS_NUMBER>\n"
        + "    <AGENCY>1</AGENCY>\n"
        + "    <DEPOT>" + DEPOT + "</DEPOT>\n"
        + "  </Table>\n"
        + "</NewDataSet>\n";
    File xmlFile = tmp.newFile("depot_assignments.xml");
    Files.write(xmlFile.toPath(), xml.getBytes(StandardCharsets.UTF_8));

    List<MtaBusDepotAssignment> assignments =
        new XMLBusDepotAssignsInputConverter(xmlFile).getBusDepotAssignments();

    String charsetInfo = "default charset=" + Charset.defaultCharset();
    assertEquals(charsetInfo, 1, assignments.size());
    assertEquals(charsetInfo, DEPOT, assignments.get(0).getDepot());
    assertEquals(charsetInfo, 1009, assignments.get(0).getBusNumber());
  }
}
