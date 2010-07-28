package org.onebusaway.nyc.integration_tests.nyc_webapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HelloWorldIntegrationTest extends NycWebappTestSupport {
  @Test
  public void test() {

    // Open the webpage in selenium
    open("/index.html");

    // Verify that text is present
    assertTrue(isTextPresent("Hello World"));

    // Verify that text is present at a particular DOM location
    assertEquals("Hello World", getText("xpath=html/body/h2"));
  }
}
