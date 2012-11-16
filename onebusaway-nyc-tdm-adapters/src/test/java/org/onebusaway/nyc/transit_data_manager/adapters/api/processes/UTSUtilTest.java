package org.onebusaway.nyc.transit_data_manager.adapters.api.processes;

import static org.junit.Assert.*;

import org.junit.Test;

public class UTSUtilTest {

  @Test
  public void testStripChars() {
    UTSUtil u = new UTSUtil();
    assertEquals(null, u.stripLeadingCharacters(null));
    assertEquals("", u.stripLeadingCharacters(""));
    assertEquals("1", u.stripLeadingCharacters("1"));
    assertEquals("", u.stripLeadingCharacters("B"));
    assertEquals("1", u.stripLeadingCharacters("B1"));
    assertEquals("11", u.stripLeadingCharacters("B11"));
  }

}
