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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.siri.model.Siri;
import org.onebusaway.utility.DateLibrary;

import com.caucho.hessian.client.HessianProxyFactory;
import com.thoughtworks.xstream.XStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Before;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.util.Date;

public class SiriIntegrationTestBase {

  /**
   * July 7, 2010 - 11:00 am in NYC
   */
  protected String _timeString = "2010-07-07T11:30:00-04:00";

  protected VehicleLocationListener _vehicleLocationListener;

  protected Date _time;

  protected AgencyAndId _vehicleId = new AgencyAndId("2008", "4444");

  public SiriIntegrationTestBase() {
    super();
  }

  @Before
  public void before() throws ParseException, MalformedURLException {
    
    _time = DateLibrary.getIso8601StringAsTime(_timeString);
    
    String federationPort = System.getProperty("org.onebusaway.transit_data_federation_webapp.port","9905");
    HessianProxyFactory factory = new HessianProxyFactory();
    
    // Connect the VehicleLocationListener for directly injecting location records
    _vehicleLocationListener = (VehicleLocationListener) factory.create(VehicleLocationListener.class, "http://localhost:" + federationPort + "/onebusaway-nyc-vehicle-tracking-webapp/remoting/vehicle-location-listener");
    
    // Reset any location records between test runs
    _vehicleLocationListener.resetVehicleLocation(_vehicleId);
  }

  protected Siri getResponse(String query) throws IOException, HttpException {
  
    HttpClient client = new HttpClient();
    String port = System.getProperty("org.onebusaway.webapp.port", "9000");
    String url = "http://localhost:" + port + "/onebusaway-api-webapp/siri/"
        + query;
    GetMethod get = new GetMethod(url);
    client.executeMethod(get);
  
    String response = get.getResponseBodyAsString();
    assertTrue(response, response.startsWith("<Siri"));
  
    XStream xstream = new XStream();
    xstream.processAnnotations(Siri.class);
    Siri siri = (Siri) xstream.fromXML(response);
    assertNotNull(siri);
    return siri;
  }

}