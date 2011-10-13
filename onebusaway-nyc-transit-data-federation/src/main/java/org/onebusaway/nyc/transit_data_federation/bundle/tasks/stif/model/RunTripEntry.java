package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model;

import java.util.List;

import org.onebusaway.transit_data_federation.services.transit_graph.StopTimeEntry;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

/**
 * This represents the part of a trip which is on a single run
 * 
 * @author novalis
 * 
 */
public class RunTripEntry implements Comparable<RunTripEntry> {
  private TripEntry entry;
  private String run;
  /**
   * For trips that switch runs mid way through, this is the time of the switch.
   * If this is -1, then this trip has no relief
   */
  private int reliefTime = -1;
  private ReliefState relief;

  public RunTripEntry(TripEntry entry, String run, int reliefTime,
      ReliefState relief) {
    this.entry = entry;
    this.run = run;
    this.reliefTime = reliefTime;
    this.relief = relief;
  }

  public int getStartTime() {
    if (getRelief() == ReliefState.AFTER_RELIEF) {
      return reliefTime;
    } else {
      // this is not really right, as a driver may get on a bus well
      // before the first stoptime (for instance if they are pulling out
      // from the depot).
      // but I don't think it will break anything.
      return entry.getStopTimes().get(0).getArrivalTime();
    }
  }

  public int getStopTime() {
	  
    // this run could end before the last stop
    int lastTime = reliefTime;

    if (lastTime < 0) {
      List<StopTimeEntry> stopTimes = entry.getStopTimes();
      StopTimeEntry lastStopTime = stopTimes.get(stopTimes.size() - 1);
      lastTime = lastStopTime.getDepartureTime();
    }
  
    return lastTime;
  }

  @Override
  public int compareTo(RunTripEntry other) {
    return getStartTime() - other.getStartTime();
  }

  public TripEntry getTripEntry() {
    return entry;
  }

  public String getRunId() {
    return run;
  }

  public ReliefState getRelief() {
    return relief;
  }

  public void setRelief(ReliefState relief) {
    this.relief = relief;
  }

  public String toString() {
    return "RunTripEntry(" + entry + "," + run + ")";
  }
}
