package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.onebusaway.nyc.vehicle_tracking.model.NycTestLocationRecord;
import org.onebusaway.realtime.api.EVehiclePhase;

/**
 * This was taken from 04513.gps.csv.
 * 
 * blockA = 2008_12024495
 * 
 * duration = 4:55-8:55am
 * 
 * layovers = 5:41-6:00am, 6:41-6:55am, 7:45-8:00am
 * 
 * @author bdferris
 */
public class DeviatedTraceIntegrationTest extends AbstractTraceRunner {

  public DeviatedTraceIntegrationTest() {
    super("04513-0-deviated.gps.csv.gz");

    // We're purposely allowing the trace to deviate
    setDistanceTolerance(1000.0);

    setMinLayoverDuringRatio(0.90);
  }

  @Override
  public void validateRecords(List<NycTestLocationRecord> expected,
      List<NycTestLocationRecord> actual) {

    super.validateRecords(expected, actual);

    for (int i = 41; i < 44; i++) {
      NycTestLocationRecord record = actual.get(i);
      assertEquals(EVehiclePhase.IN_PROGRESS.toString(),
          record.getActualPhase());
      assertEquals("deviated", record.getActualStatus());
    }
  }
}
