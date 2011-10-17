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
package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Before;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.onebusaway.collections.Counter;
import org.onebusaway.csv_entities.CsvEntityWriterFactory;
import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.realtime.api.VehicleLocationListener;
import org.onebusaway.utility.DateLibrary;

import com.caucho.hessian.client.HessianProxyFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

public class AbstractTraceRunner {

  private static Logger _log = LoggerFactory.getLogger(AbstractTraceRunner.class);

  private static TraceSupport _traceSupport = new TraceSupport();

  private VehicleTrackingManagementService _vehicleTrackingManagementService;
  
  private String _trace;

  private int _loops = 1;

  /**
   * The max amount of time we should wait for a single record to process
   */
  private long _maxTimeout = 20 * 1000;

  private double _minPhaseRatioForConsideration = 0.05;

  private double _minAccuracyRatio = 0.95;

  private Map<EVehiclePhase, Double> _minAccuracyRatiosByPhase = new HashMap<EVehiclePhase, Double>();

  private double _median = 10.0;

  private double _standardDeviation = 20.0;

  private boolean _saveResultsOnAssertionError = true;

  public AbstractTraceRunner(String trace) {
    _trace = trace;
  }

  public void setMaxTimeout(long maxTimeout) {
    _maxTimeout = maxTimeout;
  }

  public void setLoops(int loops) {
    _loops = loops;
  }

  public void setMinAccuracyRatio(double minAccuracyRatio) {
    _minAccuracyRatio = minAccuracyRatio;
  }

  public void setMinAccuracyRatioForPhase(EVehiclePhase phase,
      double minAccuracyRatio) {
    _minAccuracyRatiosByPhase.put(phase, minAccuracyRatio);
  }

  public void setMedian(double median) {
    _median = median;
  }

  public void setStandardDeviation(double standardDeviation) {
    _standardDeviation = standardDeviation;
  }

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
  
  @SuppressWarnings("unused")
  @Before 
  private void setup() throws Exception {
    String federationPort = System.getProperty("org.onebusaway.transit_data_federation_webapp.port","9905");

    HessianProxyFactory factory = new HessianProxyFactory();
    
    _vehicleTrackingManagementService = 
        (VehicleTrackingManagementService)factory.create(VehicleTrackingManagementService.class, "http://localhost:" + federationPort + "/onebusaway-nyc-vehicle-tracking-webapp/remoting/vehicle-tracking-management-service");
  }
  
  @Test
  public void test() throws Throwable {        
    File trace = new File("src/integration-test/resources/traces/" + _trace);
    List<NycTestInferredLocationRecord> expected = _traceSupport.readRecords(trace);

    int successfulIterations = 0;

    for (int i = 0; i < _loops; i++) {

      String taskId = _traceSupport.uploadTraceForSimulation(trace);

      // Wait for the task to complete
      long t = System.currentTimeMillis();
      int prevRecordCount = -1;

      while (true) {

        List<NycTestInferredLocationRecord> actual = _traceSupport.getSimulationResults(taskId);

        String asString = _traceSupport.getRecordsAsString(actual);
        _log.debug("actual records:\n" + asString);

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

        try {
          assertEquals(expected.size(), actual.size());

          validateRecords(expected, actual);
        } catch (Throwable ex) {
          if (_saveResultsOnAssertionError)
            writeResultsOnAssertionError(actual);
          if (_loops == 1)
            throw ex;
          else
            successfulIterations++;
        }

        break;
      }
    }

    if (_loops > 1)
      System.out.println("success ratio=" + successfulIterations + "/" + _loops);
  }

  /****
   * Protected Methods
   ****/

