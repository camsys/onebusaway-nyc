package org.onebusaway.nyc.transit_data_federation.services.nyc;

import java.util.Date;
import java.util.List;

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

  RunTripEntry getPreviousEntry(RunTripEntry entry);

  RunTripEntry getNextEntry(RunTripEntry entry);

  RunTripEntry getRunTripEntryForBlockInstance(BlockInstance blockInstance, int scheduleTime);

  ScheduledBlockLocation getSchedBlockLocForRunTripEntryAndTime(
      RunTripEntry runTrip, long timestamp);

  BlockInstance getBlockInstanceForRunTripEntry(RunTripEntry rte,
      Date serviceDate);

}
