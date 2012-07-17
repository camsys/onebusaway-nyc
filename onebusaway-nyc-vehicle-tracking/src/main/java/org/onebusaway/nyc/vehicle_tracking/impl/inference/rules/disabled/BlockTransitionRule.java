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
package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.disabled;

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.not;
import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.p;

import org.onebusaway.nyc.vehicle_tracking.impl.inference.JourneyPhaseSummaryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Context;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelRule;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.SensorModelSupportLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.BadProbabilityParticleFilterException;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.SensorModelResult;

import org.apache.commons.lang.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * NOTE: This rule has been disabled
 * 
 * We wish to control the transition between blocks. If a block transition from
 * BlockA to BlockB is detected, we wish the following to hold:
 * 
 * If a vehicle has finished BlockA after serving some minimum amount of BlockA,
 * then the transition to BlockB should not happen instantly (aka there should
 * be some amount of layover in-between).
 * 
 * This rule attempts to prevent a vehicle from finishing one block and
 * immediately starting another when it's in fact deadheading back to the base
 * (in the case where the DSC doesn't tell us anything). The majority of the
 * time, a vehicle typically DOES deadhead back to the base after completing a
 * block, but in the few cases we've seen where that was not the case, there was
 * typically a layover at the end of the last block before the next block was
 * started.
 * 
 * @author bdferris
 */
// @Component
public class BlockTransitionRule implements SensorModelRule {

  private final DeviationModel _blockCompletedRatio = new DeviationModel(0.03);

  /**
   * If a vehicle has serviced only a short period of time on a particular block
   * (~ less than 15 minutes), we consider it unserviced.
   */
  private final DeviationModel _blockUnserviced = new DeviationModel(15 * 60);

  private JourneyPhaseSummaryLibrary _journeyPhaseSummaryLibrary;

  @Autowired
  public void setJourneyPhaseSummaryLibrary(
      JourneyPhaseSummaryLibrary journeyPhaseSummaryLibrary) {
    _journeyPhaseSummaryLibrary = journeyPhaseSummaryLibrary;
  }

  @Override
  public SensorModelResult likelihood(SensorModelSupportLibrary library,
      Context context) throws BadProbabilityParticleFilterException {

    final VehicleState state = context.getState();
    final List<JourneyPhaseSummary> summaries = state.getJourneySummaries();
    final JourneyPhaseSummary currentBlock = _journeyPhaseSummaryLibrary.getCurrentBlock(summaries);

    /**
     * We first need a current block
     */
    if (currentBlock == null)
      return new SensorModelResult("pBlockTransition - no current block", 1.0);

    /**
     * If we've been serving a sufficient amount of the current block, we can
     * assume it survived the block transition process before and would survive
     * again (performance op so we don't have to compute everything else each
     * iteration)
     */
    final int currentBlockDuration = (int) ((currentBlock.getTimeTo() - currentBlock.getTimeFrom()) / 1000);
    if (currentBlockDuration > 5 * 60)
      return new SensorModelResult("pBlockTransition", 1.0);

    /**
     * We next need a previous block
     */
    final JourneyPhaseSummary previousBlock = _journeyPhaseSummaryLibrary.getPreviousBlock(
        summaries, currentBlock);

    if (previousBlock == null)
      return new SensorModelResult("pBlockTransition", 1.0);

    /**
     * We only care if the two blocks are different
     */
    if (ObjectUtils.equals(currentBlock.getBlockInstance(),
        previousBlock.getBlockInstance()))
      return new SensorModelResult("pBlockTransition", 1.0);

    /**
     * Did we complete the previous block?
     */
    final double pCompletedBlock = _blockCompletedRatio.probability(1.0 - previousBlock.getBlockCompletionRatioTo());

    /**
     * Did we service a sufficient amount of the previous block for it to count
     */
    final int previousBlockDuration = (int) ((previousBlock.getTimeTo() - previousBlock.getTimeFrom()) / 1000);
    final double pServicedSomePartOfBlock = not(_blockUnserviced.probability(previousBlockDuration));

    /**
     * If the transition between a completed block to the next block happens
     * immediately with no layover, we should be concerned.
     * 
     * TODO: Detect an acutal layover
     */
    final int blockTransitionDuration = (int) ((currentBlock.getTimeFrom() - previousBlock.getTimeTo()) / 1000);
    p(blockTransitionDuration < 20 * 10);

    final double pBlockTransition = not(pCompletedBlock
        * pServicedSomePartOfBlock);

    final SensorModelResult result = new SensorModelResult("pBlockTransition",
        pBlockTransition);
    result.addResult("pCompletedBlock", pCompletedBlock);
    result.addResult("pServicedSomePartOfBlock", pServicedSomePartOfBlock);

    return result;

    /**
     * We allow you to switch blocks
     */
    /*
     * return implies(pCompletedBlock * pServicedSomePartOfBlock,
     * not(pBlockTransitionHappenedWithNoLayover));
     */
  }
}
