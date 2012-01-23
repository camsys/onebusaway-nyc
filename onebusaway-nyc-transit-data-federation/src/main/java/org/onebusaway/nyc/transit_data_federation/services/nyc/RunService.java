package org.onebusaway.nyc.transit_data_federation.services.nyc;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import com.google.common.collect.TreeMultimap;

public interface RunService {
  String getInitialRunForTrip(AgencyAndId trip);

  String getReliefRunForTrip(AgencyAndId trip);

  int getReliefTimeForTrip(AgencyAndId trip);

  List<RunTripEntry> getRunTripEntriesForRun(String runId);

  RunTripEntry getActiveRunTripEntryForRunAndTime(AgencyAndId runAgencyAndId,
      long time);

  List<RunTripEntry> getActiveRunTripEntriesForAgencyAndTime(String agencyId,
      long time);

  RunTripEntry getPreviousEntry(RunTripEntry entry, long date);

  RunTripEntry getNextEntry(RunTripEntry entry, long date);

  RunTripEntry getActiveRunTripEntryForBlockInstance(
      BlockInstance blockInstance, int scheduleTime);

  ScheduledBlockLocation getSchedBlockLocForRunTripEntryAndTime(
      RunTripEntry runTrip, long timestamp);

  BlockInstance getBlockInstanceForRunTripEntry(RunTripEntry rte,
      Date serviceDate);

  RunTripEntry getRunTripEntryForTripAndTime(TripEntry trip, int scheduledTime);

  public boolean isValidRunNumber(String runNumber);

  public TreeMultimap<Integer, String> getBestRunIdsForFuzzyId(
      String runAgencyAndId) throws IllegalArgumentException;

  Collection<? extends AgencyAndId> getTripIdsForRunId(String runId);

}
