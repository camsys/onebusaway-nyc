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

package org.onebusaway.nyc.transit_data_manager.api.sourceData;

import static org.junit.Assert.*;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onebusaway.nyc.util.impl.FileUtility;

public class DscManualUploadDateTimestampFilePickerTest {

  private File tmpDir;
  private DscManualUploadDateTimestampFilePicker picker;

  @Before
  public void setUp() throws Exception {
    String tmpDirStr = System.getProperty("java.io.tmpdir") + File.separator
        + "tmp" + System.currentTimeMillis();
    boolean created = new File(tmpDirStr).mkdir();
    assertTrue(created);
    tmpDir = new File(tmpDirStr);
    picker = new DscManualUploadDateTimestampFilePicker(
        tmpDir.getAbsolutePath());
  }

  @After
  public void tearDown() throws Exception {
    org.apache.commons.io.FileUtils.deleteDirectory(tmpDir);
  }

  @Test
  public void testGetMostRecentSourceFile() throws Exception {
    // no suffix check
    String simpleFileName = tmpDir.getAbsoluteFile() + File.separator
        + "dsc.csv";
    File expectedFile = new File(simpleFileName);

    FileUtils.writeStringToFile(expectedFile, "data");

    assertTrue(expectedFile.exists());
    File actualFile = picker.getMostRecentSourceFile();
    assertNull(actualFile);

    // create a single suffix
    String suffixFilename = tmpDir.getAbsoluteFile() + File.separator
        + "dsc_20130319.csv";
    expectedFile = new File(suffixFilename);
    FileUtils.writeStringToFile(expectedFile, "data");

    assertTrue(expectedFile.exists());
    actualFile = picker.getMostRecentSourceFile();
    assertEquals(expectedFile, actualFile);

    // create a newer suffix
    suffixFilename = tmpDir.getAbsoluteFile() + File.separator
        + "dsc_20130320.csv";
    expectedFile = new File(suffixFilename);
    FileUtils.writeStringToFile(expectedFile, "data");

    assertTrue(expectedFile.exists());
    actualFile = picker.getMostRecentSourceFile();
    assertEquals(expectedFile, actualFile);

    // create an older suffix
    suffixFilename = tmpDir.getAbsoluteFile() + File.separator
        + "dsc_20130318.csv";
    File notExpectedFile = new File(suffixFilename);
    FileUtils.writeStringToFile(notExpectedFile, "data");

    assertTrue(notExpectedFile.exists());
    actualFile = picker.getMostRecentSourceFile();
    assertEquals(expectedFile, actualFile);
  }

}
