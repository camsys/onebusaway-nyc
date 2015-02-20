package org.onebusaway.nyc.transit_data_federation.services.nyc;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.nyc.transit_data_federation.bundle.tasks.stif.model.RunTripEntry;
import org.onebusaway.transit_data_federation.services.blocks.BlockInstance;
import org.onebusaway.transit_data_federation.services.blocks.ScheduledBlockLocation;
import org.onebusaway.transit_data_federation.services.transit_graph.TripEntry;

import com.google.common.collect.TreeMultimap;

/**
 * Maps of runs to trips (individual and groups of) and vice-versa.
 * @author jmaki
 *
 */
public interface RunService {

  /**
   * Simulator
   */
  public String getInitialRunForTrip(AgencyAndId trip);

  public String getReliefRunForTrip(AgencyAndId trip);

  public int getReliefTimeForTrip(AgencyAndId trip);
  
  public List<RunTripEntry> getActiveRunTripEntriesForAgencyAndTime(String agencyId,
		  long time);

  public Collection<RunTripEntry> getRunTripEntriesForRun(String runId);
  
  public Collection<RunTripEntry> getRunTripEntriesForBlock(String blockId);

  public RunTripEntry getActiveRunTripEntryForBlockInstance(
		  BlockInstance blockInstance, int scheduleTime);

  public ScheduledBlockLocation getSchedBlockLocForRunTripEntryAndTime(
		  RunTripEntry runTrip, long timestamp);

  public RunTripEntry getRunTripEntryForTripAndTime(TripEntry trip, int scheduledTime);

  /**
   * Inference engine
   */
  public TreeMultimap<Integer, String> getBestRunIdsForFuzzyId(
      String runAgencyAndId) throws IllegalArgumentException;

  public Collection<? extends AgencyAndId> getTripIdsForRunId(String runId);
  
  public Collection<? extends AgencyAndId> getTripIdsForRunId(String runId, String agencyId);

  public boolean isValidRunId(String runId);

  public boolean isValidBlockId(String blockId);

  public Set<AgencyAndId> getRoutesForRunId(String opAssignedRunId);
  
  public Set<AgencyAndId> getRoutesForRunId(String opAssignedRunId, String AgencyId);

  public Set<String> getRunIdsForTrip(TripEntry trip);

  /** 
   * Unit Testing
   */
  public RunTripEntry getPreviousEntry(RunTripEntry entry, long date);

  public RunTripEntry getNextEntry(RunTripEntry entry, long date);

}
