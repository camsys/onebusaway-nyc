package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.TraceSupport;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractTraceRunner {

  private static Logger _log = LoggerFactory.getLogger(AbstractTraceRunner.class);

  private static TraceSupport _traceSupport = new TraceSupport();

  private String _trace;

  private int _allowBlockMismatchOnFirstNRecords = 1;

  private double _distanceTolerance = 100.0;

  private long _maxTimeout = 2 * 60 * 1000;

  private String _expectedBlockId;

  public AbstractTraceRunner(String trace) {
    _trace = trace;
  }

  public void setAllowBlockMismatchOnFirstNRecords(
      int allowBlockMismatchOnFirstNRecords) {
    _allowBlockMismatchOnFirstNRecords = allowBlockMismatchOnFirstNRecords;
  }

  public void setDistanceTolerance(double distanceTolerance) {
    _distanceTolerance = distanceTolerance;
  }

  public void setMaxTimeout(long maxTimeout) {
    _maxTimeout = maxTimeout;
  }

  public void setExpectedBlockId(String expectedBlockId) {
    _expectedBlockId = expectedBlockId;
  }
  
  @Test
  public void temp() {
    
  }

  //@Test
  public void test() throws IOException, InterruptedException {

    File trace = new File("src/integration-test/resources/traces/" + _trace);
    List<NycTestLocationRecord> expected = _traceSupport.readRecords(trace);

    String taskId = _traceSupport.uploadTraceForSimulation(trace);

    // Wait for the task to complete

    long t = System.currentTimeMillis();

    while (true) {

      List<NycTestLocationRecord> actual = _traceSupport.getSimulationResults(taskId);

      String asString = _traceSupport.getRecordsAsString(actual);
      _log.debug("actual records:\n" + asString);

      if (actual.size() < expected.size()) {
        if (t + _maxTimeout < System.currentTimeMillis()) {
          fail("waited but never received enough records: expected="
              + expected.size() + " actual=" + actual.size());
        }
        Thread.sleep(10 * 1000);
        continue;
      }

      assertEquals(expected.size(), actual.size());

      for (int i = 0; i < expected.size(); i++) {

        NycTestLocationRecord expRecord = expected.get(i);
        NycTestLocationRecord actRecord = actual.get(i);

        double d = SphericalGeometryLibrary.distance(expRecord.getActualLat(),
            expRecord.getActualLon(), actRecord.getLat(), actRecord.getLon());

        assertTrue("record=" + i + " distance=" + d, d < _distanceTolerance);

        String expectedBlockId = expRecord.getActualBlockId();

        if (_expectedBlockId != null)
          expectedBlockId = _expectedBlockId;

        if (i < _allowBlockMismatchOnFirstNRecords) {
          if (!expectedBlockId.equals(actRecord.getActualBlockId())) {
            _log.info("expected block mismatch: expected=" + expectedBlockId
                + " actual=" + actRecord.getActualBlockId() + " i=" + i);
          }
        } else {
          assertEquals(expectedBlockId, actRecord.getActualBlockId());
        }
      }

      return;
    }

  }

}
