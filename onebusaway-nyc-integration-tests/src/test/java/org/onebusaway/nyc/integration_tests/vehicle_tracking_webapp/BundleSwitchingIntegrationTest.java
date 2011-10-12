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
package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp;

import static org.junit.Assert.assertEquals;

import org.onebusaway.utility.DateLibrary;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.util.Date;

public class BundleSwitchingIntegrationTest {

  public void setBundle(String bundleId, String date) throws Exception {
    setBundle(bundleId, DateLibrary.getIso8601StringAsTime(date));
  }
  
  public void setBundle(String bundleId, Date date) throws Exception {
    String port = System.getProperty("org.onebusaway.transit_data_federation_webapp.port", "9905");
    String url = "http://localhost:" + port + 
        "/onebusaway-nyc-vehicle-tracking-webapp/change-bundle.do?bundleId=" + bundleId 
        + "&time=" + DateLibrary.getTimeAsIso8601String(date);
    
    HttpClient client = new HttpClient();
    GetMethod get = new GetMethod(url);
    client.executeMethod(get);

    String response = get.getResponseBodyAsString(); 
    if(!response.equals("OK"))
      throw new Exception("Bundle switch failed!");
  }
  
  @Test
  public void wrongBundleTest() throws Throwable {
    setBundle("s54", "2011-10-10T00:00:00EDT");

    Result result = JUnitCore.runClasses(BundleSwitchingIntegrationTest_TestTrace1.class,
        BundleSwitchingIntegrationTest_TestTrace2.class);

    // all tests should fail!
    assertEquals(result.getFailureCount(), result.getRunCount());
  }

  @Test
  public void correctBundleButAfterAtLeastOneSwitchTest() throws Throwable {
    setBundle("s54", "2011-10-10T00:00:00EDT");
    setBundle("b63-winter10", "2010-12-20T00:00:00EDT");

    Result result = JUnitCore.runClasses(BundleSwitchingIntegrationTest_TestTrace1.class,
        BundleSwitchingIntegrationTest_TestTrace2.class);
    
    // all tests should pass!
    assertEquals(result.getFailureCount(), 0);
  }
}
