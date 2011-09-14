package org.onebusaway.nyc.vehicle_tracking.services;

import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;

public interface RunService {
  String getInitialRunForTrip(AgencyAndId trip);

  String getReliefRunForTrip(AgencyAndId trip);

  int getReliefTimeForTrip(AgencyAndId trip);

  List<RunTripEntry> getRunTripEntriesForRun(String runId);

  RunTripEntry getRunTripEntryForRunAndTime(String agencyId, String runId,
      long time);

  List<RunTripEntry> getRunTripEntriesForTime(String agencyId, long time);

  RunTripEntry getPreviousEntry(RunTripEntry entry);

  RunTripEntry getNextEntry(RunTripEntry entry);

}
