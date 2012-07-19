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

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.TraceSupport;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.transit_data_federation.services.AgencyAndIdLibrary;
import org.onebusaway.utility.DateLibrary;

import com.caucho.hessian.client.HessianProxyFactory;

public class SiriIntegrationTestBase {
	  
  private static TraceSupport _traceSupport = new TraceSupport();

  protected VehicleTrackingManagementService _vehicleTrackingManagementService;

  protected VehicleLocationListener _vehicleLocationListener;
  
  private long _maxTimeout = 40 * 1000;

  private String _time = "2012-03-01T13:33:42-0500";
  
  private String _trace;

  public SiriIntegrationTestBase(String trace) {
    _trace = trace;
  }

  public void setTrace(String trace) {
    _trace = trace;
  }

  public void setBundle(String bundleId, String date) throws Exception {
    setBundle(bundleId, DateLibrary.getIso8601StringAsTime(date));
  }

  public void setBundle(String bundleId, Date date) throws Exception {
    String port = System.getProperty(
        "org.onebusaway.transit_data_federation_webapp.port", "8080");

    String url = "http://localhost:" + port
        + "/onebusaway-nyc-vehicle-tracking-webapp/change-bundle.do?bundleId="
        + bundleId + "&time=" + DateLibrary.getTimeAsIso8601String(date);

    HttpClient client = new HttpClient();
    GetMethod get = new GetMethod(url);
    client.executeMethod(get);

    String response = get.getResponseBodyAsString();
    if (!response.equals("OK"))
      throw new Exception("Bundle switch failed!");
  }

  public void reset(String vId) throws Exception {
	  // reset TDS
	  String federationPort = System.getProperty(
			  "org.onebusaway.transit_data_federation_webapp.port", "8080");

	  HessianProxyFactory factory = new HessianProxyFactory();

	  _vehicleLocationListener = (VehicleLocationListener) factory
			  .create(
					  VehicleLocationListener.class,
					  "http://localhost:"
							  + federationPort
							  + "/onebusaway-nyc-vehicle-tracking-webapp/remoting/vehicle-location-listener");

	  _vehicleLocationListener.resetVehicleLocation(AgencyAndIdLibrary.convertFromString(vId));

	  // reset simulator
	  String port = System.getProperty(
			  "org.onebusaway.transit_data_federation_webapp.port", "8080");

	  String url = "http://localhost:" + port
			  + "/onebusaway-nyc-vehicle-tracking-webapp/vehicle-location!reset.do?vehicleId="
			  + vId;

	  HttpClient client = new HttpClient();
	  GetMethod get = new GetMethod(url);
	  client.executeMethod(get);
  }
  
  protected HashMap<String,Object> getVmResponse(String operatorId, String vId) throws IOException, HttpException {

	  HttpClient client = new HttpClient();
	  String port = System.getProperty("org.onebusaway.webapp.port", "8080");
	  String url = "http://localhost:" + port + "/onebusaway-nyc-webapp/api/siri/vehicle-monitoring.json?OperatorRef=" + operatorId + "&VehicleMonitoringDetailLevel=calls&MonitoringRef=" + vId + "&time=" + _time;
	  GetMethod get = new GetMethod(url);
	  client.executeMethod(get);

	  String response = get.getResponseBodyAsString();

	  JsonFactory factory = new JsonFactory(); 
	  ObjectMapper mapper = new ObjectMapper(factory); 
	  TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {}; 
	  HashMap<String,Object> o = mapper.readValue(response, typeRef); 

	  return o;
  }

  protected HashMap<String,Object> getSmResponse(String operatorId, String mRef) throws IOException, HttpException {

	  HttpClient client = new HttpClient();
	  String port = System.getProperty("org.onebusaway.webapp.port", "8080");
	  String url = "http://localhost:" + port + "/onebusaway-nyc-webapp/api/siri/stop-monitoring.json?OperatorRef=" + operatorId + "&StopMonitoringDetailLevel=calls&MonitoringRef=" + mRef + "&time=" + _time;
	  GetMethod get = new GetMethod(url);
	  client.executeMethod(get);

	  String response = get.getResponseBodyAsString();

	  JsonFactory factory = new JsonFactory(); 
	  ObjectMapper mapper = new ObjectMapper(factory); 
	  TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {}; 
	  HashMap<String,Object> o = mapper.readValue(response, typeRef); 

	  return o;
  }
  
  public void loadRecords() throws Throwable {
	  File trace = new File("src/integration-test/resources/traces/" + _trace);
	  List<NycTestInferredLocationRecord> expected = _traceSupport
			  .readRecords(trace);

	  String taskId = _traceSupport.uploadTraceForSimulation(trace);

	  // Wait for the task to complete
	  long t = System.currentTimeMillis();
	  int prevRecordCount = -1;

	  while (true) {
		  List<NycTestInferredLocationRecord> actual = _traceSupport
				  .getSimulationResults(taskId);

		  System.out.println("records=" + actual.size() + "/" + expected.size());

		  if (actual.size() < expected.size()) {

			  if (t + _maxTimeout < System.currentTimeMillis()) {
				  fail("waited but never received enough records: expected="
						  + expected.size() + " actual=" + actual.size());
			  }

			  // We reset our timeout if the record count is growing
			  if (actual.size() > prevRecordCount) {
				  t = System.currentTimeMillis();
				  prevRecordCount = actual.size();
			  }

			  Thread.sleep(1000);
			  continue;
		  }

		  break;
	  }
  }

}
