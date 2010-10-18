package org.onebusaway.nyc.integration_tests.vehicle_tracking_webapp.cases;

import static org.junit.Assert.*;

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
public class StalledWithZeroedGpsReadingsTraceIntegrationTest extends AbstractTraceRunner {

  public StalledWithZeroedGpsReadingsTraceIntegrationTest() {
    super("04513-0-stalled-zeros.gps.csv.gz");
  }

  @Override
  public void validateRecords(List<NycTestLocationRecord> expected,
      List<NycTestLocationRecord> actual) {

    super.validateRecords(expected, actual);

    for (int i = 42; i < 46; i++) {
      NycTestLocationRecord record = actual.get(i);
      assertEquals(EVehiclePhase.IN_PROGRESS.toString(),
          record.getActualPhase());
      assertEquals("stalled", record.getActualStatus());
    }

    assertNull(actual.get(41).getActualStatus());
    assertNull(actual.get(46).getActualStatus());
  }
}
