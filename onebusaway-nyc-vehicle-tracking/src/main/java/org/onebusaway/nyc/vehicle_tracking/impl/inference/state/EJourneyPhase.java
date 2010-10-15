package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

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
   * A pause before a block starts or between trip segments
   */
  LAYOVER,

  /**
   * The vehicle is actively serving a block
   */
  IN_PROGRESS,

  /**
   * Non-revenue from the end of a block back to the transit base
   */
  DEADHEAD_AFTER,

  /**
   * The vehicle is doing something unexpected
   */
  UNKNOWN
}
