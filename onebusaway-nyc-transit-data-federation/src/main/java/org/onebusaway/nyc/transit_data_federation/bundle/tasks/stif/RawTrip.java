package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.ServiceCode;

public class RawTrip implements Comparable<RawTrip> {

  public String nextRun;
  public String runId;
  public String reliefRunId;

  public ServiceCode serviceCode;
  public String firstStop;
  public String lastStop;

  public int firstStopTime;
  public int lastStopTime;
  public int recoveryTime;
  public StifTripType type;
  private ArrayList<Trip> gtfsTrips;
  public boolean firstTripInSequence;
  public boolean lastTripInSequence;
  private String dsc;
  public String signCodeRoute;

  public RawTrip(String runId, String reliefRunId, String nextRun,
      StifTripType type, String dsc) {
    this.runId = runId;
    this.reliefRunId = reliefRunId;
    this.nextRun = nextRun;
    this.type = type;
    this.dsc = dsc;
  }

  // this is just for creating bogus objects for searching
  public RawTrip(int firstStopTime) {
    this.firstStopTime = firstStopTime;
  }

  @Override
  public int compareTo(RawTrip o) {
    return firstStopTime - o.firstStopTime;
  }

  public void addGtfsTrip(Trip trip) {
    if (gtfsTrips == null) {
      gtfsTrips = new ArrayList<Trip>();
    }
    gtfsTrips.add(trip);
  }

  public List<Trip> getGtfsTrips() {
    if (gtfsTrips == null) {
      return Collections.emptyList();
    }
    return gtfsTrips;
  }

  public String getDsc() {
    return dsc;
  }

  public String getSignCodeRoute() {
    return signCodeRoute;
  }

  public String toString() {
    return "Trip on run (" + runId + ") at " + firstStopTime + " with dsc '" + dsc + "'";
  }
}
