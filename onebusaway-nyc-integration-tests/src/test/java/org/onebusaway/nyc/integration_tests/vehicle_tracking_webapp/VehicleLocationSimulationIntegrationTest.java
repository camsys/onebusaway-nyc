package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.onebusaway.geospatial.services.SphericalGeometryLibrary;
import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;

public class VehicleLocationSimulationIntegrationTest {

  private static TraceSupport _traceSupport = new TraceSupport();

  @Test
  public void test() throws IOException, InterruptedException {

    File trace = new File(
        "src/integration-test/resources/traces/2008_20100627CA_145000_B62_0014_B62_32-noisy.csv.gz");
    List<NycTestLocationRecord> expected = _traceSupport.readRecords(trace);

    String taskId = _traceSupport.uploadTraceForSimulation(trace);

    // Wait for the task to complete
    Thread.sleep(60 * 1000);

    List<NycTestLocationRecord> actual = _traceSupport.getSimulationResults(taskId);

    assertEquals(expected.size(), actual.size());

    for (int i = 0; i < expected.size(); i++) {
      NycTestLocationRecord expRecord = expected.get(i);
      NycTestLocationRecord actRecord = actual.get(i);
      double d = SphericalGeometryLibrary.distance(expRecord.getActualLat(),
          expRecord.getActualLon(), actRecord.getLat(), actRecord.getLon());
      System.out.println(d);
      assertTrue("record=" + i + " distance=" + d, d < 100);
      assertEquals(expRecord.getActualBlockId(), actRecord.getActualBlockId());
    }

  }
}
