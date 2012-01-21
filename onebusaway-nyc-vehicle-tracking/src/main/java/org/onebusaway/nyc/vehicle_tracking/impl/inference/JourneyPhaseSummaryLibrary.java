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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.BlockStateObservation;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary.Builder;
import org.onebusaway.realtime.api.EVehiclePhase;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.BlockConfigurationEntry;
import org.springframework.stereotype.Component;

@Component
public class JourneyPhaseSummaryLibrary {

  public List<JourneyPhaseSummary> extendSummaries(VehicleState parentState,
      BlockStateObservation blockState, JourneyState journeyState, Observation observation) {

    List<JourneyPhaseSummary> parentSummaries = Collections.emptyList();

    if (parentState != null)
      parentSummaries = parentState.getJourneySummaries();

    List<JourneyPhaseSummary> summaries = new ArrayList<JourneyPhaseSummary>(
        parentSummaries);

    if (summaries.isEmpty()) {
      JourneyPhaseSummary summary = createJourneySummary(blockState.getBlockState(),
          journeyState, observation);
      summaries.add(summary);
    } else {
      JourneyPhaseSummary previous = summaries.get(summaries.size() - 1);
      JourneyPhaseSummary merged = createMergedJourneySummary(blockState.getBlockState(),
          journeyState, observation, previous);
      if (merged != null) {
        summaries.set(summaries.size() - 1, merged);
      } else {
        JourneyPhaseSummary summary = createJourneySummary(blockState.getBlockState(),
            journeyState, observation);
        summaries.add(summary);
      }
    }

    return summaries;
  }

  public JourneyPhaseSummary getCurrentBlock(List<JourneyPhaseSummary> summaries) {

    if (summaries.isEmpty())
      return null;

    JourneyPhaseSummary summary = summaries.get(summaries.size() - 1);

    if (!EVehiclePhase.isActiveDuringBlock(summary.getPhase()))
      return null;

    return mergeBackwardsSummariesWithSameBlock(summaries, summaries.size() - 1);
  }

  public JourneyPhaseSummary getPreviousBlock(List<JourneyPhaseSummary> summaries,
      JourneyPhaseSummary currentBlock) {

    BlockInstance blockInstance = currentBlock.getBlockInstance();

    for (int i = summaries.size() - 1; i >= 0; i--) {
      JourneyPhaseSummary summary = summaries.get(i);
      if (summary.getBlockInstance() != null
          && ! summary.getBlockInstance().equals(blockInstance))
        return mergeBackwardsSummariesWithSameBlock(summaries, i);
    }

    return null;
  }

  /****
   * Private Methods
   ****/

  private JourneyPhaseSummary createJourneySummary(BlockState blockState,
      JourneyState journeyState, Observation obs) {

    Builder b = JourneyPhaseSummary.builder();
    b.setTimeFrom(obs.getTime());
    b.setTimeTo(obs.getTime());
    b.setPhase(journeyState.getPhase());

    if (blockState != null) {
      BlockInstance blockInstance = blockState.getBlockInstance();
      b.setBlockInstance(blockInstance);

      double blockCompletionRatio = getBlockCompletionRatio(blockState);

      b.setBlockCompletionRatioFrom(blockCompletionRatio);
      b.setBlockCompletionRatioTo(blockCompletionRatio);
    }

    return b.create();
  }

  private JourneyPhaseSummary createMergedJourneySummary(BlockState blockState,
      JourneyState journeyState, Observation obs, JourneyPhaseSummary previous) {

    if (previous.getPhase() != journeyState.getPhase())
      return null;

    BlockInstance blockInstance = null;
    if (blockState != null)
      blockInstance = blockState.getBlockInstance();

    if (!ObjectUtils.equals(previous.getBlockInstance(), blockInstance))
      return null;

    Builder b = JourneyPhaseSummary.builder(previous);
    b.setTimeTo(obs.getTime());

    if (blockState != null) {
      double blockCompletionRatio = getBlockCompletionRatio(blockState);
      b.setBlockCompletionRatioTo(blockCompletionRatio);
    }

    return b.create();
  }

  private double getBlockCompletionRatio(BlockState blockState) {

    BlockInstance blockInstance = blockState.getBlockInstance();
    ScheduledBlockLocation blockLocation = blockState.getBlockLocation();
    BlockConfigurationEntry blockConfig = blockInstance.getBlock();

    return (double) blockLocation.getDistanceAlongBlock()
        / blockConfig.getTotalBlockDistance();
  }

  private JourneyPhaseSummary mergeBackwardsSummariesWithSameBlock(
      List<JourneyPhaseSummary> summaries, int indexFrom) {

    JourneyPhaseSummary summary = summaries.get(indexFrom);
    Builder b = JourneyPhaseSummary.builder(summary);

    for (int i = indexFrom - 1; i >= 0; i--) {
      JourneyPhaseSummary prev = summaries.get(i);
      if (!ObjectUtils.equals(summary.getBlockInstance(),
          prev.getBlockInstance()))
        break;
      b.setBlockCompletionRatioFrom(prev.getBlockCompletionRatioFrom());
      b.setTimeFrom(prev.getTimeFrom());
    }

    return b.create();
  }

}
