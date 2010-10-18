package org.onebusaway.nyc.vehicle_tracking.impl.inference.state;

import org.onebusaway.geospatial.model.CoordinatePoint;

public final class JourneyState {

  private final EJourneyPhase phase;

  private final Object data;

  private JourneyState(EJourneyPhase phase, Object data) {
    this.phase = phase;
    this.data = data;
  }

  public EJourneyPhase getPhase() {
    return phase;
  }

  @SuppressWarnings("unchecked")
  public <T> T getData() {
    return (T) data;
  }

  public static JourneyState atBase() {
    return new JourneyState(EJourneyPhase.AT_BASE, null);
  }

  public static JourneyState deadheadBefore(CoordinatePoint journeyStart) {
    JourneyStartState jss = new JourneyStartState(journeyStart);
    return new JourneyState(EJourneyPhase.DEADHEAD_BEFORE, jss);
  }

  public static JourneyState layoverBefore() {
    return new JourneyState(EJourneyPhase.LAYOVER_BEFORE, null);
  }

  public static JourneyState inProgress() {
    return new JourneyState(EJourneyPhase.IN_PROGRESS, null);
  }

  public static JourneyState layoverDuring() {
    return new JourneyState(EJourneyPhase.LAYOVER_DURING, null);
  }

  public static JourneyState deadheadDuring(CoordinatePoint journeyStart) {
    JourneyStartState jss = new JourneyStartState(journeyStart);
    return new JourneyState(EJourneyPhase.DEADHEAD_DURING, jss);
  }

  public static JourneyState offRoute() {
    return new JourneyState(EJourneyPhase.OFF_ROUTE, null);
  }

  public static JourneyState deadheadAfter() {
    return new JourneyState(EJourneyPhase.DEADHEAD_AFTER, null);
  }

  public static JourneyState unknown() {
    return new JourneyState(EJourneyPhase.UNKNOWN, null);
  }
}