  protected void validateRecords(List<NycTestInferredLocationRecord> expected,
      List<NycTestInferredLocationRecord> actual) {

    Counter<EVehiclePhase> expPhaseCounts = new Counter<EVehiclePhase>();
    Counter<EVehiclePhase> actPhaseCounts = new Counter<EVehiclePhase>();

    DoubleArrayList distanceAlongBlockDeviations = new DoubleArrayList();

    for (int i = 0; i < expected.size(); i++) {

      NycTestInferredLocationRecord expRecord = expected.get(i);
      NycTestInferredLocationRecord actRecord = actual.get(i);

      assertTrue(StringUtils.isNotEmpty(expRecord.getInferredStatus()));
      
      EVehiclePhase expPhase = EVehiclePhase.valueOf(expRecord.getActualPhase());
      
      assertTrue(expPhase != null);
      
      EVehiclePhase actPhase = EVehiclePhase.valueOf(actRecord.getInferredPhase());

      expPhaseCounts.increment(expPhase);

      if (expPhase.equals(actPhase))
        actPhaseCounts.increment(expPhase);

      if (EVehiclePhase.isActiveDuringBlock(expPhase)
          && EVehiclePhase.isActiveDuringBlock(actPhase)) {
        String expectedBlockId = expRecord.getActualBlockId();
        String actualBlockId = actRecord.getInferredBlockId();

        if (expectedBlockId.equals(actualBlockId)) {
          double expectedDistanceAlongBlock = expRecord.getActualDistanceAlongBlock();
          double actualDistanceAlongBlock = actRecord.getInferredDistanceAlongBlock();
          double delta = Math.abs(expectedDistanceAlongBlock
              - actualDistanceAlongBlock);
          distanceAlongBlockDeviations.add(delta);
        }
      }
    }

    /****
     * Verify Ratios of Expected vs Actual Journey Phases
     ****/

    for (EVehiclePhase phase : EVehiclePhase.values()) {

      double expectedCount = expPhaseCounts.getCount(phase);
      double expRatio = expectedCount / expected.size();

      if (expRatio < _minPhaseRatioForConsideration)
        continue;

      double relativeRatio = actPhaseCounts.getCount(phase) / expectedCount;

      double minAccuracyRatio = _minAccuracyRatio;

      if (_minAccuracyRatiosByPhase.containsKey(phase))
        minAccuracyRatio = _minAccuracyRatiosByPhase.get(phase);

      String label = "phase ratio " + phase + "=" + relativeRatio
          + " vs min of " + minAccuracyRatio;

      System.out.println(label);

      assertTrue(label, relativeRatio > minAccuracyRatio);

    }

    if (distanceAlongBlockDeviations.size() > 1) {

      /**
       * Check that distanceAlongBlockDeviations are within tolerances
       */
      double mean = Descriptive.mean(distanceAlongBlockDeviations);
      double median = Descriptive.median(distanceAlongBlockDeviations);
      double variance = Descriptive.sampleVariance(
          distanceAlongBlockDeviations, mean);
      double stdDev = Descriptive.sampleStandardDeviation(
          distanceAlongBlockDeviations.size(), variance);

      System.out.println("median=" + median);
      System.out.println("mean=" + mean);
      System.out.println("stdDev=" + stdDev);

      assertTrue("median=" + median, median < _median);
      assertTrue("mean=" + mean, mean < 10.0);
      assertTrue("stdDev" + stdDev, stdDev < _standardDeviation);
    }
  }

  protected void writeResultsOnAssertionError(List<NycTestInferredLocationRecord> actual) {
    try {
      File outputFile = File.createTempFile(getClass().getName() + "-",
          "-results.csv");
      CsvEntityWriterFactory factory = new CsvEntityWriterFactory();
      Writer out = new FileWriter(outputFile);
      EntityHandler handler = factory.createWriter(NycTestInferredLocationRecord.class,
          out);

      for (NycTestInferredLocationRecord record : actual)
        handler.handleEntity(record);

      out.close();

      System.err.println("on assertion error, wrote results to " + outputFile);
    } catch (Exception ex) {
      _log.error("error writing results on assertion error", ex);
    }
  }

}
