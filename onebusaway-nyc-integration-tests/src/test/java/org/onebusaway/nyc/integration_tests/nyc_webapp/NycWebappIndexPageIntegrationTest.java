/**
 * Copyright (c) 2011 Metropolitan Transportation Authority
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
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
    assertEquals(header, getText("xpath=//div[@class='title']/h2"));
    
    // verify that the default search stub text appears on the page
    System.out.println(getText("xpath=//form[@id='search']"));
    
    String findText = getAttribute("xpath=//form[@id='search']/input[@type='submit']@value");
    assertEquals("Find Stop",findText);
  }
}
