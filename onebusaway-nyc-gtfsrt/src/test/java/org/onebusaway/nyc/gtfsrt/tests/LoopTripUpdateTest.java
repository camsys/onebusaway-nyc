/**
 * Copyright (C) 2017 Cambridge Systematics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.nyc.gtfsrt.tests;

import com.google.transit.realtime.GtfsRealtime;
import org.onebusaway.collections.MappingLibrary;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.realtime.api.TimepointPredictionRecord;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test that a looping trip (trip which has the same stop in two positions) is handled correctly.
 */
public class LoopTripUpdateTest extends TripUpdateTest {

  public LoopTripUpdateTest() {
    super("google_transit_queens.zip", "MTA", "7399_Q48_2017-04-10/btmap.tsv", "7399_Q48_2017-04-10/vlrb.tsv", "7399_Q48_2017-04-10/tripupdate.pb");
  }

  @Override
  protected void assertStopTimeUpdatesMatchTprs(List<TimepointPredictionRecord> records, List<GtfsRealtime.TripUpdate.StopTimeUpdate> stus) {
    assertEquals(records.size(), stus.size());
    Map<Integer, TimepointPredictionRecord> tprByStop = MappingLibrary.mapToValue(records, "stopSequence");
    for (GtfsRealtime.TripUpdate.StopTimeUpdate stu : stus) {
      TimepointPredictionRecord tpr = tprByStop.get(stu.getStopSequence());
      assertNotNull(tpr);
      assertEquals(tpr.getTimepointId().getId(), stu.getStopId());
      assertEquals(tpr.getStopSequence(), stu.getStopSequence());
      long time = tpr.getTimepointPredictedTime()/1000;
      assertTrue(stu.hasArrival() || stu.hasDeparture());
      if (stu.hasArrival())
        assertEquals(time, stu.getArrival().getTime());
      if (stu.hasDeparture())
        assertEquals(time, stu.getDeparture().getTime());
      // TODO - will arrival or departure be different at some point?
    }
  }
}
