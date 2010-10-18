package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.onebusaway.collections.Counter;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.TraceSupport;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cern.colt.list.DoubleArrayList;
import cern.jet.stat.Descriptive;

public class AbstractTraceRunner {

  private static Logger _log = LoggerFactory.getLogger(AbstractTraceRunner.class);

  private static TraceSupport _traceSupport = new TraceSupport();

  private String _trace;

  private double _distanceTolerance = 100.0;

  /**
   * The max amount of time we should wait for a single record to process
   */
  private long _maxTimeout = 20 * 1000;

  private double _minLayoverDuringRatio = 0.95;

  public AbstractTraceRunner(String trace) {
    _trace = trace;
  }

  public void setDistanceTolerance(double distanceTolerance) {
    _distanceTolerance = distanceTolerance;
  }

  public void setMaxTimeout(long maxTimeout) {
    _maxTimeout = maxTimeout;
  }

  public void setMinLayoverDuringRatio(double minLayoverDuringRatio) {
    _minLayoverDuringRatio = minLayoverDuringRatio;
  }

  @Test
  public void test() throws IOException, InterruptedException {

    File trace = new File("src/integration-test/resources/traces/" + _trace);
    List<NycTestLocationRecord> expected = _traceSupport.readRecords(trace);

    String taskId = _traceSupport.uploadTraceForSimulation(trace);

    // Wait for the task to complete

    long t = System.currentTimeMillis();
    int prevRecordCount = -1;

    while (true) {

      List<NycTestLocationRecord> actual = _traceSupport.getSimulationResults(taskId);

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

        Thread.sleep(10 * 1000);
        continue;
      }

      assertEquals(expected.size(), actual.size());

      validateRecords(expected, actual);

      return;
    }

  }

  public void validateRecords(List<NycTestLocationRecord> expected,
      List<NycTestLocationRecord> actual) {

    Counter<EVehiclePhase> expPhaseCounts = new Counter<EVehiclePhase>();
    Counter<EVehiclePhase> actPhaseCounts = new Counter<EVehiclePhase>();

    int totalBlockComparisons = 0;
    int totalCorrectBlockComparisons = 0;

    DoubleArrayList distanceAlongBlockDeviations = new DoubleArrayList();

    for (int i = 0; i < expected.size(); i++) {

      NycTestLocationRecord expRecord = expected.get(i);
      NycTestLocationRecord actRecord = actual.get(i);

      double d = SphericalGeometryLibrary.distance(expRecord.getActualLat(),
          expRecord.getActualLon(), actRecord.getLat(), actRecord.getLon());

      assertTrue("record=" + i + " distance=" + d, d < _distanceTolerance);

      EVehiclePhase expPhase = EVehiclePhase.valueOf(expRecord.getActualPhase());
      EVehiclePhase actPhase = EVehiclePhase.valueOf(actRecord.getActualPhase());

      expPhaseCounts.increment(expPhase);

      if (expPhase.equals(actPhase))
        actPhaseCounts.increment(expPhase);

      if (EVehiclePhase.isActiveDuringBlock(expPhase)
          && EVehiclePhase.isActiveDuringBlock(actPhase)) {
        String expectedBlockId = expRecord.getActualBlockId();
        String actualBlockId = actRecord.getActualBlockId();

        totalBlockComparisons++;

        if (expectedBlockId.equals(actualBlockId))
          totalCorrectBlockComparisons++;

        double expectedDistanceAlongBlock = expRecord.getActualDistanceAlongBlock();
        double actualDistanceAlongBlock = actRecord.getActualDistanceAlongBlock();
        double delta = Math.abs(expectedDistanceAlongBlock
            - actualDistanceAlongBlock);
        distanceAlongBlockDeviations.add(delta);
      }
    }

    /****
     * Verify Ratios of Expected vs Actual Journey Phases
     ****/

    double inProgressRatio = computePhaseRatio(expPhaseCounts, actPhaseCounts,
        EVehiclePhase.IN_PROGRESS);
    assertTrue("inProgressRatio=" + inProgressRatio, inProgressRatio > 0.95);

    double layoverDuringRatio = computePhaseRatio(expPhaseCounts,
        actPhaseCounts, EVehiclePhase.LAYOVER_DURING);

    assertTrue("layoverDuringRatio=" + layoverDuringRatio,
        layoverDuringRatio > _minLayoverDuringRatio);

    /**
     * Check that distanceAlongBlockDeviations are within tolerances
     */
    double mean = Descriptive.mean(distanceAlongBlockDeviations);
    double median = Descriptive.median(distanceAlongBlockDeviations);
    double variance = Descriptive.sampleVariance(distanceAlongBlockDeviations,
        mean);
    double stdDev = Descriptive.sampleStandardDeviation(
        distanceAlongBlockDeviations.size(), variance);

    assertTrue("median=" + median, median < 40.0);
    assertTrue("mean=" + mean, mean < 40.0);
    assertTrue("stdDev" + stdDev, stdDev < 70.0);
  }

  public double computePhaseRatio(Counter<EVehiclePhase> expPhaseCounts,
      Counter<EVehiclePhase> actPhaseCounts, EVehiclePhase phase) {
    return (double) actPhaseCounts.getCount(phase)
        / (double) expPhaseCounts.getCount(phase);
  }
}
