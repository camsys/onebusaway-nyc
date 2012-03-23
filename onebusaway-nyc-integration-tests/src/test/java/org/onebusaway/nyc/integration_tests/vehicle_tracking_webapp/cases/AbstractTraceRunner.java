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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Writer;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Before;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import org.onebusaway.collections.Counter;
import org.onebusaway.csv_entities.CsvEntityWriterFactory;
import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.nyc.transit_data.services.VehicleTrackingManagementService;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.ParticleFactoryImpl;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.distributions.CategoricalDist;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestInferredLocationRecord;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.utility.DateLibrary;

import com.caucho.hessian.client.HessianProxyFactory;
import com.google.common.base.Strings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

public class AbstractTraceRunner {

  
  private double _schedTimeDiffMeanReq = 35d;
  private double _schedTimeDiffStdDevReq = 55d;

  private static Logger _log = LoggerFactory
      .getLogger(AbstractTraceRunner.class);

  private static TraceSupport _traceSupport = new TraceSupport();

  private VehicleTrackingManagementService _vehicleTrackingManagementService;

  private String _trace;

  private int _loops = 1;

  /**
   * The max amount of time we should wait for a single record to process
   */
  private long _maxTimeout = 40 * 1000;

  private double _minPhaseRatioForConsideration = 0.05;

  private double _minAccuracyRatio = 0.95;

  private Map<EVehiclePhase, Double> _minAccuracyRatiosByPhase = new TreeMap<EVehiclePhase, Double>();

  private double _median = 10.0;

  private double _standardDeviation = 20.0;

  private boolean _saveResultsOnAssertionError = true;

  public AbstractTraceRunner() {

  }

  public AbstractTraceRunner(String trace) {
    _trace = trace;
  }

