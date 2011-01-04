package org.onebusaway.nyc.integration_tests.nyc_webapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class NycWebappIndexPageIntegrationTest extends NycWebappTestSupport {
  @Test
  public void test() {

    // Open the webpage in selenium
    open("/index.action");

    // verify that the header is coming through
    String header = "A City Transit Agency";
    assertTrue(isTextPresent(header));
    assertEquals(header, getText("xpath=//div[@id='header']/h1"));
    
    // verify that the default search stub text appears on the page
    String findText = getAttribute("xpath=//form[@action='search.action']/fieldset/input[2]@value");
    assertEquals("Find",findText);
  }
}
