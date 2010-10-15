package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import org.onebusaway.geospatial.model.CoordinatePoint;

public final class JourneyState {

  private final EJourneyPhase phase;

  private final BlockState blockState;

  private final Object data;

  private JourneyState(EJourneyPhase phase, BlockState blockState, Object data) {
    if (blockState == null)
      throw new IllegalArgumentException("blockState cannot be null");
    this.phase = phase;
    this.blockState = blockState;
    this.data = data;
  }

  public EJourneyPhase getPhase() {
    return phase;
  }

  public BlockState getBlockState() {
    return blockState;
  }

  @SuppressWarnings("unchecked")
  public <T> T getData() {
    return (T) data;
  }

  public static JourneyState atBase(BlockState blockState) {
    return new JourneyState(EJourneyPhase.AT_BASE, blockState, null);
  }

  public static JourneyState deadheadBefore(BlockState blockState,
      CoordinatePoint journeyStart) {
    JourneyStartState jss = new JourneyStartState(journeyStart);
    return new JourneyState(EJourneyPhase.DEADHEAD_BEFORE, blockState, jss);
  }

  public static JourneyState inProgress(BlockState blockState) {
    return new JourneyState(EJourneyPhase.IN_PROGRESS, blockState, null);
  }

  public static JourneyState layover(BlockState blockState) {
    return new JourneyState(EJourneyPhase.LAYOVER, blockState, null);
  }

  public static JourneyState deadheadAfter(BlockState blockState) {
    return new JourneyState(EJourneyPhase.DEADHEAD_AFTER, blockState, null);
  }

  public static JourneyState unknown(BlockState blockState) {
    return new JourneyState(EJourneyPhase.UNKNOWN, blockState, null);
  }
}
