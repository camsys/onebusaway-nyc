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
package org.onebusaway.nyc.report.impl;
import static org.junit.Assert.*;

import org.onebusaway.nyc.report.model.CcLocationReportRecord;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;


public class CcLocationCacheTest {
  
  @Before
  public void setup() throws IOException {

  }

  @After
  public void teardown() {
  }

  @Test
  public void test() {
    CcLocationCache cache = new CcLocationCache(10);
    assertNull(cache.get("" + 0));
    assertEquals(0, cache.size());
    cache.put(createTestRecord(0));
    assertEquals(1, cache.size());
    CcLocationReportRecord cc = cache.get("" + 0);
    assertNotNull(cc);
    assertEquals("0", cc.getUUID());
    // removing it shrinks the cache
    assertEquals(0, cache.size());
    
    // can not re-retrieve
    assertNull(cache.get("" + 0));
    
    cache.put(createTestRecord(0));
    cache.put(createTestRecord(1));
    cache.put(createTestRecord(2));
    cache.put(createTestRecord(3));
    cache.put(createTestRecord(4));
    cache.put(createTestRecord(5));
    cache.put(createTestRecord(6));
    cache.put(createTestRecord(7));
    cache.put(createTestRecord(8));
    cache.put(createTestRecord(9));
    assertEquals(10, cache.size());
    cache.put(createTestRecord(10));
    assertEquals(10, cache.size());
    assertNull(cache.get("" + 0));
    assertEquals(10, cache.size());
    CcLocationReportRecord cc10 = cache.get("" + 10);
    assertNotNull(cc10);
    assertEquals("10", cc10.getUUID());
  }

  private CcLocationReportRecord createTestRecord(int i) {
    CcLocationReportRecord cc = new CcLocationReportRecord();
    cc.setUUID("" + i);
    return cc;
  }
}