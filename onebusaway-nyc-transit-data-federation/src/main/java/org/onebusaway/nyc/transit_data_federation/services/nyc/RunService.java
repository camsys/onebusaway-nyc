package org.onebusaway.nyc.transit_data_federation.services.nyc;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;

public interface RunService {
  String getInitialRunForTrip(AgencyAndId trip);

  String getReliefRunForTrip(AgencyAndId trip);

  int getReliefTimeForTrip(AgencyAndId trip);

  List<RunTripEntry> getRunTripEntriesForRun(String runId);

  RunTripEntry getRunTripEntryForRunAndTime(AgencyAndId runAgencyAndId,
      long time);

  List<RunTripEntry> getRunTripEntriesForTime(String agencyId, long time);

  RunTripEntry getPreviousEntry(RunTripEntry entry, long date);

  RunTripEntry getNextEntry(RunTripEntry entry, long date);

  RunTripEntry getRunTripEntryForBlockInstance(BlockInstance blockInstance, int scheduleTime);

  ScheduledBlockLocation getSchedBlockLocForRunTripEntryAndTime(
      RunTripEntry runTrip, long timestamp);

  BlockInstance getBlockInstanceForRunTripEntry(RunTripEntry rte,
      Date serviceDate);

  /**
   * Get a set of Levenshtein ranked RunTripEntry objects for a runId
   * and depotCode of uncertain quality.
   * FIXME not entirely "fuzzy" yet (due to strict runNumber matching)
   * <ul>
   * <li>Check for an exact match, if found then return just the corresponding runTripEntry</li>
   * <li>Return Levenshtein ranked RunTripEntry's for matching runNumber</li>
   * </ul>
   * @param runAgencyAndId
   * @param depotCode
   * @param time
   * @return
   */
  TreeMap<Integer, List<RunTripEntry>> getRunTripEntriesForFuzzyIdAndTime(AgencyAndId runAgencyAndId,
      List<String> depotCodes, long time);

}
