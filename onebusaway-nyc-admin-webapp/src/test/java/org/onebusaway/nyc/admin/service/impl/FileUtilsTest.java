package org.onebusaway.nyc.admin.service.impl;

import static org.junit.Assert.*;

import org.junit.Test;

public class FileUtilsTest {

  @Test
  public void testEscapeFilename() {
    assertEquals("google\\ transit\\ brooklyn.zip", FileUtils.escapeFilename("google transit brooklyn.zip"));
  }

}
