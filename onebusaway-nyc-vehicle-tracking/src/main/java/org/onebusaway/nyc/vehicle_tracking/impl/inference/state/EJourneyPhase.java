package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import java.util.EnumSet;

public enum EJourneyPhase {

  /**
   * We're at a transit base
   */
  AT_BASE,

  /**
   * Non-revenue travel to the start of a block or trip
   */
  DEADHEAD_BEFORE,

  /**
   * A pause before a block starts
   */
  LAYOVER_BEFORE,

  /**
   * The vehicle is actively serving a block
   */
  IN_PROGRESS,

  /**
   * Non-revenue travel between trip segments of a block
   */
  DEADHEAD_DURING,

  /**
   * A pause before a block starts or between trip segments
   */
  LAYOVER_DURING,

  /**
   * The vehicle is off-route while actively serving a block
   */
  OFF_ROUTE,

  /**
   * Non-revenue from the end of a block back to the transit base
   */
  DEADHEAD_AFTER,

  /**
   * The vehicle is doing something unexpected
   */
  UNKNOWN;

  private static EnumSet<EJourneyPhase> _activeBeforeBlock = EnumSet.of(
      EJourneyPhase.AT_BASE, EJourneyPhase.DEADHEAD_BEFORE,
      EJourneyPhase.LAYOVER_BEFORE);

  private static EnumSet<EJourneyPhase> _activeDuringBlock = EnumSet.of(
      EJourneyPhase.IN_PROGRESS, EJourneyPhase.DEADHEAD_DURING,
      EJourneyPhase.LAYOVER_DURING, EJourneyPhase.OFF_ROUTE);

  public static boolean isActiveBeforeBlock(EJourneyPhase phase) {
    return _activeBeforeBlock.contains(phase);
  }

  public static boolean isActiveDuringBlock(EJourneyPhase phase) {
    return _activeDuringBlock.contains(phase);
  }
}
