package org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.ServiceCode;

public class StifTrip implements Comparable<StifTrip>, Serializable {
  private static final long serialVersionUID = 1L;
  
  public String nextRun;
  public String runId;
  public String reliefRunId;

  public String blockId;

  public ServiceCode serviceCode;
  public String firstStop;
  public String lastStop;

  public int firstStopTime;
  public int lastStopTime;
  public int listedLastStopTime;
  public int listedFirstStopTime;

  public int recoveryTime;
  public StifTripType type;
  private ArrayList<Trip> gtfsTrips;
  public boolean firstTripInSequence;
  public boolean lastTripInSequence;
  private String dsc;
  public String signCodeRoute;

  public File path;
  public int lineNumber;
  public TripIdentifier id;
  public String depot;
  public String nextTripOperatorDepot;
  public String agencyId;
  public char busType;

  public String direction;


  public StifTrip(String runId, String reliefRunId, String nextRun,
      StifTripType type, String dsc, char busType, String direction) {
    this.runId = runId;
    this.reliefRunId = reliefRunId;
    this.nextRun = nextRun;
    this.type = type;
    this.dsc = dsc;
    this.busType = busType;
    this.direction = direction;
  }

  // this is just for creating bogus objects for searching
  public StifTrip(int firstStopTime) {
    this.firstStopTime = firstStopTime;
  }

  @Override
  public int compareTo(StifTrip o) {
    // prioritize pullout trips before revenue or deadhead trips
    if (o.firstStopTime == firstStopTime 
        && type == StifTripType.PULLOUT
        && ( o.type == StifTripType.DEADHEAD 
          || o.type == StifTripType.REVENUE 
            )){
      return -1;
    } else if (o.firstStopTime == firstStopTime
        && o.type == StifTripType.PULLOUT
        && ( type == StifTripType.DEADHEAD
          || type == StifTripType.REVENUE) 
        ){
      return 1;
    }
    
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
    return "Trip on run (" + runId + ") at " + firstStopTime + ", dsc '" + dsc + "', direction " + direction ;
  }

  public String getRunIdWithDepot() {
    if (runId != null && runId.contains("-") && runId.startsWith("MISC")) {
      String[] runParts = runId.split("-");
      return runParts[0] + "-" + depot + "-" + runParts[1];
    }
    return runId + "-" + depot;
  }

  // This considers next OperatorDepot, falls back on depot of pullout
  public String getNextRunIdWithDepot() {
    if (nextRun != null && nextRun.contains("-") && nextRun.startsWith("MISC")) {
      String[] runParts = nextRun.split("-");
      return runParts[0] + "-" + nextTripOperatorDepot + "-" + runParts[1];
    }
    return nextRun + "-" + nextTripOperatorDepot;
  }

  String getDirection() {
	return direction;
  }
  
}
