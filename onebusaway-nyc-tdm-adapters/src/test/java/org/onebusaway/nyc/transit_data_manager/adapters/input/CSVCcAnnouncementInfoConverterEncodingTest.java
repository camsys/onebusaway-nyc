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

package org.onebusaway.nyc.transit_data_manager.adapters.input;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import tcip_final_3_0_5_1.CCDestinationSignMessage;

/**
 * Characterization test for how {@link CSVCcAnnouncementInfoConverter} decodes
 * non-ASCII destination sign text.
 *
 * The converter opens the DSC CSV with {@code new FileReader(File)}, which uses
 * the JVM default charset. Up to Java 17 that default comes from the OS locale;
 * from Java 18 on (JEP 400) it is always UTF-8. This test pins the Java 11
 * baseline on a UTF-8 host: a UTF-8 file decodes to the correct accented text.
 * If it fails after the upgrade, or on a host whose locale is not UTF-8, the
 * charset in the failure message shows which default was in effect.
 */
public class CSVCcAnnouncementInfoConverterEncodingTest {

  // Unicode escapes keep the expected value independent of the source file encoding.
  private static final String SIGN_TEXT = "CAF\u00c9 \u00d1AND\u00da via 5 AV";

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Test
  public void testUtf8MessageTextDecodesUnchanged() throws Exception {
    String csv = "\"AGENCY\",\"ROUTE\",\"DEST. SIGN CODE\",\"MESSAGE\",\"DIR\"\n"
        + "\"MTA NYCT\",\"M1\",1010,\"" + SIGN_TEXT + "\",\"N\"\n";
    File csvFile = tmp.newFile("dsc.csv");
    Files.write(csvFile.toPath(), csv.getBytes(StandardCharsets.UTF_8));

    List<CCDestinationSignMessage> messages =
        new CSVCcAnnouncementInfoConverter(csvFile).getDestinationsAsList();

    String charsetInfo = "default charset=" + Charset.defaultCharset();
    assertEquals(charsetInfo, 1, messages.size());
    assertEquals(charsetInfo, SIGN_TEXT, messages.get(0).getMessageText());
    assertEquals(charsetInfo, 1010L, messages.get(0).getMessageID().getMsgID());
  }
}