  public void setTrace(String trace) {
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
    String port = System.getProperty(
        "org.onebusaway.transit_data_federation_webapp.port", "9905");
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

  @SuppressWarnings("unused")
  @Before
  public void setup() throws Exception {
    String federationPort = System.getProperty(
        "org.onebusaway.transit_data_federation_webapp.port", "9905");

    HessianProxyFactory factory = new HessianProxyFactory();

    _vehicleTrackingManagementService = (VehicleTrackingManagementService) factory
        .create(
            VehicleTrackingManagementService.class,
            "http://localhost:"
                + federationPort
                + "/onebusaway-nyc-vehicle-tracking-webapp/remoting/vehicle-tracking-management-service");

    setSeeds();
  }

  public void setSeeds() throws Exception {

    String port = System.getProperty(
        "org.onebusaway.transit_data_federation_webapp.port", "9905");
    String factorySeed = "298763210";
    String cdfSeed = "184970829";
    String urlStr = "http://localhost:"
        + port
        + "/onebusaway-nyc-vehicle-tracking-webapp/vehicle-location-simulation!set-seeds.do?factorySeed="
        + factorySeed + "&cdfSeed=" + cdfSeed;

    HttpClient client = new HttpClient();
    GetMethod get = new GetMethod(urlStr);
    client.executeMethod(get);

    String response = get.getResponseBodyAsString();
    if (!response.equals("OK"))
      throw new Exception("Failed trying to execute:" + urlStr);

    System.out.println("Successfully set seeds: phase=" + factorySeed + ", cdf="
        + cdfSeed);
  }

  @Test
  public void test() throws Throwable {
    Map<EVehiclePhase, Double> results = runTest();

    String failedLabels = "";
    for (Entry<EVehiclePhase, Double> result : results.entrySet()) {
      double relativeRatio = result.getValue();

      double minAccuracyRatio = _minAccuracyRatio;

      if (_minAccuracyRatiosByPhase.containsKey(result.getKey()))
        minAccuracyRatio = _minAccuracyRatiosByPhase.get(result.getKey());

      String label = "\taverage phase ratio " + result.getKey() + "="
          + relativeRatio + " vs min of " + minAccuracyRatio;

      System.out.println(label);

      if (relativeRatio < minAccuracyRatio) {
        failedLabels += "\n" + label;
      }
    }
    
    assertTrue(failedLabels, StringUtils.isBlank(failedLabels));
  }

  
  /**
   * 
   * @return map of phases to average acceptance ratios
   * @throws Throwable
   */
  public Map<EVehiclePhase, Double> runTest() throws Throwable {
    File trace = new File("src/integration-test/resources/traces/" + _trace);
    List<NycTestInferredLocationRecord> expected = _traceSupport
        .readRecords(trace);

    int successfulIterations = 0;
    Map<EVehiclePhase, Double> phaseResults = new TreeMap<EVehiclePhase, Double>();

    for (int i = 0; i < _loops; i++) {

      String taskId = _traceSupport.uploadTraceForSimulation(trace);

      // Wait for the task to complete
      long t = System.currentTimeMillis();
      int prevRecordCount = -1;

      while (true) {

        List<NycTestInferredLocationRecord> actual = _traceSupport
            .getSimulationResults(taskId);

//        String asString = _traceSupport.getRecordsAsString(actual);
//        _log.debug("actual records:\n" + asString);


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

          Map<EVehiclePhase, Double> theseResults = validateRecords(expected,
              actual);

          for (Entry<EVehiclePhase, Double> result : theseResults.entrySet()) {
            double relativeRatio = result.getValue();
            Double currentVal = phaseResults.get(result.getKey());
            phaseResults.put(result.getKey(), (currentVal == null ? 0.0
                : currentVal) * i / (i + 1) + relativeRatio / (i + 1));
          }
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
      System.out
          .println("success ratio=" + successfulIterations + "/" + _loops);

    return phaseResults;
  }

  /****
   * Protected Methods
   ****/

  @SuppressWarnings("null")
  protected Map<EVehiclePhase, Double> validateRecords(
      List<NycTestInferredLocationRecord> expected,
      List<NycTestInferredLocationRecord> actual) {

    Counter<EVehiclePhase> truePhaseCounts = new Counter<EVehiclePhase>();
    Counter<EVehiclePhase> infPhaseCounts = new Counter<EVehiclePhase>();

    int actuallyActiveTrips = 0;
    int correctlyPredictedActiveTrips = 0;
    int falsePositiveCount = 0;

    Map<EVehiclePhase, Double> phaseResults = new TreeMap<EVehiclePhase, Double>();

    DoubleArrayList distanceAlongBlockDeviations = new DoubleArrayList();
    DoubleArrayList schedDevDiff = new DoubleArrayList();

    for (int i = 0; i < expected.size(); i++) {

      NycTestInferredLocationRecord trueRecord = expected.get(i);
      NycTestInferredLocationRecord infRecord = actual.get(i);

      assertTrue(StringUtils.isNotEmpty(infRecord.getInferredStatus()));

      /*
       * Check that we don't register a trip for an out-of-service DSC
       */
      String dsc = infRecord.getInferredDsc();
      if (StringUtils.isNotBlank(infRecord.getInferredTripId())) {
        assertTrue(!_vehicleTrackingManagementService
            .isOutOfServiceDestinationSignCode(dsc)
            && !_vehicleTrackingManagementService
                .isUnknownDestinationSignCode(dsc));
      }

      EVehiclePhase truePhase = EVehiclePhase
          .valueOf(trueRecord.getActualPhase());

      assertTrue(truePhase != null);

      EVehiclePhase infPhase = EVehiclePhase.valueOf(infRecord
          .getInferredPhase());

      truePhaseCounts.increment(truePhase);

      
      if (checkRecordsMatch(trueRecord, infRecord))
        infPhaseCounts.increment(truePhase);
        

      if (EVehiclePhase.isActiveDuringBlock(truePhase)
          && EVehiclePhase.isActiveDuringBlock(infPhase)) {
        String expectedBlockId = trueRecord.getActualBlockId();
        String actualBlockId = infRecord.getInferredBlockId();

        if (trueRecord.getActualServiceDate() > 0
            && infRecord.getInferredServiceDate() > 0) {
          final double actualSchedDev = (trueRecord.getActualServiceDate() + trueRecord.getActualScheduleTime()*1000d)
              - trueRecord.getTimestamp();
          final double infSchedDev = (infRecord.getInferredServiceDate() + infRecord.getInferredScheduleTime()*1000d)
              - infRecord.getTimestamp();
        
          schedDevDiff.add((Math.abs(infSchedDev) - Math.abs(actualSchedDev))/(60d * 1000d));
        }
        
        
        // FIXME it's weird to sometimes check this, no?
        if (StringUtils.equals(expectedBlockId, actualBlockId)) {
          double expectedDistanceAlongBlock = trueRecord
              .getActualDistanceAlongBlock();
          double actualDistanceAlongBlock = infRecord
              .getInferredDistanceAlongBlock();
          double delta = Math.abs(expectedDistanceAlongBlock
              - actualDistanceAlongBlock);
          distanceAlongBlockDeviations.add(delta);
        }

      }

      /*
       * here we tally the number of correctly identified (truly) active trips.
       */
      if (EVehiclePhase.isActiveDuringBlock(truePhase) 
          && !Strings.isNullOrEmpty(infRecord.getActualTripId())) {
        ++actuallyActiveTrips;

        if (StringUtils.equals(infRecord.getActualTripId(),
            infRecord.getInferredTripId())) {
          ++correctlyPredictedActiveTrips;
        }
      }
      
      /*
       * record the false positives
       */
      if (!EVehiclePhase.isActiveDuringBlock(truePhase)
          && EVehiclePhase.isActiveDuringBlock(infPhase)) {
        ++falsePositiveCount;
      }
      
    }

    /****
     * Verify Ratios of Expected vs Actual Journey Phases
     ****/

    for (EVehiclePhase phase : EVehiclePhase.values()) {

      double expectedCount = truePhaseCounts.getCount(phase);
      double expRatio = expectedCount / expected.size();

      if (expRatio < _minPhaseRatioForConsideration)
        continue;

      double relativeRatio = infPhaseCounts.getCount(phase) / expectedCount;

      double minAccuracyRatio = _minAccuracyRatio;

      if (_minAccuracyRatiosByPhase.containsKey(phase))
        minAccuracyRatio = _minAccuracyRatiosByPhase.get(phase);

      String label = "phase ratio " + phase + "=" + relativeRatio
          + " vs min of " + minAccuracyRatio;

      System.out.println(label);

      phaseResults.put(phase, relativeRatio);

    }
    System.out.println("results of " + this.getClass().getSimpleName());

    /*
     * Check that we're generally active when we should be.
     */
//    final double activeTripRatio = (double)correctlyPredictedActiveTrips / actuallyActiveTrips;
//    System.out.println("\tactive trip ratio="
//        + activeTripRatio + " ("
//        + correctlyPredictedActiveTrips + "/" + actuallyActiveTrips + ")");
//    assertTrue("active trip ratio=" + activeTripRatio, activeTripRatio > 0.95);
    
    /*
     * Check that we're generally not active when we shouldn't be.
     */
    final double falsePositiveRatio = (double)falsePositiveCount / expected.size();
    System.out.println("\tfalse positive ratio=" + falsePositiveRatio);
    assertTrue("false positive ratio=" + falsePositiveRatio, falsePositiveRatio < 0.1);

    if (schedDevDiff.size() > 1) {
      
      /*
       * Check that our predicted schedule times aren't too far
       * off from the "actual" ones.
       */
      double mean = Descriptive.mean(schedDevDiff);
//      double median = Descriptive.median(schedDevDiff);
      double variance = Descriptive.sampleVariance(
          schedDevDiff, mean);
      double stdDev = Descriptive.sampleStandardDeviation(
          schedDevDiff.size(), variance);

//      System.out.println("\tschedTimeDiff median=" + median);
      System.out.println("\tschedTimeDiff mean=" + mean);
      System.out.println("\tschedTimeDiff stdDev=" + stdDev);
      
//      assertTrue("schedTimeDiff median=" + median, median < 15d);
      assertTrue("schedTimeDiff mean=" + mean, mean < _schedTimeDiffMeanReq);
//      assertTrue("schedTimeDiff stdDev=" + stdDev, mean + 1.98 * stdDev < _schedTimeDiffStdDevReq);
    }
    
    if (distanceAlongBlockDeviations.size() > 1) {

      /**
       * Check that distanceAlongBlockDeviations are within tolerances
       */
      double mean = Descriptive.mean(distanceAlongBlockDeviations);
//      double median = Descriptive.median(distanceAlongBlockDeviations);
      double variance = Descriptive.sampleVariance(
          distanceAlongBlockDeviations, mean);
      double stdDev = Descriptive.sampleStandardDeviation(
          distanceAlongBlockDeviations.size(), variance);

//      System.out.println("\tdistAlong median=" + median);
      System.out.println("\tdistAlong mean=" + mean);
      System.out.println("\tdistAlong stdDev=" + stdDev);

      // TODO make the an actual part of the tests
      // assertTrue("median=" + median, median < _median);
      // assertTrue("mean=" + mean, mean < 10.0);
      // assertTrue("stdDev" + stdDev, stdDev < _standardDeviation);
    }

    return phaseResults;
  }

  @SuppressWarnings("null")
  private boolean checkRecordsMatch(NycTestInferredLocationRecord trueRecord,
      NycTestInferredLocationRecord infRecord) {
    
    EVehiclePhase truePhase = EVehiclePhase.valueOf(trueRecord.getActualPhase());

    assertTrue(truePhase != null);

    EVehiclePhase infPhase = EVehiclePhase.valueOf(infRecord.getInferredPhase());

    if (truePhase.equals(infPhase))
      return true;
    
    if (!trueRecord.isReportedRunInfoSet() || !Strings.isNullOrEmpty(trueRecord.getActualRunId())) {
      /*
       * When the trace doesn't include run information, then
       * we don't expect much as far as during's and after's.
       */
      if (
            (
              (truePhase.equals(EVehiclePhase.DEADHEAD_AFTER)
                  && infPhase.equals(EVehiclePhase.DEADHEAD_BEFORE))
                || 
                (infPhase.equals(EVehiclePhase.DEADHEAD_AFTER)
                  && truePhase.equals(EVehiclePhase.DEADHEAD_BEFORE))
            )
          ||
            (
              (truePhase.equals(EVehiclePhase.DEADHEAD_BEFORE)
                  && infPhase.equals(EVehiclePhase.LAYOVER_BEFORE))
                || 
                (infPhase.equals(EVehiclePhase.DEADHEAD_BEFORE)
                  && truePhase.equals(EVehiclePhase.LAYOVER_BEFORE))
            )
          ||     
            (
              (truePhase.equals(EVehiclePhase.DEADHEAD_DURING)
                  && infPhase.equals(EVehiclePhase.DEADHEAD_BEFORE))
                || 
                (infPhase.equals(EVehiclePhase.DEADHEAD_DURING)
                  && truePhase.equals(EVehiclePhase.DEADHEAD_BEFORE))
            )
          ||
            (
                (truePhase.equals(EVehiclePhase.LAYOVER_DURING)
                  && infPhase.equals(EVehiclePhase.LAYOVER_BEFORE))
                || 
                (infPhase.equals(EVehiclePhase.LAYOVER_DURING)
                  && truePhase.equals(EVehiclePhase.LAYOVER_BEFORE))
            )
          ) {
        return true;
      }
    } 
    
    return false;
  }
    

  protected void writeResultsOnAssertionError(
      List<NycTestInferredLocationRecord> actual) {
    try {
      File outputFile = File.createTempFile(getClass().getName() + "-",
          "-results.csv");
      CsvEntityWriterFactory factory = new CsvEntityWriterFactory();
      Writer out = new FileWriter(outputFile);
      EntityHandler handler = factory.createWriter(
          NycTestInferredLocationRecord.class, out);

      for (NycTestInferredLocationRecord record : actual)
        handler.handleEntity(record);

      out.close();

      System.err.println("on assertion error, wrote results to " + outputFile);
    } catch (Exception ex) {
      _log.error("error writing results on assertion error", ex);
    }
  }

  public double getSchedTimeDiffMeanReq() {
    return _schedTimeDiffMeanReq;
  }

  public void setSchedTimeDiffMeanReq(double schedTimeDiffMeanReq) {
    _schedTimeDiffMeanReq = schedTimeDiffMeanReq;
  }

  public double getSchedTimeDiffStdDevReq() {
    return _schedTimeDiffStdDevReq;
  }

  public void setSchedTimeDiffStdDevReq(double schedTimeDiffStdDevReq) {
    _schedTimeDiffStdDevReq = schedTimeDiffStdDevReq;
  }

}
