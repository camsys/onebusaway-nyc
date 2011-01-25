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
package org.onebusaway.nyc.vehicle_tracking.impl.inference;

import static org.junit.Assert.assertEquals;
import static org.onebusaway.transit_data_federation.testing.UnitTestingSupport.*;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary.Builder;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.impl.transit_graph.BlockEntryImpl;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;

public class JourneyPhaseSummaryLibraryTest {

  private JourneyPhaseSummaryLibrary _library = new JourneyPhaseSummaryLibrary();

  @Test
  public void test() {

    BlockEntryImpl blockA = block("a");
    BlockEntryImpl blockB = block("b");

    BlockConfigurationEntry blockConfigA = blockConfiguration(blockA,
        serviceIds(lsids("a"), lsids()));
    BlockConfigurationEntry blockConfigB = blockConfiguration(blockB,
        serviceIds(lsids("a"), lsids()));

    BlockInstance blockInstanceA = new BlockInstance(blockConfigA, 0);
    BlockInstance blockInstanceB = new BlockInstance(blockConfigB, 0);

    List<JourneyPhaseSummary> summaries = new ArrayList<JourneyPhaseSummary>();

    Builder b = JourneyPhaseSummary.builder();
    b.setTimeFrom(0);
    b.setTimeTo(1000);
    b.setPhase(EVehiclePhase.IN_PROGRESS);
    b.setBlockCompletionRatioFrom(0.0);
    b.setBlockCompletionRatioTo(0.5);
    b.setBlockInstance(blockInstanceA);
    summaries.add(b.create());

    b = JourneyPhaseSummary.builder();
    b.setTimeFrom(1000);
    b.setTimeTo(2000);
    b.setPhase(EVehiclePhase.IN_PROGRESS);
    b.setBlockCompletionRatioFrom(0.4);
    b.setBlockCompletionRatioTo(0.6);
    b.setBlockInstance(blockInstanceB);
    summaries.add(b.create());

    b = JourneyPhaseSummary.builder();
    b.setTimeFrom(2000);
    b.setTimeTo(3000);
    b.setPhase(EVehiclePhase.LAYOVER_DURING);
    b.setBlockCompletionRatioFrom(0.6);
    b.setBlockCompletionRatioTo(0.6);
    b.setBlockInstance(blockInstanceB);
    summaries.add(b.create());

    b = JourneyPhaseSummary.builder();
    b.setTimeFrom(3000);
    b.setTimeTo(4000);
    b.setPhase(EVehiclePhase.IN_PROGRESS);
    b.setBlockCompletionRatioFrom(0.6);
    b.setBlockCompletionRatioTo(0.8);
    b.setBlockInstance(blockInstanceB);
    summaries.add(b.create());

    JourneyPhaseSummary s1 = _library.getCurrentBlock(summaries);
    assertEquals(1000, s1.getTimeFrom());
    assertEquals(4000, s1.getTimeTo());
    assertEquals(EVehiclePhase.IN_PROGRESS, s1.getPhase());
    assertEquals(0.4, s1.getBlockCompletionRatioFrom(), 0.0);
    assertEquals(0.8, s1.getBlockCompletionRatioTo(), 0.0);
    assertEquals(blockInstanceB, s1.getBlockInstance());

    JourneyPhaseSummary s2 = _library.getPreviousBlock(summaries, s1);
    assertEquals(0, s2.getTimeFrom());
    assertEquals(1000, s2.getTimeTo());
    assertEquals(EVehiclePhase.IN_PROGRESS, s2.getPhase());
    assertEquals(0.0, s2.getBlockCompletionRatioFrom(), 0.0);
    assertEquals(0.5, s2.getBlockCompletionRatioTo(), 0.0);
    assertEquals(blockInstanceA, s2.getBlockInstance());

  }
}
