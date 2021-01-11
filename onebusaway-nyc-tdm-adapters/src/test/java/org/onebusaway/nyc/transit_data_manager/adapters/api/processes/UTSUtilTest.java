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
