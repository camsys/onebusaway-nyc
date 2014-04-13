package org.onebusaway.nyc.admin.util;

import static org.junit.Assert.*;

import org.junit.Test;
import org.onebusaway.nyc.admin.util.NYCFileUtils;

public class FileUtilsTest {

  @Test
  public void testEscapeFilename() {
    assertEquals("google\\ transit\\ brooklyn.zip", NYCFileUtils.escapeFilename("google transit brooklyn.zip"));
  }

}
