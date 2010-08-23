package org.onebusaway.nyc.integration_tests.nyc_webapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HelloWorldIntegrationTest extends NycWebappTestSupport {
  @Test
  public void test() {

    // Open the webpage in selenium
    open("/index.action");

    // verify that the header is coming through
    String header = "A City Transit Agency";
    assertTrue(isTextPresent(header));
    assertEquals(header, getText("xpath=//div[@id='header']/h1"));

    // verify that the default search stub text appears on the page
    String searchText = "You haven't added any routes to the map yet.";
    assertTrue(isTextPresent(searchText));
    assertEquals(searchText, getText("xpath=//p[@id='no-routes-displayed-message']"));
  }
}
