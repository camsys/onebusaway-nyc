package org.onebusaway.nyc.admin.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.onebusaway.nyc.admin.util.FileUtils;

public class FileUtilsTest {

  @Test
  public void testEscapeFilename() {
    assertEquals("google\\ transit\\ brooklyn.zip", FileUtils.escapeFilename("google transit brooklyn.zip"));
  }

}
