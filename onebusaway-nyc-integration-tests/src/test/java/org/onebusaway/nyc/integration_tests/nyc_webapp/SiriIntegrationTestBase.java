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
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.onebusaway.nyc.integration_tests.TraceSupport;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.utility.DateLibrary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SiriIntegrationTestBase {
	  
  protected static Logger _log = LoggerFactory.getLogger(SiriIntegrationTestBase.class);
  private static TraceSupport _traceSupport = new TraceSupport();

  protected VehicleLocationListener _vehicleLocationListener;
  
  private long _maxTimeout = 40 * 1000;

  private String _time = "1330626960000";
  
  private Date _date = null;
  
  private String _trace;
  
  private String getNycWebappPort (){
    return System.getProperty("org.onebusaway.webapp.port", "9000");
  }

  public SiriIntegrationTestBase(String trace) {
    _trace = trace;
  }

  public void setTrace(String trace) {
    _trace = trace;
  }

  public void setBundle(String bundleId, String date) throws Exception {
    // TODO this appears to have a timezone issue
    Date javaDate = DateLibrary.getIso8601StringAsTime(date);
    _date = javaDate;
    setBundle(bundleId, javaDate);
  }

  public void setBundle(String bundleId, Date date) throws Exception {
    _date = date;

    String port = System.getProperty(
        "org.onebusaway.transit_data_federation_webapp.port", "9905");

    String url = "http://localhost:" + port
        + "/onebusaway-nyc-vehicle-tracking-webapp/change-bundle.do?bundleId="
        + bundleId + "&time=" + DateLibrary.getTimeAsIso8601String(date);

    String response = getResponseForURL(url);
    if (!response.equals("OK"))
      throw new Exception("Bundle switch failed!");
  }

  public void resetAll() throws Exception {
	  // reset TDS	  
	  String federationPort = System.getProperty(
			  "org.onebusaway.transit_data_federation_webapp.port", "9905");

	  String url = "http://localhost:" + federationPort + "/onebusaway-nyc-vehicle-tracking-webapp/vehicle-location-simulation!cancelAll.do";
	  
	  @SuppressWarnings("unused")
    String r = getResponseForURL(url);
  }
  

  
  protected HashMap<String,Object> getVmResponse(Map<String, String> params) throws IOException {
    String port = getNycWebappPort();
    String url = "http://localhost:" + port + "/onebusaway-nyc-webapp/api/siri/vehicle-monitoring.json?";
    if (!params.containsKey(("key"))) {
        params.put("key", "TEST");
    }
    if (!params.containsKey(("time"))) {
      // TODO this appears to have a timezone issue
      params.put("time", "" + _date.getTime());
  }

    url = appendParamsToUrl(url, params);
    
    String response = getResponseForURL(url); 
    _log.debug(response);
    return getHashMapFromJsonResponse(response);
  }
  
  
  protected HashMap<String,Object> getVmResponse(String operatorId, String vId) throws IOException, HttpException {
  
	  String port = getNycWebappPort();
	  String url = null;
	  if (operatorId == null && vId == null) {
	    // open ended call
	    url = "http://localhost:" + port + "/onebusaway-nyc-webapp/api/siri/vehicle-monitoring.json?key=TEST&VehicleMonitoringDetailLevel=calls&time=" + _time;
	  } else {
	    url = "http://localhost:" + port + "/onebusaway-nyc-webapp/api/siri/vehicle-monitoring.json?key=TEST&OperatorRef=" + operatorId + "&VehicleMonitoringDetailLevel=calls&MonitoringRef=" + vId + "&time=" + _time;
	  }

	  String response = getResponseForURL(url);

	  _log.debug(response); 

	  return getHashMapFromJsonResponse(response);
  }

  protected HashMap<String,Object> getVmV2Response(String operatorId, String vId, String detailLevel) throws IOException, HttpException {
    
    String port = getNycWebappPort();
    String url = null;
    if (operatorId == null && vId == null) {
      // open ended call
      url = "http://localhost:" + port + "/onebusaway-nyc-webapp/api/siri/vehicle-monitoring.json?key=TEST&version=2&VehicleMonitoringDetailLevel="+ detailLevel +"&time=" + _time;
    } else {
      url = "http://localhost:" + port + "/onebusaway-nyc-webapp/api/siri/vehicle-monitoring.json?key=TEST&version=2&OperatorRef=" + operatorId + "&VehicleMonitoringDetailLevel="+ detailLevel +"&MonitoringRef=" + vId + "&time=" + _time;
    }

    String response = getResponseForURL(url);

    _log.debug(response); 

    return getHashMapFromJsonResponse(response);
  }
  
  protected HashMap<String,Object> getSmResponse(String operatorId, String mRef) throws IOException, HttpException {
	  
	  String port = getNycWebappPort();
	  String url = "http://localhost:" + port + "/onebusaway-nyc-webapp/api/siri/stop-monitoring.json?key=TEST&StopMonitoringDetailLevel=calls&MonitoringRef=" + mRef + "&time=" + _time;
	  _log.debug(url);
	  String response = getResponseForURL(url);
	  
	  _log.debug(response); 

	  return getHashMapFromJsonResponse(response);
  }

  protected HashMap<String,Object> getSmV2Response(String operatorId, String mRef, String detailLevel) throws IOException, HttpException {
    
    String port = getNycWebappPort();
    String url = "http://localhost:" + port + "/onebusaway-nyc-webapp/api/siri/stop-monitoring.json?key=TEST&StopMonitoringDetailLevel="+ detailLevel +"&MonitoringRef=" + mRef + "&time=" + _time;
    _log.debug(url);
    String response = getResponseForURL(url);
    
    _log.debug(response); 

    return getHashMapFromJsonResponse(response);
  }
  
  public void loadRecords() throws Throwable {
	  File trace = new File("src/integration-test/resources/traces/" + _trace);
	  List<NycTestInferredLocationRecord> expected = _traceSupport
			  .readRecords(trace);

	  String taskId = _traceSupport.uploadTraceForSimulation(trace, true);

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
  
  private String getResponseForURL (String url) throws HttpException, IOException{
    HttpClient client = new HttpClient();
    GetMethod get = new GetMethod(url);
    _log.debug(url);
    client.executeMethod(get);
    return get.getResponseBodyAsString();
  }
  private HashMap<String,Object> getHashMapFromJsonResponse (String response) throws JsonParseException, JsonMappingException, IOException{
    JsonFactory factory = new JsonFactory(); 
    ObjectMapper mapper = new ObjectMapper(factory); 
    TypeReference<HashMap<String,Object>> typeRef = new TypeReference<HashMap<String,Object>>() {}; 
    HashMap<String,Object> o = mapper.readValue(response, typeRef);
    return o;
  }
  
  private String appendParamsToUrl(String url, Map<String, String> params) {
    if (params == null) return url;
    StringBuffer result = new StringBuffer();
    result.append(url);
    
    for (String key : params.keySet()) {
      result.append(key).append("=").append(params.get(key)).append("&");
    }
    
    return result.substring(0, result.length()-1); // truncate trailing "&"
  }


}
