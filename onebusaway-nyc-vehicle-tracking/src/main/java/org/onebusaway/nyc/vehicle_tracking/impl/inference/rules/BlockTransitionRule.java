package org.onebusaway.nyc.vehicle_tracking.impl.inference.rules;

import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.implies;
import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.not;
import static org.onebusaway.nyc.vehicle_tracking.impl.inference.rules.Logic.p;

import java.util.List;

import org.apache.commons.lang.ObjectUtils;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.JourneyPhaseSummaryLibrary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.JourneyPhaseSummary;
import org.onebusaway.nyc.vehicle_tracking.impl.inference.state.VehicleState;
import org.onebusaway.nyc.vehicle_tracking.impl.particlefilter.DeviationModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
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
@Component
public class BlockTransitionRule implements SensorModelRule {

  private DeviationModel _blockCompletedRatio = new DeviationModel(0.03);

  /**
   * If a vehicle has serviced only a short period of time on a particular block
   * (~ less than 15 minutes), we consider it unserviced.
   */
  private DeviationModel _blockUnserviced = new DeviationModel(15 * 60);

  private JourneyPhaseSummaryLibrary _journeyPhaseSummaryLibrary;

  @Autowired
  public void setJourneyPhaseSummaryLibrary(
      JourneyPhaseSummaryLibrary journeyPhaseSummaryLibrary) {
    _journeyPhaseSummaryLibrary = journeyPhaseSummaryLibrary;
  }

  @Override
  public double likelihood(SensorModelSupportLibrary library, Context context) {

    VehicleState state = context.getState();
    List<JourneyPhaseSummary> summaries = state.getJourneySummaries();
    JourneyPhaseSummary currentBlock = _journeyPhaseSummaryLibrary.getCurrentBlock(summaries);

    /**
     * We first need a current block
     */
    if (currentBlock == null)
      return 1.0;

    /**
     * If we've been serving a sufficient amount of the current block, we can
     * assume it survived the block transition process before and would survive
     * again (performance op so we don't have to compute everything else each
     * iteration)
     */
    int currentBlockDuration = (int) ((currentBlock.getTimeTo() - currentBlock.getTimeFrom()) / 1000);
    if (currentBlockDuration > 5 * 60)
      return 1.0;

    /**
     * We next need a previous block
     */
    JourneyPhaseSummary previousBlock = _journeyPhaseSummaryLibrary.getPreviousBlock(
        summaries, currentBlock);

    if (previousBlock == null)
      return 1.0;

    /**
     * We only care if the two blocks are different
     */
    if (ObjectUtils.equals(currentBlock.getBlockInstance(),
        previousBlock.getBlockInstance()))
      return 1.0;

    /**
     * Did we complete the previous block?
     */
    double pCompletedBlock = _blockCompletedRatio.probability(1.0 - previousBlock.getBlockCompletionRatioTo());

    /**
     * Did we service a sufficient amount of the previous block for it to count
     */
    int previousBlockDuration = (int) ((previousBlock.getTimeTo() - previousBlock.getTimeFrom()) / 1000);
    double pServicedSomePartOfBlock = not(_blockUnserviced.probability(previousBlockDuration));

    /**
     * If the transition between a completed block to the next block happens
     * immediately with no layover, we should be concerned.
     * 
     * TODO: Detect an acutal layover
     */
    int blockTransitionDuration = (int) ((currentBlock.getTimeFrom() - previousBlock.getTimeTo()) / 1000);
    double pBlockTransitionHappenedWithNoLayover = p(blockTransitionDuration < 20 * 10);

    return not(pCompletedBlock * pServicedSomePartOfBlock);
    
    /**
     * We allow you to switch blocks
     */
    /*
    return implies(pCompletedBlock * pServicedSomePartOfBlock,
        not(pBlockTransitionHappenedWithNoLayover));
    */   
  }
}
