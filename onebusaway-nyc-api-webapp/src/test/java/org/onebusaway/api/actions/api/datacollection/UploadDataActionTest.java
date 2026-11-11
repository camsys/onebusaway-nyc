/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
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
package org.onebusaway.api.actions.api.datacollection;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Files;
import java.util.Collections;

import org.apache.struts2.dispatcher.multipart.UploadedFile;
import org.apache.struts2.rest.DefaultHttpHeaders;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.onebusaway.api.ResponseCodes;
import org.onebusaway.api.services.DataCollectionService;

/**
 * Tests {@link UploadDataAction}'s Struts 6 file upload handling and id check.
 */
public class UploadDataActionTest {

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Mock
  private DataCollectionService data;

  @InjectMocks
  private UploadDataAction action = new UploadDataAction();

  private File dataDir;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    dataDir = tmp.newFolder("data");
    when(data.getDataDirectory()).thenReturn(dataDir);
  }

  @Test
  public void testUploadIsCopiedToDataDirectory() throws Exception {
    File upload = tmp.newFile("upload.tmp");
    Files.write(upload.toPath(), new byte[] {1, 2, 3});

    action.setId("trace.csv");
    action.withUploadedFiles(Collections.singletonList(upload("upload", upload)));
    DefaultHttpHeaders headers = action.update();

    assertEquals(200, headers.getStatus());
    assertArrayEquals(new byte[] {1, 2, 3},
        Files.readAllBytes(new File(dataDir, "trace.csv").toPath()));
  }

  @Test
  public void testNoUploadWritesEmptyFile() throws Exception {
    action.setId("empty.csv");
    action.update();

    assertEquals(0, new File(dataDir, "empty.csv").length());
  }

  @Test
  public void testOtherFieldsAreIgnored() throws Exception {
    File other = tmp.newFile("other.tmp");
    Files.write(other.toPath(), new byte[] {9});

    action.setId("ignored.csv");
    action.withUploadedFiles(Collections.singletonList(upload("somethingElse", other)));
    action.update();

    assertEquals(0, new File(dataDir, "ignored.csv").length());
  }

  @Test
  public void testPathInIdIsRejected() throws Exception {
    action.setId("../escape.txt");
    DefaultHttpHeaders headers = action.update();

    assertEquals(ResponseCodes.RESPONSE_INVALID_ARGUMENT, headers.getStatus());
    assertFalse(new File(dataDir.getParentFile(), "escape.txt").exists());
  }

  @Test
  public void testIsPlainFileName() {
    assertTrue(UploadDataAction.isPlainFileName("trace.csv"));
    assertTrue(UploadDataAction.isPlainFileName("a..b"));
    assertFalse(UploadDataAction.isPlainFileName(null));
    assertFalse(UploadDataAction.isPlainFileName(""));
    assertFalse(UploadDataAction.isPlainFileName(".."));
    assertFalse(UploadDataAction.isPlainFileName("sub/trace.csv"));
    assertFalse(UploadDataAction.isPlainFileName("sub\\trace.csv"));
  }

  private UploadedFile upload(String inputName, File content) {
    UploadedFile file = mock(UploadedFile.class);
    when(file.getInputName()).thenReturn(inputName);
    when(file.isFile()).thenReturn(true);
    when(file.getContent()).thenReturn(content);
    return file;
  }
}
